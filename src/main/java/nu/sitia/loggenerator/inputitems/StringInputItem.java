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
import nu.sitia.loggenerator.filter.substituters.Substitution;
import java.util.*;
import java.util.logging.Logger;

/**
 * A StringInputItem is similar to a TemplateInputItem, but 
 * a string will be used as a template instead. 
 * If string is set to:
 * - continuous:
 * A random line will be selected, expanded and sent, using
 * the string as a template. This is good for generating a lot of logs.
 * - once:
 * The contents will be sent once, but all variables will
 * be expanded (like {date:...}, {oneOf:...} etc).
 */
public class StringInputItem extends AbstractInputItem {
    static final Logger logger = Logger.getLogger(StringInputItem.class.getName());

    /** Describes how this item should function */
    private String template;

    /** Offset from the current time and date to use when evaluating variables */
    private long timeOffset = 0;

    /** The offset as a String */
    private String offset = "0";

    /** Time value if template time:000 */
    private long timeValue = 0;

    /* The variable string */
    private String from = "";

    /** The cached substitution handler */
    final Substitution substitution = new Substitution();

    /** Are there more data to read */
    private boolean hasNext = true;

    /**
     * Create a new TemplateFileInputItem
     */
    public StringInputItem(Configuration config) {
        super(config);
    }

    @Override
    public boolean setParameter(String key, String value) {
        if (key != null && (key.equalsIgnoreCase("--help") || key.equalsIgnoreCase("-h"))) {
            System.out.println("StringInputItem. Load a string from the command line as a template and resolve variables before sending.\n" +
                    "Parameters:\n" +
                    "--from <string> (-fr <string>)\n" +
                    "  The string to use as a template. This string will be used as a template and all variables will be expanded.\n" +
                    "  Example: --from \"This is a test {date:yyyy-MM-dd HH:mm:ss}\"\n" +
                    "  Example: --from \"This is a test {date:yyyy-MM-dd HH:mm:ss} {oneOf:one,two,three}\"\n" +
                    "  Example: --from \"This is a test {date:yyyy-MM-dd HH:mm:ss} {oneOf:one,two,three} {random:1,2,3}\"\n" +
                    "--template <template> (-t <template>)\n" +
                    "  The template to use. One of: once, time:{number} or continuous\n" +
                    "  once: Send the data once \n" +
                    "  continuous: Send the string with variables expanded. If you just want a specified number of events, add the -l (--limit) parameter to stop sending after the specified number of events.\n" +
                    "  time:{number}: as 'continuous' but end the transmission after {number} ms\n" +
                    "--time-offset <long value> (-to <long value>)\n" +
                    "  The offset in milliseconds to use when evaluating variables\n" +
                    "  Example: --time-offset -10000 for setting the date to 10 seconds ago.\n");
            System.exit(1);
        }
        if (super.setParameter(key, value)) {
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--from") || key.equalsIgnoreCase("-fr"))) {
            if (value == null) {
                throw new RuntimeException("Missing -from parameter");
            }
            this.from = value;
            logger.fine("from " + value);
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--template") || key.equalsIgnoreCase("-t"))) {
            if (value == null) {
                throw new RuntimeException("Missing -template parameter");
            }
            if (value.equals("once") || value.equals("continuous") || value.startsWith("time:")) {
                this.template = value;
            } else {
                throw new RuntimeException("Invalid template: '" + value + "'");
            }
            if (value.startsWith("time:")) {
                String time = value.substring(5);
                try {
                    this.timeValue = Long.parseLong(time) + new Date().getTime();
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid time value: " + time);
                }
            }
            logger.fine("template " + value);
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--time-offset") || key.equalsIgnoreCase("-to"))) {
            this.offset = value;
            logger.fine("timeOffset " + value);
            return true;
        }
        return false;
    }

    @Override
    public boolean afterPropertiesSet() {
        if (template == null) {
            throw new RuntimeException("Missing -template parameter");
        }

        try {
            this.timeOffset = Long.parseLong(offset);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Usage: --time-offset [long value]. Example: --time-offset -10000 for setting the date to 10 seconds ago.");
        }
        return true;
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
        return this.hasNext;
    }

    /**
     * Read input one line at a time and return
     * {batchSize} elements.
     * @return The read input
     */
    public List<String> next() {
        List<String> result = new ArrayList<>();
        if (this.timeValue > 0) {
            // We are limited in time (for example with flag -t time:1000)
            long now = new Date().getTime();
            if (now > this.timeValue) {
                // We are done now
                this.hasNext = false;
                return result;
            }
        }

        // Generate batchSize lines as separate items
        int lines = this.batchSize;
        Map<String, String> map = new HashMap<>();
        while (lines-- > 0) {
            String row = substitution.substitute(this.from, map, new Date(new Date().getTime() + timeOffset));
            result.add(row);
        }
        
        if (this.template.equals("once")) {
            // We are done now
            this.hasNext = false;
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
     * @return The filename for this item
     */
    public String toString() {
        return"StringInputItem" + System.lineSeparator() + this.from;
    }
}
