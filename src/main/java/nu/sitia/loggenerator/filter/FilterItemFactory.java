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

package nu.sitia.loggenerator.filter;

import nu.sitia.loggenerator.Configuration;

public class FilterItemFactory {

    /**
     * Get all filters suitable for this set of conditions
     * @param config The command line arguments to parse
     * @return A list of filters to apply to the events
     */
    public static ProcessFilter create(Configuration config, String name) {
        if (name == null) {
            throw new RuntimeException("Null name. Can't create process filter");
        }

        if (name.equalsIgnoreCase("drop")) return new DropFilter(config);
        if (name.equalsIgnoreCase("gap")) return new GapDetectionFilter(config);
        if (name.equalsIgnoreCase("guard")) return new GuardFilter(config);
        if (name.equalsIgnoreCase("header")) return new HeaderFilter(config);
        if (name.equalsIgnoreCase("json")) return new JsonFilter(config);
        if (name.equalsIgnoreCase("regex")) return new RegexFilter(config);
        if (name.equalsIgnoreCase("select")) return new SelectFilter(config);
        if (name.equalsIgnoreCase("substitute")) return new SubstitutionFilter(config);

        throw new RuntimeException("Illegal filter name: " + name);
    }
}
