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
    private static final String dateRegex = "\\{date:(?<datepattern>[yYmMHhsz+-dD:\\d'T. ]+)(/(?<locale>[^}]+))?}";
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
        throw new RuntimeException(("Illegal date pattern: " + input));
    }
}
