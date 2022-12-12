package nu.sitia.loggenerator.filter.substituters;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RandomNumberSubstitute extends AbstractSubstitute {
    /** Regex for random number */
    private static final String randomRegex = "\\{random:(?<from>\\d+)-(?<to>\\d+)}";

    /** Cached pattern for random */
    private static final Pattern randomPattern = Pattern.compile(randomRegex);


    /**
     * Get a random number in a specified interval.
     * {random:1-6} will randomly pick 1, 2, 3, 4, 5 or 6.
     * @param input The string containing the random variable specification
     * @return The input but with one of the random number instead of the specification
     */
    public String substitute(String input) {
        int startPos = input.indexOf("{random:");
        if (startPos < 0) return input;

        int endPos = AbstractSubstitute.getExpressionEnd(input, startPos);
        String part = input.substring(startPos, endPos);
        // First, get the interval
        Matcher matcher = randomPattern.matcher(part);
        if (matcher.find()) {
            String from = matcher.group(1);
            String to = matcher.group(2);
            int low = Integer.parseInt(from);
            int high = Integer.parseInt(to);
            int nr = new Random().nextInt(high-low) + low;
            return input.substring(0, startPos) + nr + input.substring(endPos);
        }
        throw new RuntimeException(("Illegal random pattern: " + input));
    }

}
