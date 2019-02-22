/* Forename Surname MatriculationNumber */
/* Boyan Yotov s1509922*/

/* New references:
    Java Create Own Timer: https://stackoverflow.com/questions/10820033/make-a-simple-timer-in-java
 */

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;

public class Sender2a extends Thread {
    private static final int DATA_SIZE = 1024;
    private static final int HEADER_SIZE = 5;
    private static final int MAX_SEQ_NUM = 65535;

    /* connection vars */
    private DatagramSocket socketOut;
    private DatagramSocket socketIn;
    private InetAddress address;
    private int port;

    /* data vars */
    private Map<Integer, DatagramPacket> allPacketsOut = new HashMap<Integer, DatagramPacket>();
    private DatagramPacket packetOut;
    private DatagramPacket packetIn;
    private byte[] extractedData;
    private byte[] ackData = new byte[HEADER_SIZE-1];
    private int windowSize;
    private byte eofFlag;
    private int baseNum;
    private int seqNum;

    /* File vars */
    private FileInputStream fin = null;
    private File file;
    private int fullReads;
    private int leftover;

    /* Timing vars */
    private MyTimer timer;
    private int timerTimeout;
    private int num_resends = 0;

    /* Analysis vars */
    private long transmissionStart;
    private long transmissionEnd = 0;

    // Main functionality methods
    // ------------------------------------------------------------------
    private void setup(String[] args) {
        /* args: <RemoteHost> <Port> <Filename> <timerTimeout> <WindowSize>*/

        // parse window size
        // ------------------------------------------------------------------
        try  {
            windowSize = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.out.println("Window size is not an Integer");
            System.exit(0);
        }

        // parse transmission timeout
        // ------------------------------------------------------------------
        try {
            timerTimeout = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            System.out.println("Transmission timeout is not an Integer");
            System.exit(0);
        }

        // parse file name and open read stream
        // ------------------------------------------------------------------
        file = new File(args[2]);
        if (!file.exists()) {
            System.out.println("File not found");
            System.exit(0);
        }
        fullReads = (int)file.length() / DATA_SIZE;
        leftover = (int)file.length() - (fullReads * DATA_SIZE);

        try {
            fin = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            System.out.println("FILENOTFOUNDEXCEPTION");
            System.exit(0);
        }

        // parse server port number
        // ------------------------------------------------------------------

        try {
            this.port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Port is not an Integer");
            System.exit(0);
        }

        // parse host ip
        // ------------------------------------------------------------------

        try {
            address = InetAddress.getByName(args[0]);
        } catch (UnknownHostException e) {
            System.out.println("UNKNOWN HOST EXCEPTION");
            System.exit(0);
        }

        // initialise sockets components
        // ------------------------------------------------------------------
        try {
            socketIn = new DatagramSocket(port+1);
            // packet timeout to be calculated by private timer
            // otherwise it won't be able to receive or act upon data fast enough
            socketIn.setSoTimeout(1);
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
    }

    public void run() {
        seqNum = 1;
        baseNum = 1;
        packetIn = new DatagramPacket(ackData, ackData.length);
        // System.out.println("Client Running");
        transmissionStart = System.currentTimeMillis();
        timer = new MyTimer(timerTimeout);
        timer.start();

        // send first packet window
        for (; allPacketsOut.size()< windowSize; ){
            packetOut = createPacket();
            allPacketsOut.put(seqNum, packetOut);
            sendPacket(packetOut);
//            System.out.println("Sending packet: " + seqNum);

            seqNum = ( seqNum + 1 ) % MAX_SEQ_NUM;
        }

        while (! (((int) eofFlag & 0xFF) != 0 && allPacketsOut.size() == 0)) {
            // receive an ACK packet
            // ------------------------------------------------------------------
            try {
                socketIn.receive(packetIn);
            } catch (SocketTimeoutException e) {
                // check timer timeout for base packet
                if(timer.isTimeout()) {
                    resendPackets();
                    timer.start();
                //    num_resends++;
                //    if (num_resends > 10) transmissionEnd = System.currentTimeMillis();
                //    if (num_resends > 10 && allPacketsOut.size() <= windowSize - 1) break;
                }
            } catch (IOException e) {
                System.out.println("ERROR: IO Exception at SocketIn receive packet");
                System.exit(0);
            }

            // if we receive higher ACK number we can consider
            // all previous packets to be already ACKed
            // ------------------------------------------------------------------

            while (getACK() >= baseNum){
        //         System.out.println("Received ACK: " + getACK());
                if(allPacketsOut.containsKey(baseNum))      // removes duplicates
                    allPacketsOut.remove(baseNum);

                baseNum = (baseNum + 1) % MAX_SEQ_NUM;
                num_resends = 0;                            // reset when we send new packets
                if (((int) eofFlag & 0xFF) == 0) {
                    packetOut = createPacket();
                    allPacketsOut.put(seqNum, packetOut);

                    seqNum = (seqNum + 1) % MAX_SEQ_NUM;
                    sendPacket(packetOut);
                //     System.out.println("Sending packet: " + seqNum);
                }
            }
        }
    }

    private void analysis() {
        transmissionEnd = transmissionEnd == 0 ? System.currentTimeMillis() : transmissionEnd;
        double time = (transmissionEnd - transmissionStart) * 0.001; // get seconds
        double dataSize = ((int)file.length()) / (double)1024; // in KBs

        // print throughput
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
        Sender2a sender = new Sender2a();
        sender.setup(args);
        sender.run();
        sender.analysis();
        sender.close();
    }

    // Packet sending methods
    // ------------------------------------------------------------------
    private void extractDataChunk() {
        if (fullReads > 0) {
            extractedData = new byte[DATA_SIZE];
            try {
                //noinspection ResultOfMethodCallIgnored
                fin.read(extractedData);
                fullReads--;
                eofFlag = (byte) 0;
            } catch (IOException e) {
                System.out.println("ERROR: CANNOT EXTRACT FULL DATA CHUNK");
                System.exit(0);
                // return false;
            }
        } else {
            extractedData = new byte[leftover];
            try {
                //noinspection ResultOfMethodCallIgnored
                fin.read(extractedData);
                eofFlag = (byte) 1;
            } catch (IOException e) {
                System.out.println("ERROR: CANNOT EXTRACT DATA CHUNK");
                System.exit(0);
                // return false;
            }
        }
    }

    private DatagramPacket createPacket() {
        extractDataChunk();

        ByteBuffer bb = ByteBuffer.allocate(HEADER_SIZE + extractedData.length);
        bb.put((byte) 0); /// Offset
        bb.put((byte) 0); /// Octet
        bb.put(intToByteArray(seqNum)); /// Sequence Number
        bb.put(eofFlag); /// EoF
        bb.put(extractedData); /// Data

        byte[] combined = bb.array();

        // create packet
        return new DatagramPacket(combined, combined.length, address, port);
    }

    private void sendPacket(DatagramPacket packet) {
        try {
            socketOut.send(packet);
        } catch (IOException e) {
            System.out.println("ERROR IN SOCKET SENDING");
        }
    }

    private void resendPackets() {
    //    System.out.println("resendPackets:");
        for (int i = baseNum; i < baseNum + allPacketsOut.size() ; i++){
    //        System.out.print(i + ",");
            try {
                socketOut.send(allPacketsOut.get(i));
            } catch (IOException e) {
               System.out.println("ERROR IN SOCKET SENDING");
            }
        }
//        System.out.println("");
    }

    // UTILITIES
    // ------------------------------------------------------------------
    private int getACK() {
        //System.out.println("Receiving packet size == " + ackData.length);
        return byteArrayToInt(Arrays.copyOfRange(ackData, 2, 4));
    }

    private static byte[] intToByteArray(int value) {
        return new byte[] {
                // (byte)(value >>> 24),
                // (byte)(value >>> 16),
                (byte) (value >>> 8),
                (byte) value };
    }

    private static int byteArrayToInt(byte[] bytes) {
        int value = 0;
        for (byte aByte : bytes) {
            value = value << 8;
            value = value | ((int) aByte & 0xFF);
        }
        return value;
    }

    // own timer class
    // ------------------------------------------------------------------
    private class MyTimer {
        long startTime;
        long timeoutTime;
        public MyTimer (long timeoutTime){
            this.timeoutTime = timeoutTime;
        }
        public void start(){
            startTime = System.currentTimeMillis();
        }
        public boolean isTimeout(){
            long endTime = System.currentTimeMillis();
            return timeoutTime < (endTime - startTime);
        }
    }
}
