package nu.sitia.loggenerator.filter;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import nu.sitia.loggenerator.util.Configuration;

import java.util.List;
import java.util.*;

/**
 * Unit test for simple App.
 */
public class RegexTest
    extends TestCase
{
    private final Map testData = new HashMap();
    private Configuration config = new Configuration();



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
        config.setRegex("\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d.\\d{6}Z");
        config.setValue("{date:yyyy-MM-dd/sv:SE}");
        RegexFilter regexFilter = new RegexFilter(config);

        List<String> result = regexFilter.filter(Arrays.asList(template));

        assertNotSame(template, result.get(0));
    }


}
