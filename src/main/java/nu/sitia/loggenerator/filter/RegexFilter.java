package nu.sitia.loggenerator.filter;

import nu.sitia.loggenerator.util.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexFilter implements ProcessFilter {

    /** What to write instead */
    private String value;

    /** Cached regex pattern */
    private final Pattern pattern;

    /**
     * Create a RegexFilter and set all parameters
     * @param config The configuration object to get parameters from
     */
    public RegexFilter(Configuration config) {
        /** What to look for */
        String regex = config.getRegex();
        if (null == regex) {
            throw new RuntimeException("regex is null");
        }
        value = config.getValue();
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
            String result = toFilter.replaceAll(matcher.group(), value);
            return result;
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
}
