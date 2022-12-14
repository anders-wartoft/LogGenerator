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

public class StringSubstitute extends AbstractSubstitute {

    /** Regex for random string */
    private static final String stringRegex = "\\{string:(?<characters>[^/]+)/(?<numberof>\\d+)}";
    /** Pattern for random string */
    private static final Pattern stringPattern = Pattern.compile(stringRegex);


    /**
     * A random string with specified possible characters and length of string.
     * {string:a-zA-Z0-9/64}
     * @param input The string containing the ip variable specification
     * @return The input but with a random string instead of the specification
     */
    public String substitute(String input) {
        int startPos = input.indexOf("{string:");
        if (startPos < 0) return input;

        int endPos = AbstractSubstitute.getExpressionEnd(input, startPos);
        String part = input.substring(startPos, endPos);
        // First, get the character values and length
        Matcher matcher = stringPattern.matcher(part);
        if (matcher.find()) {
            String characterString = matcher.group(1);
            String lengthString = matcher.group(2);
            List<Character> charList = new ArrayList<>();
            // last character
            char char_1 = 0;
            // second last character
            char char_2 = 0;
            // Iterate over the specification, like a-zA-Z0-9.;,
            int loopNr = 0;
            for (char c: characterString.toCharArray()) {
                if (c == '-' && char_1 == '\\') {
                    // The last char was a \ and this is -. That means the user
                    // wanted the - character. Remove the \ from the list
                    charList.remove(charList.size()-1);
                    // and add the -
                    charList.add('-');
                }
                if (c == '-' && char_1 == '-') {
                    // The last char was a '-' and this is also a '-'. Illegal
                    throw new RuntimeException("Illegal character string in expression. Two - chars are not allowed after each other: " + input);
                }

                // We can only have a sequence if loopNr is at least 1
                if (char_1 == '-' && loopNr >=2 && char_2 != '\\') {
                    // sequence, like a-z. The 'a' is already added
                    // Remove the '-'
                    charList.remove(charList.size()-1);
                    // add the rest of the characters but the last, c
                    // that will be added later on
                    for (char x = (char)(char_2 + 1); x < c; x++) {
                        charList.add(x);
                    }
                }

                // Add the c character to the list
                charList.add(c);
                // update the second last char
                char_2 = char_1;
                // and update last char
                char_1 = c;
                // also, update the loopNr
                loopNr++;
            } // for

            int length = Integer.parseInt(lengthString);
            // Now we have a list of characters and a length
            // Create the string
            StringBuilder sb = new StringBuilder();
            Random random = new Random();
            Character [] charArray = new Character[charList.size()];
            charArray = charList.toArray(charArray);
            for (int i=0; i<length; i++) {
                // Generate a random character
                int pos = random.nextInt(charArray.length);
                sb.append(charArray[pos]);
            }
            return input.substring(0, startPos) + sb + input.substring(endPos);
        } // if match
        throw new RuntimeException(("Illegal string pattern: " + input));
    }


}
