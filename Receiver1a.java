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
import java.io.*;

public class Receiver1a {

    private DatagramSocket socket;
    private boolean running;
    private byte[] dataByte = new byte[1024];
    private FileOutputStream fout = null;

    public EchoServer() {
        socket = new DatagramSocket(4445);
    }

    private void setup(String[] args) throws FileNotFoundException, Exception {
        /* args: <RemoteHost> <Port> <Filename> */

        // Try to create file
        file = new File(args[1]);
        if (!file.exists()) {
            System.out.println("File not found");
            System.exit(0);
        }

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
        socket = new DatagramSocket();
        // address = InetAddress.getByName(host);
        dataByte = new byte[1024];

        // initfile
        try {
            fout = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
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
            DatagramPacket packet = new DatagramPacket(dataByte, 2 + 1 + 1024);
            socket.receive(packet);
            byte eofFlag;
            // remove packet header
            eofFlag = dataByte[2];
            Arrays.copyOfRange(dataByte, 3, packet.getLength());
            // write data
            // ...
            fout.write(databyte);
            // if EOF break;
            if (((int) eofFlag & 0xFF) == 1)
                running = false;
        }
        fout.close();
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

        System.out.println("Receiver1a started!");
    }

}