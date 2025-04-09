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

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public class GuardFilter extends AbstractProcessFilter {
    static final Logger logger = Logger.getLogger(GuardFilter.class.getName());

    /**
     * Create a guardFilter and set all parameters
     * @param ignoredConfig The configuration object to get parameters from
     */
    public GuardFilter(Configuration ignoredConfig) {
    }

    @Override
    public boolean setParameter(String key, String value) {
        if (key != null && (key.equalsIgnoreCase("--help") || key.equalsIgnoreCase("-h"))) {
            System.out.println("GuardFilter. Remove all guards\n" +
                    "Parameters:\n" +
                    "  None\n");
            System.exit(1);
        }
        return false;
    }

    @Override
    public boolean afterPropertiesSet() {
        return true;
    }

    /**
     * Don't add the line if it is a statistics guard
     * @param toFilter The data to filter
     * @return The data without transmission and file guards.
     */
    @Override
    public List<String> filter(List<String> toFilter) {
        List<String> result = new LinkedList<>();

        toFilter.forEach(s -> {
            String filteredString = filterLine(s);
            if (filteredString != null && filteredString.length() > 0) {
                result.add(filterLine(s));
            }
        });
        return result;
    }

    /**
     * Each line can contain several events, separated by a newline
     * @param line The line to check
     */
    private String filterLine(String line) {
        List<String> result = new LinkedList<>();

        // The input can be many lines in one element.
        for (String s : line.split("\n")) {
            if (!removeLine(s)) {
                result.add(s);
            }
        }
        return String.join("\n", result);
    }


    /**
     * If the line contains any guard, then return true
     * @param toCheck The line to check
     * @return True iff toCheck contains a guard
     */
    public boolean removeLine(String toCheck) {
        return (toCheck.contains(Configuration.BEGIN_TRANSACTION_TEXT)
            ||  toCheck.contains(Configuration.END_TRANSACTION_TEXT)
            ||  toCheck.contains(Configuration.BEGIN_FILE_TEXT)
            ||  toCheck.contains(Configuration.END_FILE_TEXT));
    }

    /**
     * Print the configuration
     * @return A printable string of the current configuration
     */
    @Override
    public String toString() {
        return "GuardFilter" + System.lineSeparator();
    }
}
