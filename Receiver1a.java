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
    private FileOutputStream fout = null;

    /* Data vars */
    int seqNum;
    byte eofFlag;
    byte num[];
    private byte[] packetData = new byte[DATA_SIZE + HEADER_SIZE];
    private byte[] dataByte;
    DatagramPacket packet;

    private void setup(String[] args) {
        /* args: <RemoteHost> <Port> <Filename> */

        // Try to parse Port number
        try {
            this.port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("ERROR: PORT IS NOT AN INTEGER");
            System.exit(0);
        }

        // initialise other components
        try {
            this.socket = new DatagramSocket(port);
        } catch (SocketException e) {
            System.out.println("ERROR: SOCKET OPENING EXCEPTION");
            System.exit(0);
        }

        // initfile
        try {
            fout = new FileOutputStream(args[1], false);
        } catch (FileNotFoundException e) {
            System.out.println("ERROR: FILE FOR WRITE NOT FOUND");
            System.exit(0);
        } catch (IOException e) {
            System.out.println("ERROR: EXCEPTION IN FILE OPENING");
            System.exit(0);
        }
    }

    public void run() {
        while (true) {

            packet = new DatagramPacket(packetData, packetData.length);

            receiveData();
            extractData();
            writeData();
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
        try {
            socket.close();
        } catch (SocketException e) {
            System.out.println("ERROR: SOCKET CANNOT CLOSE");
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        Receiver1a receiver = new Receiver1a();
        receiver.setup(args);
        receiver.run();
        receiver.close();
        // System.out.println("Receiver1a Finished!");
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
        dataByte = new byte[packet.getLength() - HEADER_SIZE];
        dataByte = Arrays.copyOfRange(receivedData, HEADER_SIZE, HEADER_SIZE + packet.getLength());
    }

    public void writeData() {
        try {
            fout.write(dataByte);
        } catch (IOException e) {
            System.out.println("ERROR IN Writing to file");
        }
    }

    public void receiveData() {
        try {
            socket.receive(packet);
            // System.out.println("got packet");
        } catch (IOException e) {
            System.out.println("ERROR: CANNOT RECEIVE PACKET");
        }
    }
}
