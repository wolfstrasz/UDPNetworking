
/* Boyan Yotov s1509922 */
/* References:
    Started with templates of Server/Client "https://www.baeldung.com/udp-in-java"
    Also file read template "https://examples.javacodegeeks.com/core-java/io/fileinputstream/read-file-in-byte-array-with-fileinputstream/"
    Everything else is my personal work.
*/
/* The file below will be extended to accomodate other types of Clients */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import utils.ArrayUtils;
import utils.FileUtils;

public abstract class Client {

    private int argSize;
    //////////////////////
    private DatagramSocket socket;
    private DatagramPacket sentPacket;
    private DatagramPacket receivedPacket;
    private byte[] data;
    /** file to send **/
    protected File input;
    private String filename;
    /** re-send packets if an acknowledgement is not received within this time **/
    private int retry_timeout;
    /** host to use **/
    private String host;
    private int port;
    private InetAddress ip;

    Client(int argSize) {
        this.argSize = argSize;
    }

    public void SetUp(String[] args) {
        /* args: <RemoteHost> <Port> <Filename> */

        // initialise network
        socket = new DatagramSocket();
        address = InetAddress.getByName(args[0]);
        port = Integer.parseInt(args[1]);
        filename = args[3];

        // initialise file read
        file = new File(filename);
        try {
            // create FileInputStream object
            fin = new FileInputStream(file);

            fileContent = new byte[(int) file.length()];

            // // Reads up to certain bytes of data from this input stream into an array of
            // // bytes.
            // fin.read(fileContent);
            // // create string from byte array
            // String s = new String(fileContent);
            // System.out.println("File content: " + s);
        } catch (FileNotFoundException e) {
            System.out.println("File not found" + e);
        } catch (IOException ioe) {
            System.out.println("Exception while reading file " + ioe);
        }

    }
}