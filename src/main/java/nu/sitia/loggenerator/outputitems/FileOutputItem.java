package nu.sitia.loggenerator.outputitems;


import nu.sitia.loggenerator.ShutdownHandler;
import nu.sitia.loggenerator.util.CommandLineParser;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;


public class FileOutputItem extends AbstractOutputItem implements SendListener, ShutdownHandler {
    /** The name of the file this item will write to */
    private final String fileName;

    /**
     * Constructor. Add the callback method from this class.
     * @param args The command line arguments
     */
    public FileOutputItem(String [] args) {
        super(args);
        super.addListener(this);

        fileName = CommandLineParser.getCommandLineArgument(args, "on", "output-name", "Output details");
        if (null == fileName) {
            CommandLineParser.getSeenParameters().forEach((k,v) -> System.out.println(k + " - " + v));
            throw new RuntimeException("Missing -on --output-name argument for -o file");
        }
        addTransactionMessages = true;
    }

    /**
     * Write to console
     * @param elements The element to write
     * @throws RuntimeException Not thrown here
     */
    @Override
    public void write(List<String> elements) throws RuntimeException {
        super.write(elements);
    }

    /**
     * Callback. What to do when the cache is full.
     * Writes to the console.
     * @param toSend String to write
     */
    @Override
    public void send(List<String> toSend)
    {
        boolean append = true;
        try (FileWriter writer = new FileWriter(fileName, append)) {
            for (String s : toSend) {
                String [] lines = s.split("\n");
                for (String line: lines) {
                    writer.write(line);
                    writer.write(System.lineSeparator());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Exception writing to: " + fileName, e);
        }
    }

    /**
     * Print the configuration
     * @return A printable string of the current configuration
     */
    @Override
    public String toString() {
        return "FileOutputItem " +
                fileName;
    }

    /**
     * Shutdown hook
     */
    @Override
    public void shutdown() {
        super.teardown();
    }
}
