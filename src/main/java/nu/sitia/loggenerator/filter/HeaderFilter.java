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

package nu.sitia.loggenerator.filter;

import nu.sitia.loggenerator.Configuration;
import nu.sitia.loggenerator.filter.substituters.Substitution;

import java.util.*;
import java.util.logging.Logger;

public class HeaderFilter extends AbstractProcessFilter  {
    static final Logger logger = Logger.getLogger(HeaderFilter.class.getName());

    /** Cached header template */
    private String header;

    /** Cached list of substitute */
    private final Substitution substitution = new Substitution();

    /** Offset from the current time and date to use when evaluating variables */
    private long timeOffset = 0;

    /** Offset from the current time and date to use when evaluating variables */
    private String offset = "0";

    /**
     * Create a HeaderFilter and set all parameters
     * @param config The configuration
     */
    public HeaderFilter(Configuration config) {

    }

    @Override
    public boolean setParameter(String key, String value) {
        if (key != null && (key.equalsIgnoreCase("--help") || key.equalsIgnoreCase("-h"))) {
            System.out.println("HeaderFilter. Add a header to each message\n" +
                    "Parameters:\n" +
                    "--string <string> (-st <string>)\n" +
                    "  The string to add\n" +
                    "--time-offset <long value> (-to <long value>)\n" +
                    "  The offset in milliseconds to use when evaluating variables\n" +
                    "  Example: --time-offset -10000 for setting the date to 10 seconds ago.\n");
            System.exit(1);
        }
        if (key != null && (key.equalsIgnoreCase("--string") || key.equalsIgnoreCase("-st"))) {
            this.header = value;
            logger.fine("string " + value);
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
        if (header == null) {
            throw new RuntimeException("Missing --string parameter");
        }
        try {
            this.timeOffset = Long.parseLong(offset);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Usage: --time-offset [long value]. Example: --time-offset -10000 for setting the date to 10 seconds ago.");
        }
        return true;
    }

    /**
     * Filter one string
     *
     * @param toFilter The string to change
     * @return toFilter with a header added before the string.
     */
    private String filter(String toFilter) {
        // Fix possible {date...} fields, and {lorem, ...:
        // Create a new date that represents now. Then, add the
        // offset provided by the user (positive for future and negative for in the past
        final Date date = new Date(new Date().getTime() + this.timeOffset);
        String filteredHeader = substitution.substitute(header, new HashMap<>(), date);
        return filteredHeader + toFilter;
    }

    @Override
    public List<String> filter(List<String> toFilter) {
        List<String> filtered = new ArrayList<>();
        toFilter.forEach(s ->
                filtered.add(filter(s)));

        return filtered;
    }

    /**
     * Print the configuration
     * @return A printable string of the current configuration
     */
    @Override
    public String toString() {
        return "HeaderFilter" + System.lineSeparator() + header + System.lineSeparator();
    }
}
