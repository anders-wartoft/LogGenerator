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
import nu.sitia.loggenerator.templates.NoneTemplate;
import nu.sitia.loggenerator.templates.Template;
import nu.sitia.loggenerator.templates.TemplateFactory;

import java.util.LinkedList;
import java.util.List;

public class FilterListFactory {

    /**
     * Get all filters suitable for this set of conditions
     * @param config The command line arguments to parse
     * @return A list of filters to apply to the events
     */
    public List<ProcessFilter> create(Configuration config) {
        String gapRegex = config.getValue("-gd");
        String removeGuards = config.getValue("-rg");
        String header = config.getValue("-he");
        String template = config.getValue("-t");
        String regex = config.getValue("-r");
        String value = config.getValue("-v");
        String doubleDetection = config.getValue("-dd");
        String jsonFilter = config.getValue("-jf");
        String jsonReport = config.getValue("-gdjr");
        String select = config.getValue("-se");
        String drop = config.getValue("-df");
        boolean df = "true".equalsIgnoreCase(drop);
        boolean dd = "true".equalsIgnoreCase(doubleDetection);

        List<ProcessFilter> filterList = new LinkedList<>();

        if (removeGuards != null && removeGuards.equalsIgnoreCase("true")) {
            filterList.add(new GuardFilter(config));
        }

        if (df) {
            filterList.add(new DropFilter(regex));
        }

        if (regex != null) {
            filterList.add(new RegexFilter(regex, value));
        }

        Template template1 = new NoneTemplate();
        if (template != null) {
            template1 = TemplateFactory.getTemplate(template);
        }

        if (jsonFilter != null) {
            filterList.add(new JsonFilter(jsonFilter));
        }

        if (regex != null ||
                header!= null ||
                !template1.isNone()) {
            // Add variable expansion, like {date: etc
            filterList.add(new SubstitutionFilter(config));
        }

        if (gapRegex != null) {
            filterList.add(new GapDetectionFilter(gapRegex, dd,
                    "true".equalsIgnoreCase(jsonReport)));
        }
        if (select != null) {
            filterList.add(new SelectFilter(select));
        }

        if (header != null) {
            filterList.add(new HeaderFilter(header));
        }

        return filterList;
    }
}
