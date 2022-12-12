package nu.sitia.loggenerator.filter.substituters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class RandomNumberSubstituteTest
    extends TestCase {

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( RandomNumberSubstituteTest.class );
    }



    /**
     * Test random
     */
    public void testRandom() {
        String template = "{random:1-6}";
        String actual = new RandomNumberSubstitute().substitute(template);
        int nr = Integer.parseInt(actual);
        assertTrue(nr >= 1 && nr <= 6);
    }
}
