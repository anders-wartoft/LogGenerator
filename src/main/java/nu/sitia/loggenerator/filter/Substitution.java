package nu.sitia.loggenerator.filter;

import nu.sitia.loggenerator.util.Ipv4;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Substitution {
    /** Regex for lorem */
    private static final String loremRegex = "\\{lorem:(?<length>\\d+):(?<wordlist>.*)/(?<delimiter>.)}";
    /** Pattern for lorem */
    private static final Pattern loremPattern = Pattern.compile(loremRegex);

    /** Regex for counter */
    private static final String counterRegex = "\\{counter:(?<name>[a-zA-Z0-9\\-_]+):(?<startvalue>\\d+)}";
    /** Pattern for counter */
    private static final Pattern counterPattern = Pattern.compile(counterRegex);
    /** The actual counters */
    private static final Map<String, Integer> counters = new HashMap<>();

    /** Regex for random string */
    private static final String stringRegex = "\\{string:(?<characters>[^/]+)/(?<numberof>\\d+)}";
    /** Pattern for random string */
    private static final Pattern stringPattern = Pattern.compile(stringRegex);

    /** Regex for random number */
    private static final String randomRegex = "\\{random:(?<from>\\d+)-(?<to>\\d+)}";

    /** Cached pattern for random */
    private static final Pattern randomPattern = Pattern.compile(randomRegex);

    /** Regex for oneOf */
    private static final String oneOfRegex = "\\{oneOf:(?<options>.*)(/(?<delimiter>.))?}";

    /** Cached pattern for getting oneOf */
    private static final Pattern oneOfPattern = Pattern.compile(oneOfRegex);

    /** Regex for a date pattern */
    private static final String dateRegex = "\\{date:(?<datepattern>[yYmMHhsz+-dD:\\d'T. ]+)(/(?<locale>[^}]+))?}";
    /** Cached pattern for getting date format string */
    private static final Pattern datePattern = Pattern.compile(dateRegex);

    /** Ipv4 variable regex */
    private static final String ipv4Regex = "\\{ipv4:(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})/(\\d{1,2})}";

    /** Cached pattern for ipv4 */
    private static final Pattern ipv4Pattern = Pattern.compile(ipv4Regex);

    /** The date to use in {date:...} variables. Using variable to make unit testing easier */
    private Date date;

    /**
     * Freemarker-like substitution.
     * All occurrences of {key} in the template will be replaced with value from the
     * translations map.
     * @param template The template containing {key} values
     * @param translations The key-value pairs
     * @return The template with all {key} substituted with value
     */
    public static String substitute(String template, Map<String, String> translations, Date date) {
        String result = template;
        while (result.indexOf("{lorem:") >= 0) {
            // Lorem ipsum requested. Replace with an number of words
            result = calculateLorem(result);
        }
        while (result.indexOf("{oneOf:") >= 0) {
            // one of several choices. Several instances might have different chosen entry
            result = calculateOneOf(result);
        }
        while (result.indexOf("{random:") >= 0) {
            // Random number
            result = calculateRandom(result);
        }
        if (result.indexOf("{date:") >= 0) {
            // Date requested. All instances will use the same value
            result = calculateDate(result, date);
        }
        while (result.indexOf("{ipv4:") >= 0) {
            // ipv4 requested. Replace with a random IP in the specified range
            result = calculateIp(result);
        }
        while (result.indexOf("{string:") >= 0) {
            // string requested. Replace with a random string
            result = calculateString(result);
        }
        while (result.indexOf("{counter:") >= 0) {
            // counter requested. Replace with an enumerable field
            result = calculateCounter(result);
        }
        for (Iterator<String> iter = translations.keySet().iterator(); iter.hasNext(); ) {
            String key = iter.next();
            String value = translations.get(key);
            result = result.replaceAll("\\{" + key + "\\}", value);
        }
        return result;
    }

    /**
     * Extract a date format string from the input and return now() with
     * that format.
     * Protected instead of private to be able to test easier
     * @param input The String with a {date:pattern} part
     * @param time The time to use in the formatting
     * @return input with the current date/time instead of the pattern {date:pattern}
     */
    protected static String calculateDate(String input, Date time) {
        // First, get the date format string
        Matcher matcher = datePattern.matcher(input);
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
            String formattedDateString = formatter.format(time);
            String result = input.replaceAll(dateRegex, formattedDateString);
            return result;
        }
        throw new RuntimeException(("Illegal date pattern: " + input));
    }

    /**
     * An IP variable is in the form:
     * {ipv4:192.168.1.1/24}
     * A random ip in that range will be inserted instead of the variable specification.
     * @param input The string containing the ip variable specification
     * @return The input but with an actual ip number instead of the specification
     */
    protected static String calculateIp(String input) {
        // First, get the ip subnet and mask string
        Matcher matcher = ipv4Pattern.matcher(input);
        if (matcher.find()) {
            String ipString = matcher.group(1);
            String cidr = matcher.group(2);
            String result = Ipv4.longToIpv4(Ipv4.getRandomIpv4(ipString, cidr));
            return input.replaceFirst(ipv4Regex, result);
        }
        throw new RuntimeException(("Illegal ipv4 pattern: " + input));
    }

    /**
     * Get one of several options.
     * {oneOf:a,b,c,d} will randomly pick a b c or d.
     * If , is needed in the options, use the following structure:
     * {oneOf:a;b;c;d/;}
     * @param input The string containing the ip variable specification
     * @return The input but with one of the choices instead of the specification
     */
    protected static String calculateOneOf(String input) {
        // First, get the choices
        Matcher matcher = oneOfPattern.matcher(input);
        if (matcher.find()) {
            String delimiter = ",";
            String choicesString = matcher.group(1);
            if (matcher.groupCount() > 1) {
                if (matcher.group(2) != null) {
                    delimiter = matcher.group(2);
                }
            }
            String [] choices = choicesString.split(delimiter);
            int nr = new Random().nextInt(choices.length);
            String selected = choices[nr];
            return input.replaceFirst(oneOfRegex, selected);
        }
        throw new RuntimeException(("Illegal oneOf pattern: " + input));
    }

    /**
     * Get a random number in a specified interval.
     * {random:1-6} will randomly pick 1, 2, 3, 4, 5 or 6.
     * @param input The string containing the random variable specification
     * @return The input but with one of the random number instead of the specification
     */
    protected static String calculateRandom(String input) {
        // First, get the interval
        Matcher matcher = randomPattern.matcher(input);
        if (matcher.find()) {
            String from = matcher.group(1);
            String to = matcher.group(2);
            int low = Integer.valueOf(from);
            int high = Integer.valueOf(to);
            int nr = new Random().nextInt(high-low) + low;
            return input.replaceFirst(randomRegex, String.valueOf(nr));
        }
        throw new RuntimeException(("Illegal random pattern: " + input));
    }

    /**
     * A random string with specified possible characters and length of string.
     * {string:a-zA-Z0-9/64}
     * @param input The string containing the ip variable specification
     * @return The input but with a random string instead of the specification
     */
    protected static String calculateString(String input) {
        // First, get the character values and length
        Matcher matcher = stringPattern.matcher(input);
        if (matcher.find()) {
            String characterString = matcher.group(1);
            String lengthString = matcher.group(2);
            List<Character> charList = new ArrayList<>();
            // last character
            char char_1 = 0;
            // second last character
            char char_2 = 0;
            // Iterate over the specification, like a-zA-Z0-9.;,
            int loopNr = 0;
            for (char c: characterString.toCharArray()) {
                if (c == '-' && char_1 == '\\') {
                    // The last char was a \ and this is -. That means the user
                    // wanted the - character. Remove the \ from the list
                    charList.remove(charList.size()-1);
                    // and add the -
                    charList.add('-');
                }
                if (c == '-' && char_1 == '-') {
                    // The last char was a - and this is -. Illegal
                    throw new RuntimeException("Illegal character string in expression. Two - chars are not allowed after each other: " + input);
                }

                // We can only have a sequence if loopNr is at least 1
                if (char_1 == '-' && loopNr >=2 && char_2 != '\\') {
                    // sequence, like a-z. The a is already added
                    // Remove the '-'
                    charList.remove(charList.size()-1);
                    // add the rest of the characters but the last, c
                    // that will be added later on
                    for (char x = (char)(char_2 + 1); x < c; x++) {
                        charList.add(x);
                    }
                }

                // Add the c character to the list
                charList.add(c);
                // update the lastlast char
                char_2 = char_1;
                // and update last char
                char_1 = c;
                // also, update the loopNr
                loopNr++;
            } // for

            int length = Integer.valueOf(lengthString);
            // Now we have a list of characters and a length
            // Create the string
            StringBuilder sb = new StringBuilder();
            Random random = new Random();
            Character [] charArray = new Character[charList.size()];
            charArray = charList.toArray(charArray);
            for (int i=0; i<length; i++) {
                // Generate a random character
                int pos = random.nextInt(charArray.length);
                sb.append(charArray[pos]);
            }
            return sb.toString();
        } // if match
        throw new RuntimeException(("Illegal string pattern: " + input));
    }

    /**
     * Replace the specification with a number. For each
     * invocation with the same name, use the next number for that name
     * {counter:myCounter:6} will be substituted for 6 on the first
     * invocation, 7 on the next and so on.
     * @param input The string containing the variable specification
     * @return The input but with one of the counter numbers instead of the specification
     */
    protected static String calculateCounter(String input) {
        // First, get the interval
        Matcher matcher = counterPattern.matcher(input);
        if (matcher.find()) {
            String name = matcher.group(1);
            String startValue = matcher.group(2);
            Integer value = Integer.valueOf(startValue);
            // Check if we have had this before
            if (counters.containsKey(name)) {
                value = counters.get(name);
            }
            String result =  input.replaceFirst(counterRegex, String.valueOf(value));
            value++;
            counters.put(name, value);
            return result;
        }
        throw new RuntimeException(("Illegal counter pattern: " + input));
    }

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
    protected static String calculateLorem(String input) {
        // First, get the interval
        Matcher matcher = loremPattern.matcher(input);
        if (matcher.find()) {
            String length = matcher.group(1);
            String words = matcher.group(2);
            String delimiter = matcher.group(3);
            int len = Integer.valueOf(length);
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
            return input.replaceFirst(loremRegex, sb.toString());
        }
        throw new RuntimeException(("Illegal lorem pattern: " + input));
    }

}
