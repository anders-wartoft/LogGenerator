package nu.sitia.loggenerator.filter.substituters;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoremSubstitute extends AbstractSubstitute {

    /** Regex for lorem */
    private static final String loremRegex = "\\{lorem:(?<length>\\d+):(?<wordlist>.*)/(?<delimiter>.)}";
    /** Pattern for lorem */
    private static final Pattern loremPattern = Pattern.compile(loremRegex);

    /**
     * Generate a number of words from a template. The template is:
     * {lorem:32:list of words to scramble / } will randomly pick a word from the
     * list, append the blank (last character after /) and repeat 32 times.
     * {lorem:4:a;b;c;d/;} will randomly pick a letter from the list (a b c or d)
     * then concatenate with ; (the delimiter), repeat 4 times and return the result.
     * N.B. The last inserted word/character will not have a delimiter appended.
     * @param input The string containing the variable specification
     * @return The input but with a list of words instead of the specification
     */
    public String substitute(String input) {
        int startPos = input.indexOf("{lorem:");
        if (startPos < 0) return input;

        int endPos = AbstractSubstitute.getExpressionEnd(input, startPos);
        String part = input.substring(startPos, endPos);
        // First, get the interval
        Matcher matcher = loremPattern.matcher(part);
        if (matcher.find()) {
            String length = matcher.group(1);
            String words = matcher.group(2);
            String delimiter = matcher.group(3);
            int len = Integer.parseInt(length);
            String [] wordList = words.split(delimiter);
            Random random = new Random();
            StringBuilder sb = new StringBuilder();
            for (int i=0; i<len; i++) {
                int pos = random.nextInt(wordList.length);
                sb.append(wordList[pos]);
                if (i < (len-1)) {
                    sb.append(delimiter);
                }
            }
            return input.substring(0, startPos) + sb + input.substring(endPos);
        }
        throw new RuntimeException(("Illegal lorem pattern: " + input));
    }

}
