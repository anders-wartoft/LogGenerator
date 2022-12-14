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
