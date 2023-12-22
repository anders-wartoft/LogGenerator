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
import nu.sitia.loggenerator.ShutdownHandler;

public abstract class AbstractInputItem implements InputItem, ShutdownHandler {
    /** How many lines will be returned in one batch? */
    protected int batchSize;

    /** The configuration */
    protected Configuration config;


    /**
     * Set the input batch size
     */
    public AbstractInputItem(Configuration config) {
        this.config = config;
    }

    /**
     * Set a parameter for the item
     * @param key The key of the parameter
     * @param value The value of the parameter
     * @return true if the parameter was consumed, false otherwise
     */
    public boolean setParameter(String key, String value) {
        if (key != null && (key.equalsIgnoreCase("--help") || key.equalsIgnoreCase("-h"))) {
            System.out.println(
                    "--batch-size <batch-size> (-bs <batch-size>)\n");
            System.exit(1);
        }
        if (key != null && (key.equalsIgnoreCase("--batch-size") || key.equalsIgnoreCase("-bs"))) {
            batchSize = Integer.parseInt(value);
            return true;
        } else {
            batchSize = 1; // Default value
        }
        return false;
    }

    public void shutdown() {

    }
}
