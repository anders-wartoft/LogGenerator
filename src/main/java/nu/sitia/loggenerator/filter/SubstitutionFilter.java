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

import nu.sitia.loggenerator.Configuration;
import nu.sitia.loggenerator.filter.substituters.Substitution;

import java.util.*;

public class SubstitutionFilter implements ProcessFilter {

    /** Other variables we want to change. Name, Value */
    private final Map<String, String> variableMap;

    /** Cached list of substitute */
    private final Substitution substitution = new Substitution();

    /**
     * Create a filter and set all parameters
     */
    public SubstitutionFilter(Configuration config) {
        variableMap = config.getVariableMap();
    }

    /**
     * Change all variables to values
     * @param toFilter The data to filter
     * @return The value of the variable
     */
    @Override
    public List<String> filter(List<String> toFilter) {
        List<String> filtered = new ArrayList<>();
        toFilter.forEach(s -> filtered.add(substitution.substitute(s, variableMap, new Date())));
        return filtered;
    }

    /**
     * Unit test code
     * @return The internal variable map
     */
    protected Map<String, String> getVariableMap() {
        return Collections.unmodifiableMap(this.variableMap);
    }

    /**
     * Print the configuration
     * @return A printable string of the current configuration
     */
    @Override
    public String toString() {
        return "SubstitutionFilter";
    }
}
