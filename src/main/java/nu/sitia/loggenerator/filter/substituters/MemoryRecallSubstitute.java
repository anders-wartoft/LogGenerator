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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MemoryRecallSubstitute extends AbstractSubstitute {
    /** If no name is given for a mr */
    private static final String DEFAULT_NAME = MemorySetSubstitute.DEFAULT_NAME;
    /** Regex for mr */
    private static final String mrRegex = "\\{mr(:(?<name>[a-zA-Z0-9\\-_]+))?}";
    /** Pattern for mr */
    private static final Pattern mrPattern = Pattern.compile(mrRegex);

    /**
     * Replace the specification with a string that has been saved earlier
     * with {ms/...}.
     * E.g.,
     * {ms/FUBAR}
     * will evaluate to
     * FUBAR
     * {ms/FUBAR} {mr}
     * will evaluate to
     * FUBAR FUBAR
     * since mr will recall what was saved earlier.
     * MemorySave can take a name parameter:
     * {ms:name1/A} {ms:name2/B} {mr:name1}
     * Will evaluate to
     * A B A
     * Since the MemoryRecall will use the name 'name1' to look up
     * the value from the internal cache.
     * @param input The string containing the variable specification
     * @return The value that has been saved earlier with the same (or omitted) name in a {ms - statement
     */
    public String substitute(String input) {
        int startPos = input.indexOf("{mr");
        if (startPos < 0) return input;

        int endPos = AbstractSubstitute.getExpressionEnd(input, startPos);
        String part = input.substring(startPos, endPos);
        // First, get the interval
        Matcher matcher = mrPattern.matcher(part);
        if (matcher.find()) {
            String name = matcher.group("name");
            if (name == null) {
                name = DEFAULT_NAME;
            }
            String value = MemorySetSubstitute.getCounterValue(name);
            if (value == null) {
                value = "";
            }
            String result = input.substring(0, startPos) + value + input.substring(endPos);
            return result;
        }
        throw new RuntimeException(("Illegal mr pattern: " + input));
    }

}
