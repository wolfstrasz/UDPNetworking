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
import java.util.concurrent.TimeUnit;

public class Sender1a extends Thread {
    public static final int DATA_SIZE = 1024;
    public static final int HEADER_SIZE = 5;

    /* connection vars */
    private DatagramSocket socket;
    private DatagramPacket packet;
    private InetAddress address;
    private int port;

    /* data vars */
    private byte[] dataByte;
    int seqNum = 0;
    byte eofFlag = 0;

    /* File vars */
    FileInputStream fin = null;
    int fileSize;
    int fullReads;
    int leftover;
    File file;

    /* Analysis vars */
    Long retransmissions;
    Long transmissionStart;
    Long transmissionEnd;
    Long packetsNumber;

    private void setup(String[] args) {
        /* args: <RemoteHost> <Port> <Filename> */

        // Try to create file
        file = new File(args[2]);
        if (!file.exists()) {
            System.out.println("ERROR: FILE NOT FOUND");
            System.exit(0);
        }
        fileSize = (int) file.length();
      //  System.out.println("filesize" + fileSize);
        fullReads = fileSize / DATA_SIZE;
      //  System.out.println("filesize" + fullReads);
        leftover = fileSize - (fullReads * DATA_SIZE);
      //  System.out.println("filesize" + leftover);
        // Try to parse Port number
        try {
            this.port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("ERROR: PORT IS NOT AN INTEGER");
            System.exit(0);
        }
        // Try to get host
        try {
            address = InetAddress.getByName(args[0]);
        } catch (UnknownHostException e) {
            System.out.println("ERROR: UNKNOWN HOST");
            System.exit(0);
        }

        // initialise other components
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            System.out.println("ERROR: SOCKET OPENING EXCEPTION");
            System.exit(0);
        }

        // Init file
        try {
            fin = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            System.out.println("ERROR: FILE FOR READ NOT FOUND");
            System.exit(0);
        }

        eofFlag = (byte)0;
    }

    public void run() {
        transmissionStart = System.currentTimeMillis();
        while (true) {

        //    System.out.println("Here 1");
            packet = createPacket();
        //    System.out.println("Here 2");
            sendPacket();
        //    System.out.println("Here 3");
            seqNum++;

            // sleep
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.out.println("ERROR: THREAD SLEEP");
                System.exit(0);
            }
           // System.out.println("Here 4");
            // check for finish
            if (((int) eofFlag & 0xFF) != 0)
                break;
        }
    }

    public void analysis() {
        transmissionEnd = System.currentTimeMillis();
        double time = (transmissionEnd - transmissionStart) * 0.001; // get seconds
        double dataSize = seqNum * (HEADER_SIZE + DATA_SIZE) / (double) 1024; // get transmitted data size in KBs
        System.out.println((int) (dataSize / time));
    }

    public void close() {
        try {
            fin.close();
        } catch (IOException e) {
            System.out.println("ERROR: FILE STREAM CANNOT CLOSE");
            System.exit(0);
        }

        socket.close();
    }

    public static void main(String[] args) {
        Sender1a sender = new Sender1a();
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

    public void extractDataChunk() {
        if (fullReads > 0) {
            dataByte = new byte[DATA_SIZE];
            try {
                fin.read(dataByte);
                fullReads--;
                eofFlag = (byte) 0;
            } catch (IOException e) {
                System.out.println("ERROR: CANNOT EXTRACT FULL DATA CHUNK");
                System.exit(0);
                // return false;
            }
        } else {
            dataByte = new byte[leftover];
            try {
                fin.read(dataByte);
                eofFlag = (byte) 1;
            } catch (IOException e) {
                System.out.println("ERROR: CANNOT EXTRACT DATA CHUNK");
                System.exit(0);
                // return false;
            }
        }
    }

    public DatagramPacket createPacket() {
        extractDataChunk();

        ByteBuffer bb = ByteBuffer.allocate(HEADER_SIZE + dataByte.length);
        bb.put((byte) 0); /// Offset
        bb.put((byte) 0); /// Octet
        bb.put(intToByteArray(seqNum)); /// Sequence Number
        bb.put(eofFlag); /// EoF
        bb.put(dataByte); /// Data

        byte[] combined = bb.array();

        // create packet

        System.out.println("Packet #" + seqNum + " length: " + combined.length);
        return new DatagramPacket(combined, combined.length, address, port);
    }

    public void sendPacket() {
        // System.out.println("Sending packet: " + seqNum);
        try {
            socket.send(packet);
        } catch (IOException e) {
            System.out.println("ERROR IN SOCKET SENDING");
        }
    }
}
