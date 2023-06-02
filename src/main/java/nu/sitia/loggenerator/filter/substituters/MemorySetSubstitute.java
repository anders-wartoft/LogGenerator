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

public class MemorySetSubstitute extends AbstractSubstitute {
    /** If no name is given for a memory */
    public static final String DEFAULT_NAME = "defaultName";
    /** Regex for memory */
    private static final String msRegex = "\\{ms(:(?<name>[a-zA-Z0-9\\-_]+))?/(?<value>.*)}";
    /** Pattern for memory */
    private static final Pattern msPattern = Pattern.compile(msRegex);
    /** The actual memories */
    private static Map<String, String> cache = new HashMap<>();


    /**
     * Remember the contents of the value field for later.
     * The contents (value) can be recalled with {mr}.
     * Several named memories can be used by utilizing the name field:
     * {ms:name/expression}
     * From the above construction, the expression can be retrieved later on
     * with
     * {mr:name}
     * Expression can contain variables, like:
     * {ms/{oneOf:a,b,c,d}}
     * will yield one of a, b, c or d
     * and
     * {mr}
     * will evaluate to exactly the same value
     * @param input The string containing the variable specification
     * @return The expression from the input (and saving the value for later on)
     */
    public String substitute(String input) {
        int startPos = input.indexOf("{ms");
        if (startPos < 0) return input;

        int endPos = AbstractSubstitute.getExpressionEnd(input, startPos);
        String part = input.substring(startPos, endPos);
        // First, get the name
        Matcher matcher = msPattern.matcher(part);
        if (matcher.find()) {
            String name = matcher.group("name");
            if (name == null) {
                name = DEFAULT_NAME;
            }
            String value = matcher.group("value");
            // Special case here. We need to evaluate the contents of the value field
            Substitution substitution = new Substitution();
            String newValue = substitution.substitute(value, new HashMap<>(), null);
            String result = input.substring(0, startPos) + newValue + input.substring(endPos);
            // and save the value for later
            cache.put(name, newValue);
            return result;
        }
        throw new RuntimeException(("Illegal counter pattern: " + input));
    }

    /**
     * Reset the internal state:
     */
    public void clear() {
        cache = new HashMap<>();
    }


    /**
     * Utility method so other classes can read the value of
     * all saved data, by name
     * @param key The name of the memory
     * @return The String value of the memory or null if not found
     */
    public static String getCounterValue(String key) {
        if (null == key) {
            return cache.get(DEFAULT_NAME);
        }
        return cache.get(key);
    }

}
