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
import nu.sitia.loggenerator.inputitems.UDPInputItem;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SelectFilter extends AbstractProcessFilter  {
    static final Logger logger = Logger.getLogger(SelectFilter.class.getName());

    /** Cached regex pattern */
    private Pattern pattern;

    /** for toString() */
    private String regex;

    /**
     * Create a RegexFilter and set all parameters
     * @param config The configuration
     */
    public SelectFilter(Configuration config) {
    }

    @Override
    public boolean setParameter(String key, String value) {
        if (key != null && (key.equalsIgnoreCase("--help") || key.equalsIgnoreCase("-h"))) {
            System.out.println("SelectFilter. Select all messages matching a regex\n" +
                    "Parameters:\n" +
                    "--regex <regex> (-r <regex>)\n" +
                    "  The regex to match\n");
            System.exit(1);
        }
        if (key != null && (key.equalsIgnoreCase("--regex") || key.equalsIgnoreCase("-r"))) {
            this.regex = value;
            logger.fine("regex " + value);
            return true;
        }
        return false;
    }

    @Override
    public boolean afterPropertiesSet() {
        if (regex == null) {
            throw new RuntimeException("Missing --regex parameter");
        }
        pattern = Pattern.compile(regex);
        return true;
    }


    /**
     * Filter one string
     *
     * @param toFilter The string to change
     * @return toFilter with a header added before the string.
     */
    private String filter(String toFilter) {
        Matcher matcher = pattern.matcher(toFilter);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return toFilter;
    }

    @Override
    public List<String> filter(List<String> toFilter) {
        List<String> filtered = new ArrayList<>();
        toFilter.forEach(s ->
                filtered.add(filter(s)));

        return filtered;
    }

    /**
     * The current configuration
     * @return A printout of the current configuration
     */
    @Override
    public String toString() {
        return "SelectFilter" + System.lineSeparator() + regex + System.lineSeparator();
    }
}
