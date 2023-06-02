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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RepeatSubstitute extends AbstractSubstitute {
    /** Regex for repeat */
    private static final String repeatRegex = "\\{repeat:(?<times>[^#]+)#(?<torepeat>[^#]+)(#(?<delimiter>[^#]+))?#}";
    /** Pattern for repeat */
    private static final Pattern repeatPattern = Pattern.compile(repeatRegex);

    private Substitution substitution;

    /**
     * The first parameter must be able to contain variables. In order to resolve them,
     * we must be able to call the substitution recursively. Save the class for that purpose.
     * If the last parameter is used, the last parameter is inserted between all instances
     * of torepeat.
     * @param substitution The class to use to resolve the times parameter.
     */
    public RepeatSubstitute(Substitution substitution) {
        this.substitution = substitution;
    }

    /**
     * Replace the specification with a number of occurrences of the second parameter.
     * The number of times to repeat is given as the first parameter.
     * The first parameter can be a variable, like {random:1-10}.
     * If used, the third parameter is a delimiter for the result.
     * @param input The string containing the variable specification
     * @return The input but with one of the replace variables instead of the specification
     */
    public String substitute(String input) {
        int startPos = input.indexOf("{repeat:");
        if (startPos < 0) return input;

        int endPos = AbstractSubstitute.getExpressionEnd(input, startPos);
        String part = input.substring(startPos, endPos);
        // First, get the number of times
        Matcher matcher = repeatPattern.matcher(part);
        if (matcher.find()) {
            int repeat;
            String times = matcher.group("times");
            if (times.indexOf("{") >= 0) {
                // Resolve recursively
                times = substitution.substitute(times, new HashMap<>(), new Date());
            }
            // Now, the variable should be an integer
            repeat = Integer.parseInt(times);

            String toRepeat = matcher.group("torepeat");
            String delimiter = matcher.group("delimiter");

            StringBuilder sb = new StringBuilder();
            for (int i=0; i<repeat; i++) {
                sb.append(toRepeat);
                if (delimiter != null && i+1 < repeat) {
                    sb.append(delimiter);
                }
            }
            String result = input.substring(0, startPos) + sb.toString() + input.substring(endPos);
            return result;
        }
        throw new RuntimeException(("Illegal resolve pattern: " + input));
    }

    /**
     * Reset the internal state:
     */
    public void clear() {
    }
}
