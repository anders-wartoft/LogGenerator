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

package nu.sitia.loggenerator.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class CommandLineParser {

    private static final Map<String, String> seenParameters = new LinkedHashMap<>();

    /**
     * Helper function to get a command line argument
     * @param args The command line arguments
     * @param shortName A name abbreviation for the variable
     * @param longName The name of the variable
     * @param description A description for the variable
     * @return The value for that variable
     */
    public static String getCommandLineArgument(String [] args, String shortName, String longName, String description) {
        seenParameters.put(longName, description);
        String result = null;
        for (int i=0; i<args.length; i++) {
            String arg = args[i];
            String name1 = "-" + shortName;
            String name2 = "--" + longName;
            if (arg.equalsIgnoreCase(name1) || arg.equalsIgnoreCase(name2)) {
                if (i < args.length-1) {
                    result = args[i+1];
                } else {
                    System.err.println("-" + shortName + " or " + "--" + longName + " missing parameter. " + description);
                }
            }
        }
        return result;
    }

    /**
     * Return a list of parameters that can be set for this invocation
     * @return The parameters seen by this instance
     */
    public static Map<String, String> getSeenParameters() {
        return Collections.unmodifiableMap(seenParameters);
    }
}