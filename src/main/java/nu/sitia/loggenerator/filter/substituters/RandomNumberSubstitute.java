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

package nu.sitia.loggenerator.filter.substituters;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RandomNumberSubstitute extends AbstractSubstitute {
    /** Regex for random number */
    private static final String randomRegex = "\\{random:(?<from>\\d+)-(?<to>\\d+)}";

    /** Cached pattern for random */
    private static final Pattern randomPattern = Pattern.compile(randomRegex);


    /**
     * Get a random number in a specified interval.
     * {random:1-6} will randomly pick 1, 2, 3, 4, 5 or 6.
     * @param input The string containing the random variable specification
     * @return The input but with one of the random number instead of the specification
     */
    public String substitute(String input) {
        int startPos = input.indexOf("{random:");
        if (startPos < 0) return input;

        int endPos = AbstractSubstitute.getExpressionEnd(input, startPos);
        String part = input.substring(startPos, endPos);
        // First, get the interval
        Matcher matcher = randomPattern.matcher(part);
        if (matcher.find()) {
            String from = matcher.group(1);
            String to = matcher.group(2);
            int low = Integer.parseInt(from);
            int high = Integer.parseInt(to);
            int nr = new Random().nextInt(1 + high - low) + low;
            return input.substring(0, startPos) + nr + input.substring(endPos);
        }
        throw new RuntimeException(("Illegal random pattern: " + input));
    }

}
