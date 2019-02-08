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



public class Receiver1a extends Thread{

    private DatagramSocket socket;
    private boolean running;
    private File file;
    private int port;
    private byte[] dataByte = new byte[1024];
    private FileOutputStream fout = null;

/*
    public EchoServer() {
        socket = new DatagramSocket(4445);
    }
*/
    private void setup(String[] args) {
        /* args: <RemoteHost> <Port> <Filename> */

        // Try to create file
    //    file = new File("heelo");
    //    if (!file.exists()) {
    //        file.createNewFile();
        //    System.out.println("File not found IT");
        //    System.exit(0);
    //    }


        // Try to parse Port number
        try {
            this.port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Port is not an Integer");
            System.exit(0);
        }
        // Try to get host
        // host = args[0];

        // initialise other components
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            System.out.println("SOCKET EXCEPTION");
        }
        // address = InetAddress.getByName(host);
        dataByte = new byte[1027];

        // initfile
        try {
            fout = new FileOutputStream(args[1], false);
        } catch (FileNotFoundException e) {
            //throw e;
        } catch (IOException e) {
            //throw e;
        }
    }

    public static final int byteArrayToInt(byte[] bytes) {
        int value = 0;
        for (int i = 0; i < bytes.length; i++) {
            value = value << 8;
            value = value | ((int) bytes[i] & 0xFF);
        }
        return value;
    }

    public void run() {

        running = true;

        while (running) {
            System.out.println("running");
            //DatagramPacket packet = new DatagramPacket(dataByte, 2 + 1 + 1024);
            DatagramPacket packet = new DatagramPacket(dataByte, 1024 + 1 + 2);
            try {
                socket.receive(packet);
                System.out.println("got packet");
            } catch (IOException e){
                System.out.println("ERROR IN SOCKET RECEIVING");

            }


            // remove packet header
            byte[] seqNum = Arrays.copyOfRange(dataByte, 0, 1);
            System.out.println("Receive packet: " + seqNum);
            byte eofFlag = dataByte[2];
            System.out.println("eof : " +( (int) eofFlag & 0xFF));
            byte[] data = new byte[1024];
            data = Arrays.copyOfRange(dataByte, 3, packet.getLength());
            // write data
            // ...
            try {
                fout.write(data);
            } catch (IOException e) {
                System.out.println("ERROR IN Writing to file");

            }
            // if EOF break;
            if (((int) eofFlag & 0xFF) == 1){
                running = false;
            }
        }
        try {
            fout.close();
        } catch (IOException e){
            System.out.println("ERROR IN Closing file");

        }
        // running = true;

        // while (running) {
        // DatagramPacket packet = new DatagramPacket(buf, buf.length);
        // socket.receive(packet);

        // InetAddress address = packet.getAddress();
        // int port = packet.getPort();
        // packet = new DatagramPacket(buf, buf.length, address, port);
        // String received = new String(packet.getData(), 0, packet.getLength());

        // if (received.equals("end")) {
        // running = false;
        // continue;
        // }
        // socket.send(packet);
        // }
        // socket.close();
    }

    public static void main(String[] args) {
        Receiver1a receiver = new Receiver1a();
        receiver.setup(args);
        System.out.println("Receiver1a started!");
        receiver.run();
        System.out.println("Receiver1a Finished!");
    }

}
