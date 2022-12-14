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

package nu.sitia.loggenerator.filter;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexFilter implements ProcessFilter {

    /** What to write instead */
    private final String value;

    /** Cached regex pattern */
    private final Pattern pattern;

    /** for toString() */
    private final String regex;

    /**
     * Create a RegexFilter and set all parameters
     * @param regex The value to search for
     * @param value The value to use instead
     */
    public RegexFilter(String regex, String value) {
        this.value = value;
        this.regex = regex;
        // What to look for
        if (null == regex) {
            throw new RuntimeException("regex is null");
        }
        // What to replace it with
        if (null == value) {
            throw new RuntimeException("value is null");
        }
        pattern = Pattern.compile(regex);
    }

    /**
     * Filter one string
     *
     * @param toFilter The string to change
     * @return toFilter with a header added before the string.
     */
    private String filter(String toFilter) {
        Matcher matcher = pattern.matcher(toFilter);
        if (matcher.find()) {
            return toFilter.replaceAll(matcher.group(), value);
        }
        return toFilter;
    }

    @Override
    public List<String> filter(List<String> toFilter) {
        List<String> filtered = new ArrayList<>();
        toFilter.forEach(s ->
                filtered.add(filter(s)));

        return filtered;
    }

    /**
     * The current configuration
     * @return A printout of the current configuration
     */
    @Override
    public String toString() {
        return "RegexFilter" + System.lineSeparator() + regex + " - " + value + System.lineSeparator();
    }
}
