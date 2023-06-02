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

public class ProbSubstitute extends AbstractSubstitute {

    /** Regex for prob */
    private static final String probRegex = "\\{prob:(?<options>.*)}";

    /** Cached pattern for getting prob */
    private static final Pattern probPattern = Pattern.compile(probRegex);

    /**
     * Get one of several options.
     * {prob:a/1,b/2,c/2,d/1} will randomly pick a b c or d, but b and c will have a double
     * probability to be picked, compared to a or d.
     * @param input The string containing the ip variable specification
     * @return The input but with one of the choices instead of the specification
     */
    public String substitute(String input) {
        // Since Java doesn't have recursive regexes, we search for the end of the expression manually:
        int startPos = input.indexOf("{prob:");
        if (startPos < 0) return input;

        int endPos = getExpressionEnd(input, startPos);
        String part = input.substring(startPos, endPos);
        // First, get the choices
        Matcher matcher = probPattern.matcher(part);
        if (matcher.find()) {
            String delimiter = ",";
            String choicesString = matcher.group(1);
            String [] choices = choicesString.split(delimiter);
            String [] str = new String[choices.length];
            int [] probability = new int [choices.length];
            for (int i=0; i<choices.length; i++) {
                String [] strprob = choices[i].split("/");
                str[i] = strprob[0];
                if (strprob.length > 1) {
                    probability[i] = Integer.parseInt(strprob[1]);
                } else {
                    probability[i] = 1;
                }
            }
            // Now we have two arrays, one with the values (str) and one with the relative probabilities
            // Sum up the relative probabilities
            int sumProbabilities = 0;
            for (int i=0; i<probability.length; i++) {
                sumProbabilities += probability[i];
            }
            // Now pick one of them
            int nr = new Random().nextInt(sumProbabilities) + 1;
            int sum = 0;
            String selected = null;
            for (int i=0; i<probability.length; i++) {
                sum += probability[i];
                if (sum >= nr && selected == null) {
                    // Pick this one
                    selected = str[i];
                }
            }
            return input.substring(0, startPos) + selected + input.substring(endPos);
        }
        throw new RuntimeException(("Illegal oneOf pattern: " + input));
    }

}
