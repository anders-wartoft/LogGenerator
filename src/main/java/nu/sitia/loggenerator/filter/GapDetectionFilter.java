package nu.sitia.loggenerator.filter;

import nu.sitia.loggenerator.util.Configuration;
import nu.sitia.loggenerator.util.gapdetector.GapDetector;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GapDetectionFilter implements ProcessFilter {
    /** The detector */
    private final GapDetector detector = new GapDetector();

    /** The pattern to get the next number from the log */
    private final Pattern pattern;
    /**
     * Create a guardFilter and set all parameters
     * @param config The configuration object to get parameters from
     */
    public GapDetectionFilter(Configuration config) {
        String regex = config.getGapRegex();
        if (null == regex) throw new RuntimeException("No gap regex detected");
        pattern = Pattern.compile(regex);
        // When we are shutting down the transmission we'd like to be able to
        // print the gaps, so add the detector to the config
        config.setDetector(detector);
    }

    /**
     * The regex should be like "text(\\d+)other text" so the capture group is the next number.
     * @param toFilter The data to filter
     * @return The data without transmission and file guards.
     */
    @Override
    public List<String> filter(List<String> toFilter) {
        toFilter.forEach(s -> {
            Matcher matcher = pattern.matcher(s);
            if (matcher.find()) {
                String numberString = matcher.group(1);
                long number = Long.parseLong(numberString);
                detector.check(number);
            }
        });

        return toFilter;
    }
}
