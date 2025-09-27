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
 *  WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nu.sitia.loggenerator.inputitems;

import nu.sitia.loggenerator.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * A CounterInputItem always return the same string but with an
 * ever-increasing number added at the end.
 */
public class CounterInputItem extends AbstractInputItem {
    /** The string to return */
    private String string = null;

    /** The number to send */
    private long number = 1;

    /**
     * Create a new StaticInputItem
     */
    public CounterInputItem(Configuration config) {
        super(config);
    }

    @Override
    public boolean setParameter(String key, String value) {
        if (key != null && (key.equalsIgnoreCase("--help") || key.equalsIgnoreCase("-h"))) {
            System.out.println("CounterInputItem. Return a string with an ever-increasing number\n" +
                    "Parameters:\n" +
                    "--string <string> (-st <string>)\n" +
                    "  The string to return\n" +
                    "--start-number <number> (-sn <number>)\n" +
                    "  The number to start with (default 1)\n");
        }
        if(super.setParameter(key, value)) {
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--string") || key.equalsIgnoreCase("-st"))) {
            this.string = value;
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--start-number") || key.equalsIgnoreCase("-sn"))) {
            try {
                this.number = Long.parseLong(value);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number format for --start-number");
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean afterPropertiesSet() {
        return this.string != null;
    }

    /**
     * Let the item prepare for reading
     */
    public void setup() throws RuntimeException {
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
        List<String> result = new ArrayList<>();
        for (int i=0; i<batchSize; i++) {
            result.add(string + number++);
        }
        return result;
    }

    /**
     * Let the item teardown after reading
     */
    public void teardown() {
    }

    /**
     * Debug printouts (logger)
     * @return The configuration for this item
     */
    public String toString() {
        return "CounterInputItem" + System.lineSeparator()
         + this.string + this.number;
    }
}
