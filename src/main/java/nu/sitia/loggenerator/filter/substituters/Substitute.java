package nu.sitia.loggenerator.filter.substituters;

public interface Substitute {
    /**
     * All Substitutes can substitute content in a String
     * @param input The input string
     * @return The input string with all, or parts, substituted for other content
     */
     String substitute(String input);
}
