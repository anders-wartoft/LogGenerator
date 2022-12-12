package nu.sitia.loggenerator.filter.substituters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Unit test for simple App.
 */
public class StringSubstituteTest
    extends TestCase {

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( StringSubstituteTest.class );
    }



    /**
     * Test string generation
     */
    public void testString() {
        String template = "{string:a/10}";
        String expected = "aaaaaaaaaa";
        String actual = new StringSubstitute().substitute(template);
        assertEquals(expected, actual);
    }


    /**
     * Test string generation
     */
    public void testString2() {
        String template = "{string:a-z/10}";
        String actual = new StringSubstitute().substitute(template);
        assertNotSame(template, actual);
        assertEquals(10, actual.length());
    }

    /**
     * Test string generation
     */
    public void testString3() {
        String template = "{string:a-z0-9\\-/100}";
        String actual = new StringSubstitute().substitute(template);
        assertNotSame(template, actual);
        assertEquals(100, actual.length());
    }

    /**
     * Test string generation
     */
    public void testString4() {
        String template = "{string:a-c\\-/8}";
        String actual = new StringSubstitute().substitute(template);
        assertNotSame(template, actual);
        assertEquals(8, actual.length());
    }

    /**
     * Test string generation
     */
    public void testString5() {
        String template = "{string:a-z\\-_\\/8}";
        String actual = new StringSubstitute().substitute(template);
        assertNotSame(template, actual);
        assertEquals(8, actual.length());
    }

}
