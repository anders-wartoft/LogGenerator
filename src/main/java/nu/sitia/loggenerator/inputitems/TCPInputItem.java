package nu.sitia.loggenerator.inputitems;

import nu.sitia.loggenerator.util.CommandLineParser;

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

    /** The port to listen on */
    private final int port;

    /** The socket to use */
    private ServerSocket serverSocket;

    /**
     * Create a new UDPInputItem
     * @param args The command line arguments
     */
    public TCPInputItem(String [] args) {
        super(args);
        hostName = CommandLineParser.getCommandLineArgument(args, "host", "host-name", "Host name to bind to");
        String portString = CommandLineParser.getCommandLineArgument(args, "port", "port", "Port to listen on");
        if (null == portString) {
            CommandLineParser.getSeenParameters().forEach((k,v) -> System.out.println(k + " - " + v));
            throw new RuntimeException("Parameter -port (-port) is required for TCP input item");
        }
        port = Integer.parseInt(portString);
    }

    /**
     * Let the item prepare for reading
     */
    public void setup() throws RuntimeException {
        try {
            if (hostName != null) {
                // Listen on a specified address
                serverSocket = new ServerSocket();
                SocketAddress socketAddress = new InetSocketAddress(hostName, port);
                serverSocket.bind(socketAddress);

            } else {
                // Listen to broadcast
                serverSocket = new ServerSocket(port);
            }
        } catch (SocketException e) {
            throw new RuntimeException("Socket exception", e);
        } catch (IOException e) {
            throw new RuntimeException("IOException trying to bind to " + hostName + ":" + port, e);
        }
        if (hostName != null) {
            logger.info("Serving TCP server on " + hostName + ":" + port);
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

    /**
     * Print the configuration
     * @return A printable string of the current configuration
     */
    @Override
    public String toString() {
        return "TCPInputItem" + System.lineSeparator() +
                hostName + System.lineSeparator() +
                port;
    }
}
