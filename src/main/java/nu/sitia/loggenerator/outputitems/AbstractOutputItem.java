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

import java.util.*;

public abstract class AbstractOutputItem implements OutputItem {
    /** How many lines will be returned in one batch? */
    protected int batchSize;

    /** Callback method for sending the cache */
    private SendListener sl = null;

    /** If batchSize > 1, then store the elements in a cache until they can be written */
    private final List<String> cache = new ArrayList<>();

    /** Should this output item add transaction messages? */
    protected boolean addTransactionMessages = false;

    /** Print transaction messages? */
    public boolean printTransactionMessages() {
        return addTransactionMessages;
    }

    /**
     * When the cache reaches batchSize numbers, or when the teardown is called,
     * the SendListener object will receive a call to the 'send()' method.
     * @param sl An object implementing the SendListener interface.
     */
    @Override
    public void addListener (SendListener sl) {
        this.sl = sl;
    }

    /**
     * Save the output batch size
     * @param config The command line arguments
     */
    public AbstractOutputItem(Configuration config) {
        String batchString = config.getValue("-ob");
        if (null != batchString) {
            batchSize = Integer.parseInt(batchString);
        } else {
            batchSize = 0; // Default value
        }
    }

    /**
     * How many elements should be read at a time?
     * Default is 1.
     * @param size The new size
     */
    public void setBatchSize(int size) {
        this.batchSize = size;
    }

    /**
     * Add one element to the cache. If the cache is full, call the
     * 'send()' method in the SendListener.
     * @param elements The element to add
     */
    public void write(List<String> elements) throws RuntimeException {
        // We must have a send method to call...
        assert(sl != null);
        cache.addAll(elements);
        if (cache.size() >= batchSize && cache.size() > 0) {
            sendBatch(cache);
            cache.clear();
        }
    }

    /**
     * Call sl.send() with at most batch size items
     * @param elements What to send
     */
    public void sendBatch(List<String> elements) {
        List<String> batch = new LinkedList<>();
        if (batchSize > 0) {
            // The input can be many lines in one element.
            // Unpack the elements and add to the batch
            elements.forEach(e -> batch.addAll(Arrays.asList(e.split("\n"))));

            // Now, repack in a good size for the output (batchSize)
            List<String> output = new LinkedList<>();
            for (int j = 0; j < batch.size(); j += batchSize) {
                for (int i = 0; i < batchSize && i + j < batch.size(); i++) {
                    output.add(batch.get(i + j));
                }
                sl.send(List.of(String.join("\n", output)));
                output.clear();
            }
        } else {
            // batchSize == 0, just send everything
            sl.send(elements);
        }
    }

    /**
     * Setup the environment.
     * @throws RuntimeException Not thrown here
     */
    @Override
    public void setup() throws RuntimeException {

    }

    /**
     * Tear down the environment.
     */
    @Override
    public void teardown() {
        if (!cache.isEmpty()) {
            sl.send(cache);
            cache.clear();
        }
    }

}
