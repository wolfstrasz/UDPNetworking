/* Forename Surname MatriculationNumber */
/* Boyan Yotov s1509922*/
import javax.xml.crypto.Data;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.net.*;
import java.io.*;
import java.util.*;

public class Receiver2b extends Thread {
    private static final int DATA_SIZE = 1024;
    private static final int HEADER_SIZE = 5;
    private int MAX_SEQ_NUM = 65535;

    /* connection vars */
    private DatagramSocket socketIn;
    private DatagramSocket socketOut;
    private InetAddress address;
    private int port;

    /* File vars */
    private FileOutputStream fout = null;

    /* Data vars */
    private Map<Integer, byte[]> bufferedData = new HashMap<Integer,byte[]>();
    private DatagramPacket packetOut;
    private DatagramPacket packetIn;
    private byte[] packetData = new byte[DATA_SIZE + HEADER_SIZE];
    private byte[] dataToWrite;
    private byte eofFlag;
    private int windowSize;
    private int baseNum;
    private int seqNum;
    private boolean gotLastPacket = false;

    // Main functionality methods
    // ------------------------------------------------------------------
    private void setup(String[] args) {
        /* args: <RemoteHost> <Port> <Filename> <WindowSize>*/

        // get localhost IP
        // ------------------------------------------------------------------
        try {
            address = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            System.out.println("UNKNOWN HOST EXCEPTION");
            System.exit(0);
        }

        // parse window size
        // ------------------------------------------------------------------
        try  {
            windowSize = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.out.println("Window size is not an Integer");
            System.exit(0);
        }

        // parse port number
        // ------------------------------------------------------------------
        try {
            this.port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Port is not an Integer");
            System.exit(0);
        }

        // initialise sockets components
        // ------------------------------------------------------------------
        try {
            socketIn = new DatagramSocket(port);
        } catch (SocketException e) {
            System.out.println("SOCKET EXCEPTION");
            System.exit(0);
        }

        try {
            socketOut = new DatagramSocket();
        } catch (SocketException e) {
            System.out.println("SOCKET EXCEPTION");
            System.exit(0);
        }

        // initialise file write
        // ------------------------------------------------------------------
        try {
            fout = new FileOutputStream(args[1], false);
        } catch (FileNotFoundException e) {
        }

    }

    public void run() {
        seqNum = 1;
        baseNum = 1;
        MAX_SEQ_NUM = windowSize * 2;
        packetIn = new DatagramPacket(packetData, packetData.length);

        //System.out.println("Running with MSN: " + MAX_SEQ_NUM);
        while (!(gotLastPacket && bufferedData.keySet().size() == 0)) {
//            System.out.println("Waiting to receivePacket... ");
            receivePacket();
            extractData();
//            System.out.println("Basenum: " + baseNum);
//            System.out.println("Received packet: " + seqNum + " : " + eofFlag);
            if (seqNum == baseNum){
                writeData();
                //createACKPacket();
                //sendPacket();
                baseNum = (baseNum + 1 ) % MAX_SEQ_NUM; // => !isInWindow(seqNum)
                deliverBuffered();
            }

            if (isInWindow(seqNum)) {
                // add to buffer
//                System.out.println("Buffering packet: " + seqNum + " (basenum = " + baseNum + " )");
                bufferedData.put(seqNum, dataToWrite);
                packetOut = createACKPacket();
//                System.out.println("Sending ack packet: " + seqNum);
                sendPacket(packetOut);
            } else {
                packetOut = createACKPacket();
                sendPacket(packetOut);
//                System.out.println("Writing data and sending ack packet: " + seqNum);
            }

//            System.out.println("DATA TO WRITE: " + bufferedData.keySet().size());
//            for (Integer i: bufferedData.keySet()){
//                System.out.print(":" + i);
//            }
//            System.out.println("");
//            System.out.println("gotLastPacket: " + gotLastPacket + " && " + "bufferedData.size: " + bufferedData.keySet().size());
            if (((int) eofFlag & 0xFF) != 0) gotLastPacket =  true;
        }

    }

    private void close() {
        try {
            fout.close();
        } catch (IOException e) {
            System.out.println("ERROR: CANNOT CLOSE FILE");
        }
        socketIn.close();
        socketOut.close();
    }

    public static void main(String[] args) {
        Receiver2b receiver = new Receiver2b();
        receiver.setup(args);
        receiver.run();
        receiver.close();
    }

    // packet receiving methods
    // ------------------------------------------------------------------
    private void receivePacket() {
        try {
            socketIn.receive(packetIn);
            // System.out.println("got packet");
        } catch (IOException e) {
            System.out.println("ERROR: CANNOT RECEIVE PACKET");
        }
    }

    private void extractData() {
        seqNum = byteArrayToInt(Arrays.copyOfRange(packetData, 2, 4));
        eofFlag = packetData[4];
        dataToWrite = new byte[packetIn.getLength() - HEADER_SIZE];
        dataToWrite = Arrays.copyOfRange(packetData, HEADER_SIZE, packetIn.getLength());
//        System.out.println("Receiving packet size == " + dataToWrite.length);
    }

    private void writeData() {
        try {
            fout.write(dataToWrite);
        } catch (IOException e) {
            System.out.println("ERROR IN Writing to file");
        }
    }

    private boolean isInWindow(int number){
        if (baseNum <= number)
            return (number - baseNum) < windowSize;
        else return (MAX_SEQ_NUM - baseNum + number) < windowSize;
    }

    private void deliverBuffered(){
//        System.out.println("Start to deliverBuffered");
        while (bufferedData.containsKey(baseNum)){
            System.out.println("Delivered: " + baseNum);
            dataToWrite = bufferedData.get(baseNum);
            writeData();
            bufferedData.remove(baseNum);

            baseNum = (baseNum + 1) % MAX_SEQ_NUM;
        }
//        System.out.println("Finished deliverBuffered");
    }

    // packet sending methods
    // ------------------------------------------------------------------
    private DatagramPacket createACKPacket() {
        ByteBuffer bb = ByteBuffer.allocate(HEADER_SIZE - 1);
        bb.put((byte) 0); /// Offset
        bb.put((byte) 0); /// Octet
        bb.put(intToByteArray(seqNum)); /// Sequence Number
        byte[] combined = bb.array();

        // create packet
        return new DatagramPacket(combined, combined.length, address, port + 1);
//        System.out.println("Sending ACK packet size == " + packetOut.getLength());
    }

    private void sendPacket(DatagramPacket packet) {
//        System.out.println("Sending packet: " + seqNum);
        try {
            socketOut.send(packet);
        } catch (IOException e) {
            System.out.println("ERROR IN SOCKET SENDING");
        }
    }

    // UTILITIES
    // ------------------------------------------------------------------
    private static byte[] intToByteArray(int value) {
        return new byte[]{
                // (byte)(value >>> 24),
                // (byte)(value >>> 16),
                (byte) (value >>> 8), (byte) value};
    }

    private static int byteArrayToInt(byte[] bytes) {
        int value = 0;
        for (byte aByte : bytes) {
            value = value << 8;
            value = value | ((int) aByte & 0xFF);
        }
        return value;
    }

}
