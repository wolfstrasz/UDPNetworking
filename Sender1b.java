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

public class Sender1b extends Thread {
    public static final int DATA_SIZE = 1024;
    public static final int HEADER_SIZE = 5;

    /* connection vars */
    private DatagramSocket socketIn;
    private DatagramSocket socketOut;
    private InetAddress address;
    private int port;

    /* data vars */
    private byte[] dataByte;
    private byte[] ackData;
    int seqNum;
    byte eofFlag;
    DatagramPacket packetOut;
    DatagramPacket packetIn;

    /* File vars */
    File file;
    int fileSize;
    int fullReads;
    int leftover;
    FileInputStream fin = null;

    /* Analysis vars */
    int retransmissions;
    long transmissionStart;
    long transmissionEnd;
    long packetsNumber;
    int transmissionTimeout;
    //long transmittedDataSize;
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

            // throw e;
        // } catch (IOException e) {
        //     System.out.println("IOEXCEPTION");
        //     System.exit(0);

        //     // throw e;
        }
        ackData = new byte[HEADER_SIZE-1];
        packetIn = new DatagramPacket(ackData, ackData.length);
    }

    public void run() {
        transmissionStart = System.currentTimeMillis();
        boolean last_ack_received = false;
        int last_ack_tries = 0;
        while (true) {

            switch (state) {
            case WAIT_CALL_0:
            //System.out.println("IN STATE: WAIT_CALL_0");

                seqNum = 0;
                createPacket();
            //    transmittedDataSize +=  packetOut.getLength();
                sendPacket();
                // startTimer();
                state = State.WAIT_ACK_0;
                break;
            case WAIT_ACK_0:
                //System.out.println("IN STATE: WAIT_ACK_0");
                try {
                    receivePacket();
                } catch (SocketTimeoutException e) {
                    //    System.out.println("SOCKET TIMEOUT");
                    if (((int) eofFlag & 0xFF) != 0){
                        last_ack_tries ++;
                    } else retransmissions++;
                    //        System.out.println("MORE STUFF");
                //    transmittedDataSize +=  packetOut.getLength();
                    sendPacket();
                    break;
                } catch (IOException e){
                    //retransmissions++;
                //    retransmissions = retransmissions + 1;
                    //sendPacket();
                    break;
                }

                if (isACK(0)) {
                    state = State.WAIT_CALL_1;
                    if (((int) eofFlag & 0xFF) != 0)
                        last_ack_received = true;
                }

                break;

            case WAIT_CALL_1:
            //System.out.println("IN STATE: WAIT_CALL_1");

                seqNum = 1;
                createPacket();
                //transmittedDataSize +=  packetOut.getLength();
                sendPacket();
                // startTimer();
                state = State.WAIT_ACK_1;
                break;

            case WAIT_ACK_1:
            //System.out.println("IN STATE: WAIT_ACK_1");

                try {
                    receivePacket();
                } catch (SocketTimeoutException e) {
                //    System.out.println("SOCKET TIMEOUT");
                //    retransmissions = retransmissions + 1;
                    if (((int) eofFlag & 0xFF) != 0){
                        last_ack_tries ++;
                    } else retransmissions++;

                //    transmittedDataSize +=  packetOut.getLength();
                    sendPacket();
                    break;
                } catch (IOException e){
                    //retransmissions = retransmissions + 1;
                    //sendPacket();
                    break;
                }

                if (isACK(1)) {
                    state = State.WAIT_CALL_0;
                    if (((int) eofFlag & 0xFF) != 0)
                        last_ack_received = true;
                }
                break;
            default:
                System.out.println("FSM reached illegal state.");
                System.exit(0);
                break;
            }

            // Check for finish
            if ((((int) eofFlag & 0xFF) != 0 && last_ack_received) || last_ack_tries > 10)
                break;
        }
    }

    public void analysis() {
        transmissionEnd = System.currentTimeMillis();
        double time = (transmissionEnd - transmissionStart) * 0.001; // get seconds
        //System.out.println("DATA (Clean):" + fullReads);
        //System.out.println("DATA (I) : " + (fullReads + 1 + retransmissions) );
        //System.out.println("DATA (B) : " + (fullReads + 1 + retransmissions) * (HEADER_SIZE + DATA_SIZE));
        //double dataSize = (fullReads + 1 + retransmissions) * ((HEADER_SIZE + DATA_SIZE) / (double) 1024); // get transmitted data size in KBs
        double dataSize = fileSize / (double)1024;
        //System.out.println("TIME (s)   : " + time);
        //System.out.println("DATA (Kb) : " + dataSize);
        System.out.println(retransmissions + " " + (int) (dataSize / time));
    }

    public void close() {
        try {
            fin.close();
        } catch (IOException e) {
            System.out.println("ERROR: FILE STREAM CANNOT CLOSE");
            System.exit(0);
        }
        // try {
            socketIn.close();
            socketOut.close();
        // } catch (SocketException e) {
        //     System.out.println("ERROR: SOCKET CANNOT CLOSE");
        //     System.exit(0);
        // }
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

    public static final int byteArrayToInt(byte[] bytes) {
        int value = 0;
        for (int i = 0; i < bytes.length; i++) {
            value = value << 8;
            value = value | ((int) bytes[i] & 0xFF);
        }
        return value;
    }

    // PACKET SENDING
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

    public void createPacket() {
        extractDataChunk();

        ByteBuffer bb = ByteBuffer.allocate(HEADER_SIZE + dataByte.length);
        bb.put((byte) 0); /// Offset
        bb.put((byte) 0); /// Octet
        bb.put(intToByteArray(seqNum)); /// Sequence Number
        bb.put(eofFlag); /// EoF
        bb.put(dataByte); /// Data

        byte[] combined = bb.array();

        // create packet
        packetOut = new DatagramPacket(combined, combined.length, address, port);
        //System.out.println("Sending packet size == " + packetOut.getLength());
    }

    public void sendPacket() {
        // System.out.println("Sending packet: " + seqNum);
        try {
            socketOut.send(packetOut);
        } catch (IOException e) {
            System.out.println("ERROR IN SOCKET SENDING");
        }
    }

    // PACKET RECEIVING
    public boolean isACK(int number) {
        int ackNumber = byteArrayToInt(Arrays.copyOfRange(ackData, 2, 4));
        //System.out.println("Receiving packet size == " + ackData.length);
        return number == ackNumber ? true : false;
    }

    public void receivePacket() throws SocketTimeoutException, IOException{
        try {
            socketIn.receive(packetIn);
        } catch (SocketTimeoutException e) {
           throw e;
        } catch (IOException e) {
            throw e;
        }
    }
}
