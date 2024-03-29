package nu.sitia.loggenerator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Unit test for simple App.
 */
public class ConfigurationTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ConfigurationTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( ConfigurationTest.class );
    }

    /**
     * Replaced by tests of the components
     */
    public void testConfiguration()
    {
        String [] args = {
            "-i", "file",
            "-ifn", "testname"
        };

        Configuration config = new Configuration(args);

        assertEquals( "file", config.getValue("-i" ));
        assertEquals( "testname", config.getValue("-ifn" ));
    }





}
