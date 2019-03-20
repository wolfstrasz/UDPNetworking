
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
    private InetAddress address;
    private int port;

    /* File vars */
    private FileOutputStream fout = null;

    /* Data vars */
    private Map<Integer, byte[]> bufferedData = new HashMap<Integer, byte[]>();
    private DatagramPacket packetOut;
    private DatagramPacket packetIn;
    private byte[] packetData = new byte[DATA_SIZE + HEADER_SIZE];
    private byte[] dataToWrite;
    private byte eofFlag;
    private int windowSize;
    private int baseNum;
    private int seqNum;
    private boolean gotLastPacket = false;

    // pre-run setup
    // ------------------------------------------------------------------
    private void setup(String[] args) {
        /* args: <RemoteHost> <Port> <Filename> <WindowSize> */

        // get localhost IP
        // ------------------------------------------------------------------
        try {
            address = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            System.out.println("ERROR: Unknown host at getting localhost ip");
            System.exit(0);
        }

        // parse window size
        // ------------------------------------------------------------------
        try {
            windowSize = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.out.println("ERROR: Window size is not an Integer");
            System.exit(0);
        }

        // parse port number
        // ------------------------------------------------------------------
        try {
            this.port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("ERROR: Parsed port is not an Integer");
            System.exit(0);
        }

        // initialise sockets components
        // ------------------------------------------------------------------
        try {
            socketIn = new DatagramSocket(port);
        } catch (SocketException e) {
            System.out.println("ERROR: SOCKET EXCEPTION at opening socketIn");
            System.exit(0);
        }

        try {
            socketOut = new DatagramSocket();
        } catch (SocketException e) {
            System.out.println("ERROR: SOCKET EXCEPTION at opening socketOut");
            System.exit(0);
        }

        // initialise file write
        // ------------------------------------------------------------------
        try {
            fout = new FileOutputStream(args[1], false);
        } catch (FileNotFoundException e) {
        }

    }

    // main server functionality
    // ------------------------------------------------------------------
    public void run() {
        seqNum = 1;
        baseNum = 1;
        MAX_SEQ_NUM = windowSize * 2;
        packetIn = new DatagramPacket(packetData, packetData.length);

        // work until we have received at some point the last packet and
        // all of our buffered data is writen in the file
        // ------------------------------------------------------------------
        while (!(gotLastPacket && bufferedData.keySet().size() == 0)) {
            receivePacket();
            extractData();

            // if we receive packet in order write it, send ACK
            // check if we can write any of the buffered data
            if (seqNum == baseNum) {
                // System.out.println("Writing data");
                writeData();
                createACKPacket();
                sendPacket(packetOut);
                baseNum = (baseNum + 1) % MAX_SEQ_NUM;
                deliverBuffered();

                // if it is inside the window but not in order
                // then buffer it and send ACK
            } else if (isInWindow(seqNum)) {
                // add to buffer
                bufferedData.put(seqNum, dataToWrite);
                packetOut = createACKPacket();
                // System.out.println("Sending ack packet: " + seqNum);
                sendPacket(packetOut);

                // otherwise send ACK
            } else {
                packetOut = createACKPacket();
                sendPacket(packetOut);
                // System.out.println("Sending ack packet: " + seqNum);
            }

            if (((int) eofFlag & 0xFF) != 0)
                gotLastPacket = true;
        }

        // after finish send stop signal 100 times
        packetOut = createSTOPPacket();
        for (int i = 0; i < 100; i++) {
            sendPacket(packetOut);
        }

    }

    // closes output stream and sockets
    // ------------------------------------------------------------------
    private void close() {
        try {
            fout.close();
        } catch (IOException e) {
            System.out.println("ERROR: CANNOT CLOSE FILE");
        }
        socketIn.close();
        socketOut.close();
    }

    // entry point
    // ------------------------------------------------------------------
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
        } catch (IOException e) {
            System.out.println("ERROR: CANNOT RECEIVE PACKET");
        }
    }

    private void extractData() {
        seqNum = byteArrayToInt(Arrays.copyOfRange(packetData, 2, 4));
        eofFlag = packetData[4];
        dataToWrite = new byte[packetIn.getLength() - HEADER_SIZE];
        dataToWrite = Arrays.copyOfRange(packetData, HEADER_SIZE, packetIn.getLength());
    }

    private void writeData() {
        try {
            fout.write(dataToWrite);
        } catch (IOException e) {
            System.out.println("ERROR IN Writing to file");
        }
    }

    // function to check whether a number is
    // inside the window
    // case 1 : [0, ..., basenum, ..., number, ..., window*2]
    // case 2 : [0, ..., number, ..., basenum, ..., window*2]
    // ------------------------------------------------------------------
    private boolean isInWindow(int number) {
        if (baseNum <= number)
            return (number - baseNum) < windowSize;
        else
            return (MAX_SEQ_NUM - baseNum + number) < windowSize;
    }

    // function that goes through all packets that are buffered
    // and are in order to be delivered
    // ------------------------------------------------------------------
    private void deliverBuffered() {
        // System.out.println("Start to deliverBuffered");
        while (bufferedData.containsKey(baseNum)) {
            // System.out.println("Delivered: " + baseNum);
            dataToWrite = bufferedData.get(baseNum);
            writeData();
            bufferedData.remove(baseNum);

            baseNum = (baseNum + 1) % MAX_SEQ_NUM;
        }
        // System.out.println("Finished deliverBuffered");
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
    }

    // new time of packet that will let the client know that the server
    // finished all its job and client must stop sending data
    // ------------------------------------------------------------------
    private DatagramPacket createSTOPPacket() {
        ByteBuffer bb = ByteBuffer.allocate(HEADER_SIZE - 1);
        bb.put((byte) 1); /// Offset
        bb.put((byte) 1); /// Octet
        bb.put(intToByteArray(seqNum)); /// Sequence Number
        byte[] combined = bb.array();

        // create packet
        return new DatagramPacket(combined, combined.length, address, port + 1);
    }

    // sends given packet
    // ------------------------------------------------------------------
    private void sendPacket(DatagramPacket packet) {
        try {
            socketOut.send(packet);
        } catch (IOException e) {
            System.out.println("ERROR IN SOCKET SENDING");
        }
    }

    // UTILITIES
    // ------------------------------------------------------------------
    private static byte[] intToByteArray(int value) {
        return new byte[] {
                // (byte)(value >>> 24),
                // (byte)(value >>> 16),
                (byte) (value >>> 8), (byte) value };
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
