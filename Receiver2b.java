/* Forename Surname MatriculationNumber */
/* Boyan Yotov s1509922*/
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
    private int port;
    private InetAddress address;
    /* File vars */
    private FileOutputStream fout = null;

    /* Data vars */
    private int seqNum;
   // private int nextSeqNum;
    private int baseNum;
    private int windowSize;
    private byte eofFlag;
    private byte num[];
    private byte[] packetData = new byte[DATA_SIZE + HEADER_SIZE];
    private byte[] dataByte;
    private DatagramPacket packetIn;
    private DatagramPacket packetOut;
    private Map<Integer, byte[]> dataToWrite;

    private void setup(String[] args) {
        /* args: <RemoteHost> <Port> <Filename> <WindowSize>*/
        // parse window size;
        try  {
            windowSize = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.out.println("Window size is not an Integer");
            System.exit(0);
        }

        // Try to parse Port number
        try {
            this.port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Port is not an Integer");
            System.exit(0);
        }

        // initialise other components
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

        // initfile
        try {
            fout = new FileOutputStream(args[1], false);
        } catch (FileNotFoundException e) {
            // throw e;
            // } catch (IOException e) {
            //     // throw e;
        }

        dataByte = new byte[DATA_SIZE];
        packetData = new byte[HEADER_SIZE + DATA_SIZE];
        eofFlag = (byte) 0;

        try {
            address = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            System.out.println("UNKNOWN HOST EXCEPTION");
            System.exit(0);
        }
        packetIn = new DatagramPacket(packetData, packetData.length);
        seqNum = 1;
        dataToWrite = new HashMap<Integer,byte[]>();
        //nextSeqNum = 0;
    }

    public void run() {
        baseNum = 1;
        boolean gotLast = false;
       // nextSeqNum = 0;
        MAX_SEQ_NUM = windowSize * 2;

        //System.out.println("Running with MSN: " + MAX_SEQ_NUM);
        while (true) {
            System.out.println("Waiting to receivePacket... ");
            receivePacket();
            extractData();
           System.out.println("Basenum: " + baseNum);
            System.out.println("Received packet: " + seqNum + " : " + eofFlag);
            if (seqNum == baseNum){
                writeData();
                //createACKPacket();
                //sendPacket();
                baseNum = (baseNum + 1 ) % MAX_SEQ_NUM;
                deliverBuffered();
            }

            if (isInWindow(seqNum)) {
                System.out.println("Buffering packet: " + seqNum + " (basenum = " + baseNum + " )");
                addToBuffer();
                createACKPacket();
                System.out.println("Sending ack packet: " + seqNum);
                sendPacket();
            }

            if (!isInWindow(seqNum)) {
                createACKPacket();
                sendPacket();
                System.out.println("Writing data and sending ack packet: " + seqNum);

            }

            System.out.println("DATA TO WRITE: " + dataToWrite.keySet().size());
            for (Integer i: dataToWrite.keySet()){
                System.out.print(":" + i);
            }
            System.out.println("");
            //System.out.println("GotLast: " + gotLast + " && " + "dataToWrite.size: " + dataToWrite.keySet().size());
            if (gotLast == false) gotLast = ((int) eofFlag & 0xFF) != 0;
            if (gotLast && dataToWrite.keySet().size() == 0)
                break;
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

    // UTILITIES
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

    // PACKET RECEIVING
    public void receivePacket() {
        try {
            socketIn.receive(packetIn);
            // System.out.println("got packet");
        } catch (IOException e) {
            System.out.println("ERROR: CANNOT RECEIVE PACKET");
        }
    }

    public void extractData() {
        seqNum = byteArrayToInt(Arrays.copyOfRange(packetData, 2, 4));
        eofFlag = packetData[4];
        dataByte = new byte[packetIn.getLength() - HEADER_SIZE];
        dataByte = Arrays.copyOfRange(packetData, HEADER_SIZE, packetIn.getLength());
        //System.out.println("Receiving packet size == " + dataByte.length);
    }

    public void writeData() {
        try {
            fout.write(dataByte);
        } catch (IOException e) {
            System.out.println("ERROR IN Writing to file");
        }
    }

    // PACKET SENDING
    public void createACKPacket() {
        ByteBuffer bb = ByteBuffer.allocate(HEADER_SIZE - 1);
        bb.put((byte) 0); /// Offset
        bb.put((byte) 0); /// Octet
        bb.put(intToByteArray(seqNum)); /// Sequence Number
        byte[] combined = bb.array();

        // create packet
        packetOut = new DatagramPacket(combined, combined.length, address, port + 1);
        //System.out.println("Sending ACK packet size == " + packetOut.getLength());
    }

    public void sendPacket() {
        // System.out.println("Sending packet: " + seqNum);
        try {
            socketOut.send(packetOut);
        } catch (IOException e) {
            System.out.println("ERROR IN SOCKET SENDING");
        }
    }

    public boolean isInWindow(int number){
        if (baseNum <= number)
            return (number - baseNum) < windowSize;
        else return (MAX_SEQ_NUM - baseNum + number) < windowSize;
    }
    public void deliverBuffered(){
        System.out.println("Start to deliverBuffered");
        while (dataToWrite.containsKey(baseNum)){
            System.out.println("Delivered: " + baseNum);
            dataByte = dataToWrite.get(baseNum);
            writeData();
            dataToWrite.remove(baseNum);
            
            baseNum = (baseNum + 1) % MAX_SEQ_NUM;
        }
        System.out.println("Finished deliverBuffered");

    }
    public void addToBuffer() {
        dataToWrite.put(seqNum, dataByte);
    }
}
