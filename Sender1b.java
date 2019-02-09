/* Forename Surname MatriculationNumber */
/* Boyan Yotov s1509922*/

/* References:
    Started with templates of Server/Client "https://www.baeldung.com/udp-in-java"
    Also file read template "https://stackoverflow.com/questions/858980/file-to-byte-in-java"
    IntToByteArray : https://stackoverflow.com/questions/2183240/java-integer-to-byte-array
    https://stackoverflow.com/questions/5683486/how-to-combine-two-byte-arrays
    https://stackoverflow.com/questions/7619058/convert-a-byte-array-to-integer-in-java-and-vice-versa
    https://www.techiedelight.com/measure-elapsed-time-execution-time-java/
    Everything else is my personal work.
*/
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.util.concurent.TimeUnit;

public class Sender1b extends Thread {
    public static final int DATA_SIZE = 1024;
    public static final int HEADER_SIZE = 5;

    /* connection vars */
    private DatagramSocket socket;
    private InetAddress address;
    private int port;

    /* data vars */
    private byte[] dataByte;
    int seqNum;
    byte eofFlag;

    /* File vars */
    File file;
    FileInputStream fin = null;

    /* Analysis vars */
    Long retransmissions = 0;
    Long transmissionStart = 0;
    Long transmissionEnd = 0;
    Long packetsNumber = 0;
    Long transmissionTimeout = 0;

    // FSM
    public enum State {
        WAIT_CALL_0, WAIT_ACK_0, WAIT_CALL_1, WAIT_ACK_1,
    };

    State state;

    private void setup(String[] args) {
        /* args: <RemoteHost> <Port> <Filename> <TransmissionTimeout> */
        state = State.WAIT_CALL_0;

        // parse transmission timeout
        try {
            transmissionTimeout = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            System.out.println("Transmission timeout is not an Integer");
            System.exit(0);
        }

        // Try to init file
        file = new File(args[2]);
        if (!file.exists()) {
            System.out.println("File not found");
            System.exit(0);
        }

        // Try to parse Port number
        try {
            this.port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Port is not an Integer");
            System.exit(0);
        }
        // Try to get host
        try {
            address = InetAddress.getByName(args[0]);
        } catch (UnknownHostException e) {
            System.out.println("UNKNOWN HOST EXCEPTION");
            System.exit(0);
        }

        // initialise other components
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            System.out.println("SOCKET EXCEPTION");
            System.exit(0);

        }

        // Init file
        try {
            fin = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            System.out.println("FILENOTFOUNDEXCEPTION");
            System.exit(0);

            // throw e;
        } catch (IOException e) {
            System.out.println("IOEXCEPTION");
            System.exit(0);

            // throw e;
        }

        dataByte = new byte[DATA_SIZE];
        seqNum = 0;
    }

    public void run() {
        transmission_start_time = System.currentTimeMillis();
        while (true) {

            switch (state) {
            case WAIT_CALL_0:
                break;
            case WAIT_ACK_0:
                break;
            case WAIT_CALL_1:
                break;
            case WAIT_ACK_1:
                break;
            default:
                System.out.println("FSM reached illegal state.");
                System.exit(0);
                break;
            }
            // System.out.println("Creating packet: " + seqNum);
            // // create packet
            // DatagramPacket packet = createPacket();

            // // send packet
            // System.out.println("Sending packet: " + seqNum);
            // try {
            // socket.send(packet);
            // } catch (IOException e) {
            // System.out.println("ERROR IN SOCKET SENDING");
            // }

            // seqNum++;

            // // Sleep
            // System.out.println("Sleeping: ");
            // try {
            // Thread.sleep(100);
            // } catch (InterruptedException e) {

            // }

            // Check for finish
            if (((int) eofFlag & 0xFF) != 0)
                break;
        }
    }

    public void analysis() {
        transmissionEnd = System.currentTimeMillis();
        double time = (transmissionEnd - transmissionStart) * 0.001; // get seconds
        double dataSize = packetsNumber * (HEADER_SIZE + DATA_SIZE) / (double) 1024; // get transmitted data size in KBs
        System.out.println(retransmissions + " " + (int) (dataSize / time));
    }

    public void close() {
        try {
            fin.close();
        } catch (IOException e) {
            System.out.println("ERROR IN FILE CLOSING");
        }
    }

    public static void main(String[] args) {
        Sender1b sender = new Sender1b();
        sender.setup(args);
        sender.run();
        sender.analysis();
        sender.close();
    }

    // UTILITIES:
    public static final byte[] intToByteArray(int value) {
        return new byte[] {
                // (byte)(value >>> 24),
                // (byte)(value >>> 16),
                (byte) (value >>> 8), (byte) value };
    }

    public boolean extractDataChunk() {
        try {
            if (fin.read(dataByte) == -1) {
                // its last file.
                eofFlag = (byte) 1;
            }

        } catch (IOException e) {
            System.out.println("IOException at extractDataChunk()");
            System.exit(0);
            // return false;
        }

        eofFlag = (byte) 0;
    }

    public DatagramPacket createPacket() {

        extractDataChunk();

        ByteBuffer bb = ByteBuffer.allocate(HEADER_SIZE + DATA_SIZE);
        bb.put((byte) 0); /// Offset
        bb.put((byte) 0); /// Octet
        bb.put(intToByteArray(seqNum)); /// Sequence Number
        bb.put(eofFlag); /// EoF
        bb.put(dataByte); /// Data

        byte[] combined = bb.array();

        // create packet
        return new DatagramPacket(combined, combined.length, address, port);
    }
}
