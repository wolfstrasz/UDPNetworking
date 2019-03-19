/* Forename Surname MatriculationNumber */
/* Boyan Yotov s1509922*/

/* References:
    Started with templates of Server/Client "https://www.baeldung.com/udp-in-java"
    Also file read template "https://stackoverflow.com/questions/858980/file-to-byte-in-java"
    Everything else is my personal work.
*/
import java.net.DatagramSocket;
import java.util.Arrays;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.net.*;
import java.io.*;
import java.util.*;

public class Receiver1b extends Thread {
    public static final int DATA_SIZE = 1024;
    public static final int HEADER_SIZE = 5;

    /* connection vars */
    private DatagramSocket socketIn;
    private DatagramSocket socketOut;
    private int port;
    private InetAddress address;
    /* File vars */
    private FileOutputStream fout = null;

    /* Data vars */
    int seqNum;
    byte eofFlag;
    byte num[];
    private byte[] packetData = new byte[DATA_SIZE + HEADER_SIZE];
    private byte[] dataByte;
    DatagramPacket packetIn;
    DatagramPacket packetOut;

    // FSMw
    public enum State {
        WAIT_RECEIVE_0, WAIT_RECEIVE_1,
    };

    State state;

    private void setup(String[] args) {
        /* args: <RemoteHost> <Port> <Filename> */
        state = State.WAIT_RECEIVE_0;
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
        } catch( SocketException e) {
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
    }

    public void run() {

        while (true) {
            switch (state) {
            case WAIT_RECEIVE_0:
            //System.out.println("IN STATE: WAIT_RECEIVE_0");

                receivePacket();
                extractData();
                if (seqNum == 1) {
                    createACKPacket();
                    sendPacket();
                } else {
                    writeData();
                    createACKPacket();
                    sendPacket();
                    state = State.WAIT_RECEIVE_1;
                }
                break;
            case WAIT_RECEIVE_1:
            //System.out.println("IN STATE: WAIT_RECEIVE_1");

                receivePacket();
                extractData();
                if (seqNum == 0) {
                    createACKPacket();
                    sendPacket();
                } else {
                    writeData();
                    createACKPacket();
                    sendPacket();
                    state = State.WAIT_RECEIVE_0;
                }
                break;
            default:
                System.out.println("FSM reached illegal state.");
                System.exit(0);
                break;
            }
            // }
            // if EOF break;
            if (((int) eofFlag) == 1)
                break;
        }

    }

    public void close() {
        try {
            fout.close();
        } catch (IOException e) {
            System.out.println("ERROR: CANNOT CLOSE FILE");
        }
        // try {
            socketIn.close();
        // } catch (SocketException e) {
        //     System.out.println("ERROR: SOCKET CANNOT CLOSE");
        //     System.exit(0);
        // }
        // try {
            socketOut.close();
        // } catch (SocketException e) {
            // System.out.println("ERROR: SOCKET CANNOT CLOSE");
            // System.exit(0);
        // }
    }

    public static void main(String[] args) {
        Receiver1b receiver = new Receiver1b();
        receiver.setup(args);
        receiver.run();
        receiver.close();
        // System.out.println("Receiver1a Finished!");
    }

    // UTILITIES
    public static final byte[] intToByteArray(int value) {
        return new byte[] {
                // (byte)(value >>> 24),
                // (byte)(value >>> 16),
                (byte) (value >>> 8), (byte) value };
    }

    public static final int byteArrayToInt(byte[] bytes) {
        int value = 0;
        for (int i = 0; i < bytes.length; i++) {
            value = value << 8;
            value = value | ((int) bytes[i] & 0xFF);
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
        packetOut = new DatagramPacket(combined, combined.length, address, port+1);
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

}
