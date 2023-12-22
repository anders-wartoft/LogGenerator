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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateSubstitute extends AbstractSubstitute {
    /** The date to use in substitutions */
    private Date date;

    /** Regex for a date pattern */
    private static final String dateRegex = "\\{date:(?<datepattern>[yYmMHhsz+-dD:\\d'T. ]+|epoch|epoch16)(/(?<locale>[^}]+))?}";
    /** Cached pattern for getting date format string */
    private static final Pattern datePattern = Pattern.compile(dateRegex);

    public void setDate(Date date) {
        this.date = date;
    }

    /**
     * Extract a date format string from the input and return now() with
     * that format.
     * Protected instead of private to be able to test easier
     * @param input The String with a {date:pattern} part
     * @return input with the current date/time instead of the pattern {date:pattern}
     */
    public String substitute(String input) {
        int startPos = input.indexOf("{date:");
        if (startPos < 0) return input;

        int endPos = getExpressionEnd(input, startPos);
        String part = input.substring(startPos, endPos);
        if ("{date:epoch16}".equalsIgnoreCase(part)) {
            String formattedDateString = String.format("%013d", date.getTime()) + "000";
            return input.substring(0, startPos) + formattedDateString + input.substring(endPos);
        } else if ("{date:epoch}".equalsIgnoreCase(part)) {
            String formattedDateString = String.format("%013d", date.getTime());
            return input.substring(0, startPos) + formattedDateString + input.substring(endPos);
        } else {
            // First, get the date format string
            Matcher matcher = datePattern.matcher(part);
            if (matcher.find()) {
                String datePattern = matcher.group(1);
                String locale = "en:US";
                // the format might add /locale-name
                if (matcher.groupCount() > 2) {
                    if (matcher.group(2) != null) {
                        locale = matcher.group(2);
                    }
                }
                SimpleDateFormat formatter = new SimpleDateFormat(datePattern, Locale.forLanguageTag(locale));
                String formattedDateString = formatter.format(date);
                return input.substring(0, startPos) + formattedDateString + input.substring(endPos);
            }
            throw new RuntimeException(("Illegal date pattern: " + input));        }
    }
}
