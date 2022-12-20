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

package nu.sitia.loggenerator.filter.substituters;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CounterSubstitute extends AbstractSubstitute {
    /** If no name is given for a counter */
    private static final String DEFAULT_NAME = "defaultName";
    /** Regex for counter */
    private static final String counterRegex = "\\{counter:((?<name>[a-zA-Z0-9\\-_]+):)?(?<startvalue>\\d+)}";
    /** Pattern for counter */
    private static final Pattern counterPattern = Pattern.compile(counterRegex);
    /** The actual counters */
    private static Map<String, Integer> counters = new HashMap<>();


    /**
     * Replace the specification with a number. For each
     * invocation with the same name, use the next number for that name
     * {counter:myCounter:6} will be substituted for 6 on the first
     * invocation, 7 on the next and so on.
     * @param input The string containing the variable specification
     * @return The input but with one of the counter numbers instead of the specification
     */
    public String substitute(String input) {
        int startPos = input.indexOf("{counter:");
        if (startPos < 0) return input;

        int endPos = AbstractSubstitute.getExpressionEnd(input, startPos);
        String part = input.substring(startPos, endPos);
        // First, get the interval
        Matcher matcher = counterPattern.matcher(part);
        if (matcher.find()) {
            String name = matcher.group("name");
            if (name == null) {
                name = DEFAULT_NAME;
            }
            String startValue = matcher.group("startvalue");
            Integer value = Integer.valueOf(startValue);
            // Check if we have had this before
            if (counters.containsKey(name)) {
                value = counters.get(name);
            }
            String result = input.substring(0, startPos) + value + input.substring(endPos);
            value++;
            counters.put(name, value);
            return result;
        }
        throw new RuntimeException(("Illegal counter pattern: " + input));
    }

    /**
     * Reset the internal state:
     */
    public void clear() {
        counters = new HashMap<>();
    }
}
