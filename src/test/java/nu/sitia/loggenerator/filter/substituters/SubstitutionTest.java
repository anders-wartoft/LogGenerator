package nu.sitia.loggenerator.filter.substituters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit test for simple App.
 */
public class SubstitutionTest
    extends TestCase
{
    private final Map<String, String> testData = new HashMap<>();

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public SubstitutionTest(String testName )
    {
        super( testName );
        testData.put("foo", "bar");
        testData.put("x", "y");
        testData.put("some", "all");
        testData.put("peace", "good");
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( SubstitutionTest.class );
    }


    /**
     * Test substitution
     */
    public void testSubstitution1()
    {
        String template = "Test if one {foo} can be substituted";
        String expected = "Test if one bar can be substituted";
        String actual = new Substitution().substitute(template, testData, new Date());
        assertEquals(expected, actual);
    }

    /**
     * Test substitution
     */
    public void testSubstitution2()
    {
        String template = "Test if two {foo} {peace} can be substituted";
        String expected = "Test if two bar good can be substituted";
        String actual = new Substitution().substitute(template, testData, new Date());
        assertEquals(expected, actual);
    }

    /**
     * Test substitution
     */
    public void testSubstitution3()
    {
        String template = "Test if one illegal {foo2} will be substituted";
        String actual = new Substitution().substitute(template, testData, new Date());
        assertEquals(template, actual);
    }

}
