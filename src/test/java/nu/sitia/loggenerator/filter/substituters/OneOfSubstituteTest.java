package nu.sitia.loggenerator.filter.substituters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Unit test for simple App.
 */
public class OneOfSubstituteTest
    extends TestCase {

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( OneOfSubstituteTest.class );
    }

    /**
     * Test oneOf
     */
    public void testOneOf() {
        String template = "test foo {oneOf:a,b,c,d} bar";
        String actual = new OneOfSubstitute().substitute(template);
        assertEquals(14, actual.length());
    }

    /**
     * Test oneOf recursively
     */
    public void testOneOfRecurse() {
        String template = "test foo {oneOf:a,b,{oneOf:1,2,3,4},d} bar";
        String actual = new OneOfSubstitute().substitute(template);
        assertEquals(14, actual.length());
    }

}
