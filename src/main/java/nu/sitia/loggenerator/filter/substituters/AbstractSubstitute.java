package nu.sitia.loggenerator.filter.substituters;

public abstract class AbstractSubstitute implements Substitute {

    /**
     * This is a hack since Java doesn't have recursive regexes.
     * In a string like "test {first:a,b,{second:d,e}}", the call to
     * searchMatchingBrace("test {first:a,b,{second:d,e}}", 5) will result in
     * 28, pointing to the second }
     * @param input The string to examine
     * @param startPos Where the starting brace is
     */
    protected static int getExpressionEnd(String input, int startPos) {
        int counter = 0;
        for (int i=startPos; i<input.length(); i++) {
            if (input.charAt(i) == '{') {
                counter++;
            }
            if (input.charAt(i) == '}') {
                counter--;
            }
            if (counter == 0) {
                return i+1;
            }
        }
        throw new RuntimeException("Error parsing the expression " + input.substring(startPos));
    }
}
