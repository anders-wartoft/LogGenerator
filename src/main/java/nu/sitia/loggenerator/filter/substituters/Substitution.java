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


public class Substitution {

    /**
     * The substitutes to iterate over
     */
    private final List<Substitute> items = new LinkedList<>();

    /**
     * Cache the date object to update the date() for each invocation
     */
    private final DateSubstitute dateSubstitute;

    /**
     * Create the list of substitutes
     */
    public Substitution() {
        dateSubstitute = new DateSubstitute();
        items.add(dateSubstitute);
        items.add(new CounterSubstitute());
        items.add(new Ipv4Substitute());
        items.add(new LoremSubstitute());
        items.add(new OneOfSubstitute());
        items.add(new PrioritySubstitute());
        items.add(new RandomNumberSubstitute());
        items.add(new StringSubstitute());
    }

    /**
     * Freemarker-like substitution.
     * All occurrences of {key} in the template will be replaced with value from the
     * translations map.
     *
     * @param template     The template containing {key} values
     * @param translations The key-value pairs
     * @return The template with all {key} substituted with value
     */
    public String substitute(String template, Map<String, String> translations, Date date) {
        // Update the timestamp
        dateSubstitute.setDate(date);

        String result = template;
        // Start with built-in values, like {syslog-header}, {ip}:
        for (String key : translations.keySet()) {
            String value = translations.get(key);
            result = result.replaceAll("\\{" + key + "}", value);
        }

        // Now, do the list of {date: ...
        String lastResult;
        do {
            lastResult = result;
            for (Substitute item : items) {
                result = item.substitute(result);
            }
        } while (!lastResult.equals(result));
        return result;
    }
}
