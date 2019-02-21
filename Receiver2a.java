/* Forename Surname MatriculationNumber */
/* Boyan Yotov s1509922*/

import javax.xml.crypto.Data;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.net.*;
import java.io.*;

public class Receiver2a extends Thread {
    private static final int DATA_SIZE = 1024;
    private static final int HEADER_SIZE = 5;
    private static final int MAX_SEQ_NUM = 65535;

    /* connection vars */
    private DatagramSocket socketIn;
    private DatagramSocket socketOut;
    private InetAddress address;
    private int port;

    /* File vars */
    private FileOutputStream fout = null;

    /* Data vars */
    private byte[] packetInData = new byte[DATA_SIZE + HEADER_SIZE];
    private byte[] dataToWrite;
    private DatagramPacket packetOut;
    private DatagramPacket packetIn;
    private int nextSeqNum;
    private int seqNum;
    private byte eofFlag;

    // Main functionality methods
    // ------------------------------------------------------------------
    private void setup(String[] args) {
        /* args: <RemoteHost> <Port> <Filename> */

        // get localhost IP
        // ------------------------------------------------------------------
        try {
            address = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            System.out.println("UNKNOWN HOST EXCEPTION");
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
        packetIn = new DatagramPacket(packetInData, packetInData.length);
        //System.out.println("Server Running");
        while (((int) eofFlag) == 0) {

            receivePacket();
            extractData();
            //System.out.println("Received packet: " + seqNum);
            if (seqNum == nextSeqNum) {
                //System.out.println("Writing and sending ACK");
                writeData();
                packetOut = createACKPacket();
                sendPacket(packetOut);
                nextSeqNum = (nextSeqNum + 1) % MAX_SEQ_NUM;
            } else {
                //System.out.println("Sending old ACK packet");
                sendPacket(packetOut);
            }
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
        Receiver2a receiver = new Receiver2a();
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
        seqNum = byteArrayToInt(Arrays.copyOfRange(packetInData, 2, 4));
        eofFlag = packetInData[4];
        dataToWrite = new byte[packetIn.getLength() - HEADER_SIZE];
        dataToWrite = Arrays.copyOfRange(packetInData, HEADER_SIZE, packetIn.getLength());
        //System.out.println("Receiving packet size == " + dataToWrite.length);
    }

    private void writeData() {
        try {
            fout.write(dataToWrite);
        } catch (IOException e) {
            System.out.println("ERROR IN Writing to file");
        }
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
        //System.out.println("Sending ACK packet size == " + packetOut.getLength());
    }

    private void sendPacket(DatagramPacket packet) {
        // System.out.println("Sending packet: " + seqNum);
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
