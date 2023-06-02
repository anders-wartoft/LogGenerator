package nu.sitia.loggenerator.filter.substituters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Unit test for simple App.
 */
public class ProbSubstituteTest
    extends TestCase {

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( ProbSubstituteTest.class );
    }

    /**
     * Test oneOf
     */
    public void testProb() {
        String template = "test foo {prob:a,b,c,d} bar";
        String actual = new ProbSubstitute().substitute(template);
        assertEquals(14, actual.length());
    }

    public void testProb2() {
        String template = "test {prob:a/0,b/100,c/0} bar";
        String actual = new ProbSubstitute().substitute(template);
        assertEquals("test b bar", actual);
    }

    public void testProb3() {
        String template = "test {prob:a/0,b/0,c} bar";
        String actual = new ProbSubstitute().substitute(template);
        assertEquals("test c bar", actual);
    }

}
