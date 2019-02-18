/* Forename Surname MatriculationNumber */
/* Boyan Yotov s1509922*/


import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;
//import java.util.concurrent.TimeUnit;

public class Sender2b extends Thread {
    private static final int DATA_SIZE = 1024;
    private static final int HEADER_SIZE = 5;

    /* connection vars */
    private DatagramSocket socketIn;
    private DatagramSocket socketOut;
    private InetAddress address;
    private int port;

    /* data vars */
    private byte[] dataOut;
    private byte[] ackData;
    private int seqNum;
    private int baseNum;
    private int maxSeqNum;
    private byte eofFlag;
    private Map<Integer, DatagramPacket> packetsOut;
    private DatagramPacket lastPacket;
    private DatagramPacket packetIn;
    private int windowSize;


    /* File vars */
    private File file;
    private int fileSize;
    private int fullReads;
    private int leftover;
    private FileInputStream fin = null;

    /* Analysis vars */
    private long transmissionStart;
    private int transmissionTimeout;

    private void setup(String[] args) {
        /* args: <RemoteHost> <Port> <Filename> <TransmissionTimeout> <WindowSize>*/
        // parse window size;
        try  {
            windowSize = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.out.println("Window size is not an Integer");
            System.exit(0);
        }


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
        fileSize = (int)file.length();
        fullReads = fileSize / DATA_SIZE;
        leftover = fileSize - (fullReads * DATA_SIZE);

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
            socketIn = new DatagramSocket(port+1);
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
        // set timeout
        try {
            socketIn.setSoTimeout(transmissionTimeout);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        // Init file
        try {
            fin = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            System.out.println("FILENOTFOUNDEXCEPTION");
            System.exit(0);
        }


        // Init other data
        seqNum = 0;
        baseNum = 0;
        maxSeqNum = windowSize*2;
        ackData = new byte[HEADER_SIZE-1];
        packetsOut = new HashMap<>();
        packetIn = new DatagramPacket(ackData, ackData.length);
    }

    public void run() {
        int num_resends = 0;
        for (; packetsOut.size()< windowSize; ){
            createPacket();
            seqNum ++;
        }
        transmissionStart = System.currentTimeMillis();
        sendAllPackets();
        while (true) {
            // Try to receive ack
            try {
                socketIn.receive(packetIn);
            } catch (SocketTimeoutException e) {
                num_resends ++;
                sendAllPackets();
                continue;
            } catch (IOException e) {
                System.out.println("FSAF");
                System.exit(0);
            }

            if (isACK(baseNum)){
                packetsOut.remove(baseNum);
                baseNum = (baseNum + 1) % maxSeqNum;

                if (((int) eofFlag & 0xFF) != 0) {
                    createPacket();
                    seqNum = (seqNum + 1) % maxSeqNum;
                    sendLastPacket();
                }
            } else {
                for (Integer i : packetsOut.keySet()){
                    if (isACK(i))
                        packetsOut.remove(i);
                }
            }

            // Check for finish
            if ((((int) eofFlag & 0xFF) != 0 && packetsOut.size() == 0) || num_resends > 10)
                break;
        }
    }

    private void analysis() {
        long transmissionEnd = System.currentTimeMillis();
        double time = (transmissionEnd - transmissionStart) * 0.001; // get seconds
        double dataSize = fileSize / (double)1024; // in KBs
        //System.out.println(retransmissions + " " + (int) (dataSize / time));
        System.out.println((int) (dataSize / time));
    }

    private void close() {
        try {
            fin.close();
        } catch (IOException e) {
            System.out.println("ERROR: FILE STREAM CANNOT CLOSE");
            System.exit(0);
        }
        socketIn.close();
        socketOut.close();
    }

    public static void main(String[] args) {
        Sender2b sender = new Sender2b();
        sender.setup(args);
        sender.run();
        sender.analysis();
        sender.close();
    }

    // UTILITIES:
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

    // PACKET SENDING
    private void extractDataChunk() {
        if (fullReads > 0) {
            dataOut = new byte[DATA_SIZE];
            try {
                //noinspection ResultOfMethodCallIgnored
                fin.read(dataOut);
                fullReads--;
                eofFlag = (byte) 0;
            } catch (IOException e) {
                System.out.println("ERROR: CANNOT EXTRACT FULL DATA CHUNK");
                System.exit(0);
                // return false;
            }
        } else {
            dataOut = new byte[leftover];
            try {
                //noinspection ResultOfMethodCallIgnored
                fin.read(dataOut);
                eofFlag = (byte) 1;
            } catch (IOException e) {
                System.out.println("ERROR: CANNOT EXTRACT DATA CHUNK");
                System.exit(0);
                // return false;
            }
        }
    }

    private void createPacket() {
        extractDataChunk();

        ByteBuffer bb = ByteBuffer.allocate(HEADER_SIZE + dataOut.length);
        bb.put((byte) 0); /// Offset
        bb.put((byte) 0); /// Octet
        bb.put(intToByteArray(seqNum)); /// Sequence Number
        bb.put(eofFlag); /// EoF
        bb.put(dataOut); /// Data

        byte[] combined = bb.array();

        // create packet
        lastPacket = new DatagramPacket(combined, combined.length, address, port);
        packetsOut.put(seqNum, lastPacket);
        //System.out.println("Sending packet size == " + packetOut.getLength());
    }

    private void sendLastPacket() {
        // System.out.println("Sending packet: " + seqNum);
        try {
            socketOut.send(lastPacket);
        } catch (IOException e) {
            System.out.println("ERROR IN SOCKET SENDING");
        }
    }

    private void sendAllPackets() {
        for (Integer i : packetsOut.keySet()){
            try {
                socketOut.send(packetsOut.get(i));
            } catch (IOException e) {
                System.out.println("ERROR IN SOCKET SENDING");
            }
        }
    }

    // PACKET RECEIVING
    private boolean isACK(int number) {
        int ackNumber = byteArrayToInt(Arrays.copyOfRange(ackData, 2, 4));
        //System.out.println("Receiving packet size == " + ackData.length);
        return number == ackNumber;
    }


}
