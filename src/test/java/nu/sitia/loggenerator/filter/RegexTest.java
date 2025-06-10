package nu.sitia.loggenerator.filter;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;


/**
 * Unit test for simple App.
 */
public class RegexTest
    extends TestCase
{

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public RegexTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( RegexTest.class );
    }

    /**
     * Test regex.
     */
    public void testRegex() {
        String template = "2050-01-01T10:00:00.001001Z Some event here";
        String regex = "\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d.\\d{6}Z";
        String value = "{date:yyyy-MM-dd/sv:SE}";

        RegexFilter regexFilter = new RegexFilter(null);
        regexFilter.setParameter("--regex", regex);
        regexFilter.setParameter("--value", value);
        regexFilter.afterPropertiesSet();

        List<String> result = regexFilter.filter(List.of(template));

        assertNotSame(template, result.get(0));
    }

    /**
     * Test regex.
     */
    public void testRegexReplaceQuotes() {
        String template = "2050-01-01T10:00:00.001001Z \"Some\" event here";
        String expected = "2050-01-01T10:00:00.001001Z \\\"Some\\\" event here";
        String regex = "\"";
        String value = "\\\"";

        RegexFilter regexFilter = new RegexFilter(null);
        regexFilter.setParameter("--regex", regex);
        regexFilter.setParameter("--value", value);
        regexFilter.afterPropertiesSet();

        List<String> result = regexFilter.filter(List.of(template));

        assertEquals(expected, result.get(0));
    }

    /**
     * Test regex.
     */
    public void testRegexReplacement() {
        String template = "2050-01-01T10:00:00.001001Z \"Some\" event here";
        String regex = "^.*$";
        String value = "{\"message\":\"{all}\"}";
        String expected = "{\"message\":\"2050-01-01T10:00:00.001001Z \\\"Some\\\" event here\"}";

        // First, fix the quotes
        RegexFilter quoteFilter = new RegexFilter(null);
        quoteFilter.setParameter("--regex", "\"");
        quoteFilter.setParameter("--value", "\\\\\"");
        quoteFilter.afterPropertiesSet();
        List<String> quotedResult = quoteFilter.filter(List.of(template));

        RegexFilter regexFilter = new RegexFilter(null);
        regexFilter.setParameter("--regex", regex);
        regexFilter.setParameter("--value", value);
        regexFilter.afterPropertiesSet();

        List<String> result = regexFilter.filter(List.of(quotedResult.get(0)));

        assertEquals(expected, result.get(0));
    }

}
