/* Forename Surname MatriculationNumber */
/* Boyan Yotov s1509922*/

/* References:
    Started with templates of Server/Client "https://www.baeldung.com/udp-in-java"
    Also file read template "https://stackoverflow.com/questions/858980/file-to-byte-in-java"
    IntToByteArray : https://stackoverflow.com/questions/2183240/java-integer-to-byte-array
    https://stackoverflow.com/questions/5683486/how-to-combine-two-byte-arrays
    https://stackoverflow.com/questions/7619058/convert-a-byte-array-to-integer-in-java-and-vice-versa
    Everything else is my personal work.
*/
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;

public class Sender1a extends Thread {
    public static final int DATA_SIZE = 1024;
    public static final int HEADER_SIZE = 5;

    /* connection vars */
    private DatagramSocket socket;
    private InetAddress address;
    // private String host;
    private int port;

    /* data vars */
    private byte[] dataByte;
    int seqNum;
    byte eofFlag;
    byte num[];
    /* File vars */
    File file;
    FileInputStream fin = null;


    // UTILITIES

    public static final int byteArrayToInt(byte[] bytes) {
        int value = 0;
        value = (bytes[0] & 0xFF) << 8 | (bytes[1] & 0xFF);
        for (int i = 0; i < bytes.length; i++) {
            //value = value << 8;
            //value = value | ((int) bytes[i] & 0xFF);
        }
        return value;
    }

    private void setup(String[] args) {
        /* args: <RemoteHost> <Port> <Filename> */

        // Try to create file
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
        // host = args[0];
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

        while (true) {
            System.out.println("Creating packet: " + seqNum);
            // create packet
            DatagramPacket packet = createPacket();

            // send packet
            System.out.println("Sending packet: " + seqNum);

            try {
                socket.send(packet);
            } catch (IOException e) {
                System.out.println("ERROR IN SOCKET SENDING");
            }

            seqNum++;

            // Sleep
            System.out.println("Sleeping: ");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {

            }

            // Check for finish
            if (((int) eofFlag & 0xFF) != 0)
                break;
        }
    }

    public void analysis() {
    }

    public void close() {
        try {
            fin.close();
        } catch (IOException e) {
            System.out.println("ERROR IN FILE CLOSING");

        }
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

    public boolean extractDataChunk() {
        try {
            if (fin.read(dataByte) == -1) {
                // its last file.
                return false;
            }

        } catch (IOException e) {
            System.out.println("IOException at extractDataChunk()");
            System.exit(0);
            // return false;
        }

        return true;
    }

    public DatagramPacket createPacket() {
    //    System.out.println("SEQ       : " + seqNum);
        num = intToByteArray(seqNum);
    //    System.out.println("SEQ -> NUM: " + num);
    //    System.out.println("NUM -> SEQ: " + byteArrayToInt(num));

        if(extractDataChunk()) eofFlag = (byte) 0;
        else eofFlag = (byte )1;

        ByteBuffer bb = ByteBuffer.allocate(HEADER_SIZE + DATA_SIZE);
        bb.put((byte) 0); /// Offset
        bb.put((byte) 0); /// Octet
        bb.put(num); /// Sequence Number
        bb.put(eofFlag); /// EoF
        bb.put(dataByte); /// Data

        System.out.println((int)dataByte[0] + " " + (int)dataByte[1] + " ][ " + (int)dataByte[1022] + " " + (int)dataByte[1023]);

        byte[] combined = bb.array();
        //byte[] cc = {(byte)0,(byte)0};
    //    int NuMcomb = byteArrayToInt(Arrays.copyOfRange(combined, 2, 4));
        System.out.println("EOF:" + (int)combined[4]);
    //    System.out.println("First 2 Bytes / seqNum: " + (int)combined[0] + (int)combined[1] + NuMcomb);
        // create packet
        return new DatagramPacket(combined, combined.length, address, port);
    }
}
