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

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OneOfSubstitute extends AbstractSubstitute {

    /** Regex for oneOf */
    private static final String oneOfRegex = "\\{oneOf:(?<options>.*)(/(?<delimiter>.))?}";

    /** Cached pattern for getting oneOf */
    private static final Pattern oneOfPattern = Pattern.compile(oneOfRegex);

    /**
     * Get one of several options.
     * {oneOf:a,b,c,d} will randomly pick a b c or d.
     * If , is needed in the options, use the following structure:
     * {oneOf:a;b;c;d/;}
     * @param input The string containing the ip variable specification
     * @return The input but with one of the choices instead of the specification
     */
    public String substitute(String input) {
        // Since Java doesn't have recursive regexes, we search for the end of the expression manually:
        int startPos = input.indexOf("{oneOf:");
        if (startPos < 0) return input;

        int endPos = getExpressionEnd(input, startPos);
        String part = input.substring(startPos, endPos);
        // First, get the choices
        Matcher matcher = oneOfPattern.matcher(part);
        if (matcher.find()) {
            String delimiter = ",";
            String choicesString = matcher.group(1);
            if (matcher.groupCount() > 1) {
                if (matcher.group(2) != null) {
                    delimiter = matcher.group(2);
                }
            }
            String [] choices = choicesString.split(delimiter);
            int nr = new Random().nextInt(choices.length);
            String selected = choices[nr];
            return input.substring(0, startPos) + selected + input.substring(endPos);
        }
        throw new RuntimeException(("Illegal oneOf pattern: " + input));
    }

}
