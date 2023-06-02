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

public class CounterMemoryRecallSubstitute extends AbstractSubstitute {
    /** If no name is given for a cmr */
    private static final String DEFAULT_NAME = CounterSubstitute.DEFAULT_NAME;
    /** Regex for cmr */
    private static final String cmrRegex = "\\{cmr(:(?<name>[a-zA-Z0-9\\-_]+))?}";
    /** Pattern for cmr */
    private static final Pattern cmrPattern = Pattern.compile(cmrRegex);

    /**
     * Replace the specification with a number. For each
     * invocation with the same name, use the next number for that name
     * {cmr:myCounter} will be substituted for the value of the counter:myCounter
     * @param input The string containing the variable specification
     * @return The input but with one of the counter numbers instead of the specification
     */
    public String substitute(String input) {
        int startPos = input.indexOf("{cmr");
        if (startPos < 0) return input;

        int endPos = AbstractSubstitute.getExpressionEnd(input, startPos);
        String part = input.substring(startPos, endPos);
        // First, get the interval
        Matcher matcher = cmrPattern.matcher(part);
        if (matcher.find()) {
            String name = matcher.group("name");
            if (name == null) {
                name = DEFAULT_NAME;
            }
            Integer counterValue = CounterSubstitute.getCounterValue(name);
            String value = "";
            if (counterValue != null) {
                value = String.valueOf(counterValue);
            }
            String result = input.substring(0, startPos) + value + input.substring(endPos);
            return result;
        }
        throw new RuntimeException(("Illegal counter pattern: " + input));
    }

}
