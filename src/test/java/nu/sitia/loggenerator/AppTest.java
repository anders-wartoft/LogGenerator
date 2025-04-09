package nu.sitia.loggenerator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Replaced by tests of the components
     */
    public void testApp()
    {
        assertTrue( true );
    }

    /**
     * Test file read and write to "Cmd" output
     */
    public void testItemProxyGapDetection()
    {
        String [] args = {
                "-i", "counter",
                "--string", "Test:",
                "--limit", "10",
                "-o", "cmd",
                "-f", "gap",
                "-r", ":(\\d+)$",
                "-o", "cmd",
                "-s", "true"
        };
        App.main(args);
    }

    /**
     * Test read Elastic (not in testcases)
     */
/**    public void testElastic()
    {
        String [] args = {
                "-p", "./src/test/data/elasticsearch-input.properties",
                "-eif", "_source"
        };
        App app = new App();
        app.main(args);
    }
*/
/**    public void testElastic()
    {
        String [] args = {
                "-i", "json-file",
                "-ifn", "./src/test/data/elasticsearch.json",
                "-o", "cmd",
                "-jfp", "hits.hits",
                "-jf", "_id"
        };
        App app = new App();
        app.main(args);
    }
 */
}
