package nu.sitia.loggenerator.filter;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;
import nu.sitia.loggenerator.Configuration;

/**
 * Unit test for simple App.
 */
public class GuardFilterTest
    extends TestCase
{

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public GuardFilterTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( GuardFilterTest.class );
    }

    /**
     * Test filter.
     */
    public void testFilter() {
        String template = Configuration.BEGIN_TRANSACTION_TEXT;

        GuardFilter guardFilter = new GuardFilter(null);
        guardFilter.afterPropertiesSet();

        List<String> result = guardFilter.filter(List.of(template));
        
        assertTrue(result.size() == 0);
    }
}
