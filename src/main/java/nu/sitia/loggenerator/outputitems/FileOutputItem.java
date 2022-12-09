package nu.sitia.loggenerator.outputitems;


import nu.sitia.loggenerator.util.Configuration;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class FileOutputItem extends AbstractOutputItem implements SendListener {
    static Logger logger = Logger.getLogger(FileOutputItem.class.getName());
    /** The name of the file this item will write to */
    private final String fileName;

    /**
     * Constructor. Add the callback method from this class.
     * @param config The Configuration object
     */
    public FileOutputItem(Configuration config) {
        super();
        super.addListener(this);
        this.fileName = config.getOutputName();
        setBatchSize(config.getInputBatchSize());
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
                writer.write(s);
            }
        } catch (IOException e) {
            throw new RuntimeException("Exception writing to: " + fileName, e);
        }
    }

}
