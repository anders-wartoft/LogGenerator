package nu.sitia.loggenerator.inputitems;

import nu.sitia.loggenerator.util.Configuration;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TCPInputItem extends AbstractInputItem {
    static final Logger logger = Logger.getLogger(TCPInputItem.class.getName());
    /** The address:port to connect to as given as a command line argument*/
    private final String hostName;
    /** The socket to use */
    private ServerSocket serverSocket;

    /**
     * Create a new UDPInputItem
     * @param config The Configuration object
     */
    public TCPInputItem(Configuration config) {
        super(config);
        hostName = config.getInputName();
    }

    /**
     * Let the item prepare for reading
     */
    public void setup() throws RuntimeException {
        InetAddress address = null;
        // InputName is the "address:port", or just "port"
        // Port number to connect to
        int port;
        String hostname = null;
        if (hostName.contains(":")) {
            String [] temp = hostName.split(":");
            hostname = temp[0];
            try {
                address = InetAddress.getByName(hostname);
            } catch (UnknownHostException e) {
                throw new RuntimeException("Unknown host: " + hostname, e);
            }
            port = Integer.parseInt(temp[1]);
        } else {
            port = Integer.parseInt(hostName);
        }
        try {
            if (hostname != null) {
                // Listen on a specified address
                serverSocket = new ServerSocket();
                SocketAddress socketAddress = new InetSocketAddress(hostname, port);
                serverSocket.bind(socketAddress);

            } else {
                // Listen to broadcast
                serverSocket = new ServerSocket(port);
            }
        } catch (SocketException e) {
            throw new RuntimeException("Socket exception", e);
        } catch (IOException e) {
            throw new RuntimeException("IOException trying to bind to " + address + ":" + port, e);
        }
        if (hostname != null) {
            logger.info("Serving TCP server on " + hostname + ":" + port);
        } else {
            logger.info("Serving TCP server on port: " + port);
        }
    }

    /**
     * Are there more messages to read?
     * @return True iff there are more messages
     */
    public boolean hasNext() {
        return true;
    }

    /**
     * Read input one line at a time and return
     * {batchSize} elements.
     * @return The read input
     */
    public List<String> next() {
        // Don't batch the TCP received data...
        // int lines = this.batchSize;
        List<String> result = new ArrayList<>();
        try {
            Socket socket = serverSocket.accept();
            logger.log(Level.FINE, "Received connection from " + socket.getRemoteSocketAddress());
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            // Wait until ready
            while (!reader.ready()) {
                Thread.sleep(1);
            }
            // now, read every line
            result = reader.lines().toList();
            reader.close();
        }
        catch (IOException e) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                throw new RuntimeException("Socket receive interrupted");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * Let the item teardown after reading
     */
    public void teardown() {
        // Unreachable code
    }
}
