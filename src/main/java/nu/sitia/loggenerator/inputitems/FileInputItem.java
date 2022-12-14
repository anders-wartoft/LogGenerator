/*
 * Copyright 2022 sitia.nu https://github.com/anders-wartoft/LogGenerator
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 *  WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nu.sitia.loggenerator.inputitems;

import nu.sitia.loggenerator.Configuration;
import nu.sitia.loggenerator.util.CommandLineParser;


import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class FileInputItem extends AbstractInputItem {
    /** The name of the file this item will read from */
    protected final String fileName;

    /** The scanner to read from. Initialized in setup() */
    protected Scanner scanner = null;

    /** If false, start with sending guard message if statistics is enabled */
    protected boolean initialized = false;

    /** Should we add statistics messages to the file contents? */
    protected boolean isStatistics;

    /**
     * Create a new FileInputItem
     * @param args The command line arguments
     */
    public FileInputItem(String fileName, String [] args) {
        super(args);
        this.fileName = fileName;
        if (fileName == null) {
            CommandLineParser.getSeenParameters().forEach((k,v) -> System.out.println(k + " - " + v));
            throw new RuntimeException("Required parameter '-fn' or '--file-name' not found.");
        }
        String statisticsParameter = CommandLineParser.getCommandLineArgument(args, "s", "statistics", "Add statistics messages and printouts");
        this.isStatistics = statisticsParameter != null &&
                        statisticsParameter.equalsIgnoreCase("true");
    }

    /**
     * Let the item prepare for reading
     */
    public void setup() throws RuntimeException {
        FileInputStream input;
        File file = new File(fileName);
        if (!file.exists()) {
            throw new RuntimeException("File not found: " + file.getAbsolutePath());
        }
        try {
            input = new FileInputStream(fileName);
            scanner = new Scanner(input);
        } catch (FileNotFoundException e) {
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
            if (isStatistics) {
                result.add(Configuration.BEGIN_FILE_TEXT + fileName);
            }
        }
        int lines = this.batchSize;
        while (scanner.hasNextLine() && lines-- > 0) {
            result.add(scanner.nextLine());
        }
        if (!scanner.hasNextLine() && isStatistics) {
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
     * Make adding two items to a list depending on the id only
     * @param o Other object
     * @return true Iff the id of the two items are identical, and not nul.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileInputItem that = (FileInputItem) o;
        return fileName.equals(that.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName);
    }

    /**
     * Debug printouts (logger)
     * @return The filename for this item
     */
    public String toString() {
        return "FileInputItem" + System.lineSeparator() +
                this.fileName;
    }
}
