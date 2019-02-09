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
import java.net.*;
import java.io.*;
import java.util.*;

public class Receiver1a extends Thread {
    public static final int DATA_SIZE = 1024;
    public static final int HEADER_SIZE = 5;

    /* connection vars */
    private DatagramSocket socket;
    private int port;

    /* File vars */
    private File file;
    private FileOutputStream fout = null;

    /* Data vars */
    private byte[] dataByte;
    int seqNum;
    byte eofFlag;
    byte num[];
    private byte[] receivedData;

    private boolean running;

    /*
     * public EchoServer() { socket = new DatagramSocket(4445); }
     */
    private void setup(String[] args) {
        /* args: <RemoteHost> <Port> <Filename> */

        // Try to parse Port number
        try {
            this.port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Port is not an Integer");
            System.exit(0);
        }

        // initialise other components
        try {
            this.socket = new DatagramSocket(port);
        } catch (SocketException e) {
            System.out.println("SOCKET EXCEPTION");
            System.exit(0);
        }

        // initfile
        try {
            fout = new FileOutputStream(args[1], true);
        } catch (FileNotFoundException e) {
            // throw e;
        } catch (IOException e) {
            // throw e;
        }

        this.dataByte = new byte[DATA_SIZE];
        receivedData = new byte[HEADER_SIZE + DATA_SIZE];
        this.eofFlag = 0;

    }

    public void run() {

        running = true;

        while (running) {

            DatagramPacket packet = new DatagramPacket(receivedData, receivedData.length);
            try {
                socket.receive(packet);
                System.out.println("got packet");
            } catch (IOException e) {
                System.out.println("ERROR IN SOCKET RECEIVING");

            }

            extractData();

            // write data
            try {
                fout.write(dataByte);
            } catch (IOException e) {
                System.out.println("ERROR IN Writing to file");

            }
            // if EOF break;
            if (((int) eofFlag) == 1) {
                running = false;
            }
        }

    }

    public void close() {
        try {
            fout.close();
        } catch (IOException e) {
            System.out.println("ERROR IN Closing file");

        }
    }

    public static void main(String[] args) {
        Receiver1a receiver = new Receiver1a();
        receiver.setup(args);
        receiver.run();
        receiver.close();
        System.out.println("Receiver1a Finished!");
    }

    // UTILITIES

    public static final int byteArrayToInt(byte[] bytes) {
        int value = 0;
        for (int i = 0; i < bytes.length; i++) {
            value = value << 8;
            value = value | ((int) bytes[i] & 0xFF);
        }
        return value;
    }

    public void extractData() {
        seqNum = byteArrayToInt(Arrays.copyOfRange(receivedData, 2, 4));
        System.out.println("Receive packet: " + seqNum);
        eofFlag = receivedData[4];
        System.out.println("eof : " + ((int) eofFlag));
        dataByte = Arrays.copyOfRange(receivedData, HEADER_SIZE, HEADER_SIZE + DATA_SIZE);
        System.out.println("dataByte size = " + dataByte.length);
        System.out.println((int)dataByte[0] + " " + (int)dataByte[1] + " ][ " + (int)dataByte[1022] + " " + (int)dataByte[1023]);

    }
}
