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

package nu.sitia.loggenerator.outputitems;


import nu.sitia.loggenerator.Configuration;
import nu.sitia.loggenerator.ShutdownHandler;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;


public class FileOutputItem extends AbstractOutputItem implements SendListener, ShutdownHandler {
    /** The name of the file this item will write to */
    private final String fileName;

    /**
     * Constructor. Add the callback method from this class.
     * @param config The command line arguments
     */
    public FileOutputItem(Configuration config) {
        super(config);
        super.addListener(this);

        fileName = config.getValue("-ofn");
        if (null == fileName) {
            throw new RuntimeException(config.getNotFoundInformation("-ofn"));
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
