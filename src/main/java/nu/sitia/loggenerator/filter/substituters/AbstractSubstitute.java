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

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSubstitute implements Substitute {

    /**
     * This is a hack since Java doesn't have recursive regexes.
     * In a string like "test {first:a,b,{second:d,e}}", the call to
     * searchMatchingBrace("test {first:a,b,{second:d,e}}", 5) will result in
     * 28, pointing to the second }
     * @param input The string to examine
     * @param startPos Where the starting brace is
     */
    protected static int getExpressionEnd(String input, int startPos) {
        int counter = 0;
        for (int i=startPos; i<input.length(); i++) {
            if (input.charAt(i) == '{') {
                counter++;
            }
            if (input.charAt(i) == '}') {
                counter--;
            }
            if (counter == 0) {
                return i+1;
            }
        }
        throw new RuntimeException("Error parsing the expression " + input.substring(startPos));
    }

    /**
     * Split a string by a character. If a field starts with a { tnen
     * the characters within the field should not be used fof splitting.
     * The field is terminated by a matching }
     * @param input The input to split
     * @param delimiter The delimiter to use
     * @return An array of String from the input
     */
    protected String [] split(String input, String delimiter) {
        List<String> result = new ArrayList<>();
        int index;
        do {
            int pos = 0;
            if (input.indexOf("{") == 0) {
                pos = 1;
                int found = 1;
                // spool forward until we find a matching }
                do {
                    char c = input.charAt(pos);
                    if (c == '}') {
                        found --;
                    }
                    if (c == '{') {
                        found ++;
                    }
                    pos ++;
                    if (pos > input.length()) {
                        throw new RuntimeException("Error in input for substituter: " + input + " malformed {}");
                    }
                } while (found > 0);
            }
            index = input.indexOf(delimiter, pos);
            if (index >= 0) {
                String temp = input.substring(0, index);
                result.add(temp);
                input = input.substring(index + delimiter.length());
            }
        } while (index >= 0);
        result.add(input);
        return result.toArray(new String[result.size()]);
    }
}
