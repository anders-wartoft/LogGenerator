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

import nu.sitia.loggenerator.templates.NoneTemplate;
import nu.sitia.loggenerator.templates.Template;
import nu.sitia.loggenerator.templates.TemplateFactory;
import nu.sitia.loggenerator.util.CommandLineParser;

import java.util.LinkedList;
import java.util.List;

public class FilterListFactory {

    /**
     * Get all filters suitable for this set of conditions
     * @param args The command line arguments to parse
     * @return A list of filters to apply to the events
     */
    public List<ProcessFilter> create(String[] args) {
        String gapRegex = CommandLineParser.getCommandLineArgument(args, "gd", "gap-detection", "Regex for gap detection");
        String removeGuards = CommandLineParser.getCommandLineArgument(args, "rg", "remove-guards", "Drop messages that is used for statistics before we send them?");
        String header = CommandLineParser.getCommandLineArgument(args, "he", "header", "String to add to the beginning of each entry. May contain variables.");
        String template = CommandLineParser.getCommandLineArgument(args, "t", "template", "Should the input be regarded as a template and variables resolved?");
        String regex = CommandLineParser.getCommandLineArgument(args, "r", "regex", "Regex to search for and replace with a value.");
        String value = CommandLineParser.getCommandLineArgument(args,"v", "value", "Value to replace the regex with.");

        List<ProcessFilter> filterList = new LinkedList<>();

        if (removeGuards != null && removeGuards.equalsIgnoreCase("true")) {
            filterList.add(new GuardFilter(args));
        }

        if (regex != null) {
            filterList.add(new RegexFilter(regex, value));
        }

        if (header != null) {
            filterList.add(new HeaderFilter(header));
        }

        Template template1 = new NoneTemplate();
        if (template != null) {
            template1 = TemplateFactory.getTemplate(template);
        }

        if (regex != null ||
                header!= null ||
                template1.isNone()) {
            // Add variable expansion, like {date: etc
            filterList.add(new SubstitutionFilter());
        }

        if (gapRegex != null) {
            filterList.add(new GapDetectionFilter(gapRegex));
        }

        return filterList;
    }
}
