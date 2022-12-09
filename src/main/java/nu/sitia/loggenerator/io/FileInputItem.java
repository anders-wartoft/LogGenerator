package nu.sitia.loggenerator.io;

import nu.sitia.loggenerator.util.Configuration;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FileInputItem extends AbstractInputItem {
    /** The name of the file this item will read from */
    private final String fileName;

    /** The scanner to read from. Initialized in setup() */
    private Scanner scanner = null;

    /** The config object */
    private final Configuration config;

    /** If false, start with sending guard message if statistics is enabled */
    private boolean initialized = false;

    /**
     * Create a new FileInputItem
     * @param config The Configuration object
     */
    public FileInputItem(String name, Configuration config) {
        this.fileName = name;
        this.config = config;
        setBatchSize(config.getInputBatchSize());
    }

    /**
     * Let the item prepare for reading
     */
    public void setup() throws RuntimeException {
        FileInputStream input;
        try {
            input = new FileInputStream(fileName);
            scanner = new Scanner(input);
        } catch (FileNotFoundException e) {
            File file = new File(fileName);
            throw new RuntimeException("File not found: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Are there more messages to read?
     * @return True iff there are more messages
     */
    public boolean hasNext() {
        return scanner.hasNext();
    }

    /**
     * Read input one line at a time and return
     * {batchSize} elements.
     * @return The read input
     */
    public List<String> next() {
        List<String> result = new ArrayList<>();
        if (!initialized) {
            initialized = true;
            if (config.isStatistics()) {
                result.add(Configuration.BEGIN_FILE_TEXT + fileName);
            }
        }
        int lines = this.batchSize;
        while (scanner.hasNextLine() && lines-- > 0) {
            result.add(scanner.nextLine());
        }
        if (!scanner.hasNextLine() && config.isStatistics()) {
            // End of file
            result.add(Configuration.END_FILE_TEXT + fileName);
        }
        return result;
    }

    /**
     * Let the item teardown after reading
     */
    public void teardown() {
        scanner.close();
        // make sure we have to run setup() again before read()
        scanner = null;
    }

    /**
     * Debug printouts (logger)
     * @return The filename for this item
     */
    public String toString() {
        return this.fileName;
    }
}
