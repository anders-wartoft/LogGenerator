package nu.sitia.loggenerator.filter.substituters;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CounterSubstitute extends AbstractSubstitute {
    /** Regex for counter */
    private static final String counterRegex = "\\{counter:(?<name>[a-zA-Z0-9\\-_]+):(?<startvalue>\\d+)}";
    /** Pattern for counter */
    private static final Pattern counterPattern = Pattern.compile(counterRegex);
    /** The actual counters */
    private static final Map<String, Integer> counters = new HashMap<>();


    /**
     * Replace the specification with a number. For each
     * invocation with the same name, use the next number for that name
     * {counter:myCounter:6} will be substituted for 6 on the first
     * invocation, 7 on the next and so on.
     * @param input The string containing the variable specification
     * @return The input but with one of the counter numbers instead of the specification
     */
    public String substitute(String input) {
        int startPos = input.indexOf("{counter:");
        if (startPos < 0) return input;

        int endPos = AbstractSubstitute.getExpressionEnd(input, startPos);
        String part = input.substring(startPos, endPos);
        // First, get the interval
        Matcher matcher = counterPattern.matcher(part);
        if (matcher.find()) {
            String name = matcher.group(1);
            String startValue = matcher.group(2);
            Integer value = Integer.valueOf(startValue);
            // Check if we have had this before
            if (counters.containsKey(name)) {
                value = counters.get(name);
            }
            String result = input.substring(0, startPos) + value + input.substring(endPos);
            value++;
            counters.put(name, value);
            return result;
        }
        throw new RuntimeException(("Illegal counter pattern: " + input));
    }
}
