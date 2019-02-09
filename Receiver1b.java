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

public class Receiver1b extends Thread {
    public static final int DATA_SIZE = 1024;
    public static final int HEADER_SIZE = 5;

    /* connection vars */
    private DatagramSocket socket;
    private int port;

    /* File vars */
    private FileOutputStream fout = null;

    /* Data vars */
    private byte[] dataByte;
    int seqNum;
    byte eofFlag;
    byte num[];
    private byte[] receivedData;

    // FSM
    public enum State {
        WAIT_RECEIVE_0, WAIT_RECEIVE_1,
    };

    State state;

    /*
     * public EchoServer() { socket = new DatagramSocket(4445); }
     */
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

        dataByte = new byte[DATA_SIZE];
        receivedData = new byte[HEADER_SIZE + DATA_SIZE];
        eofFlag = (byte) 0;

    }

    public void run() {

        while (true) {
            switch (state) {
            case WAIT_RECEIVE_0:
                receivePacket();
                if (isACK(1)) {
                    seqNum = 1;
                    createPacket();
                    sendPacket();
                } else {
                    extractData();
                    writeData();
                    seqNum = 0;
                    createPacket();
                    sendPacket();
                    state = State.WAIT_RECEIVE_1;
                }
                break;
            case WAIT_RECEIVE_1:
                receivePacket();
                if (isACK(0)) {
                    seqNum = 0;
                    createPacket();
                    sendPacket();
                } else {
                    extractData();
                    writeData();
                    seqNum = 1;
                    createPacket();
                    sendPacket();
                    state = State.WAIT_RECEIVE_1;
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
            System.out.println("ERROR IN Closing file");

        }
    }

    public static void main(String[] args) {
        Receiver1b receiver = new Receiver1b();
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
        eofFlag = receivedData[4];
        dataByte = Arrays.copyOfRange(receivedData, HEADER_SIZE, HEADER_SIZE + DATA_SIZE);
    }

    public void writeData() {
        try {
            fout.write(dataByte);
        } catch (IOException e) {
            System.out.println("ERROR IN Writing to file");

        }
    }
}
