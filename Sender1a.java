/* Forename Surname MatriculationNumber */
/* Boyan Yotov s1509922*/

/* References:
    Started with templates of Server/Client "https://www.baeldung.com/udp-in-java"
    Also file read template "https://stackoverflow.com/questions/858980/file-to-byte-in-java"
    IntToByteArray : https://stackoverflow.com/questions/2183240/java-integer-to-byte-array
    https://stackoverflow.com/questions/5683486/how-to-combine-two-byte-arrays
    Everything else is my personal work.
*/
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.io.*;

public class Sender1a {

    private InetAddress address;
    private String host;
    private int port;

    private byte[] dataByte;
    private DatagramSocket socket;

    File file;
    FileInputStream fin = null;
    byte fileContent[];

    private void setup(String[] args) throws FileNotFoundException, Exception {
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
        host = args[0];

        // initialise other components
        socket = new DatagramSocket();
        address = InetAddress.getByName(host);
        dataByte = new byte[1024];

        // Init file
        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }
    }

    public void run() {
        int seqNum = 0;
        while (extractDataChunk()) {
            byte num[] = intToByteArray(seqNum);
            byte eofFlag = (byte) 0;

            // Combine packet
            List<Byte> packet = new ArrayList<Byte>(Arrays.<Byte>asList(num));
            packet.addAll(Arrays.<Byte>asList(eofFlag));
            packet.addAll(Arrays.<Byte>asList(dataByte));
            byte[] combined = list.toArray(new byte[list.size()]);
            // create packet
            DatagramPacket udpPacket = new DatagramPacket(combined, combined.length, address, port);
            socket.send(packet);
            seqNum++;
        }

        byte num[] = intToByteArray(seqNum);
        byte eofFlag = (byte) 1;

        // Combine packet
        List<Byte> packet = new ArrayList<Byte>(Arrays.<Byte>asList(num));
        packet.addAll(Arrays.<Byte>asList(eofFlag));
        packet.addAll(Arrays.<Byte>asList(dataByte));
        byte[] combined = list.toArray(new byte[list.size()]);
        // create packet
        DatagramPacket udpPacket = new DatagramPacket(combined, combined.length, address, port);
        socket.send(packet);
    }

    public static final byte[] intToByteArray(int value) {
        return new byte[] {
                // (byte)(value >>> 24),
                // (byte)(value >>> 16),
                (byte) (value >>> 8), (byte) value };
    }

    public boolean extractDataChunk() {

        if (fin.read(dataByte) == -1) {
            // its last file.
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        Sender1a sender = new Sender1a();
        sender.setup(args);
        // sender.extractData();
        sender.run();
        sender.fin.close();
    }

    /////////////////////////////////////////////////////////////////////////////////
    public void extractData() throws FileNotFoundException, IOException {
        byte[] input_data = null;
        FileInputStream input_stream = null;
        ByteArrayOutputStream output_stream = null;
        byte[] bytes = null;
        try {
            input_stream = new FileInputStream(file);
            output_stream = new ByteArrayOutputStream();
            copyStream(input_stream, output_stream);
            bytes = output_stream.toByteArray();
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } finally {
            if (input_stream != null) {
                input_stream.close();
                input_stream = null;
            }
            if (output_stream != null) {
                output_stream.close();
                output_stream = null;
            }
        }

    }

    public String sendEcho(String msg) {
        buf = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 4445);
        socket.send(packet);
        packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        String received = new String(packet.getData(), 0, packet.getLength());
        return received;
    }

}