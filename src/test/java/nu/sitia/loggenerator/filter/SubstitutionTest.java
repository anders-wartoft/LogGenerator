package nu.sitia.loggenerator.filter;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import nu.sitia.loggenerator.util.Ipv4;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
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
        String actual = Substitution.substitute(template, testData, new Date());
        assertEquals(expected, actual);
    }

    /**
     * Test substitution
     */
    public void testSubstitution2()
    {
        String template = "Test if two {foo} {peace} can be substituted";
        String expected = "Test if two bar good can be substituted";
        String actual = Substitution.substitute(template, testData, new Date());
        assertEquals(expected, actual);
    }

    /**
     * Test substitution
     */
    public void testSubstitution3()
    {
        String template = "Test if one illegal {foo2} will be substituted";
        String actual = Substitution.substitute(template, testData, new Date());
        assertEquals(template, actual);
    }

    /**
     * Test date patterns
     */
    public void testDatePatterns() {
        String template = "{date:yyyy-MM-dd}";
        String expected = "2100-01-01";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        Date date;
        try {
            date = formatter.parse(expected);
            String actual = Substitution.calculateDate(template, date);
            assertEquals(expected, actual);
        } catch (ParseException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test date patterns
     */
    public void testDatePatternsWithLocale() {
        String template = "{date:yyyy-MM-dd/sv:SE}";
        String expected = "2100-01-01";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        Date date;
        try {
            date = formatter.parse(expected);
            String actual = Substitution.calculateDate(template, date);
            assertEquals(expected, actual);
        } catch (ParseException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test date patterns
     */
    public void testDatePatternsWithLocale2() {
        String template = "{date:MMM dd HH:mm:ss/en:US}";
        String dateString = "2100-01-01";
        String expected = "Jan 01 00:00:00";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        Date date;
        try {
            date = formatter.parse(dateString);
            String actual = Substitution.calculateDate(template, date);
            assertEquals(expected, actual);
        } catch (ParseException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test ipv4 substitution
     */
    public void testSubstitutionIpv4a()
    {
        String template = "Test if one {ipv4:192.168.1.1/32} can be substituted";
        String expected = "Test if one 192.168.1.1 can be substituted";
        String actual = Substitution.substitute(template, testData, new Date());
        assertEquals(expected, actual);
    }

    /**
     * Test ipv4 substitution
     */
    public void testSubstitutionIpv4b()
    {
        String template = "Test if one {oneOf:{ipv4:192.168.0.0/16},{ipv4:172.16.0.0/12},{ipv4:10.0.0.0/8}} can be substituted";
        String left = "Test if one ";
        String right = " can be substituted";
        String actual = Substitution.substitute(template, testData, new Date());
        assertTrue(actual.startsWith(left));
        assertTrue(actual.endsWith(right));
    }


    /**
     * Test number of addresses in a subnet
     */
    public void testNrAddress() {
        String cidr = "32";
        long nr = Ipv4.nrValuesInSubnet(cidr);
        assertEquals(1, nr);
        assertEquals(2, Ipv4.nrValuesInSubnet("31"));
        assertEquals(4, Ipv4.nrValuesInSubnet("30"));
        assertEquals(8, Ipv4.nrValuesInSubnet("29"));
        assertEquals(256, Ipv4.nrValuesInSubnet("24"));
        assertEquals(16777216L, Ipv4.nrValuesInSubnet("8"));

    }

    /**
     * Test the conversion for ipv4 String to/from long
     */
    public void testIpv4ToLong() {
        assertEquals("192.168.1.1", Ipv4.longToIpv4(Ipv4.ipv4ToLong("192.168.1.1")));
    }

    /**
     * Test to get a starting address from a subnet
     */
    public void testStartingAddress() {
        String address = "192.168.1.182";
        String cidr = "24";
        String expected = "192.168.1.0";
        String actual =
                Ipv4.longToIpv4(
                   Ipv4.getStartingAddress(address, cidr)
                );
        assertEquals(expected, actual);
    }


    /**
     * Test to get a random address from a subnet
     */
    public void testRandomAddress() {
        String address = "192.168.1.182";
        String cidr = "24";
        String actual =
                Ipv4.longToIpv4(
                        Ipv4.getRandomIpv4(address, cidr)
                );
        assertTrue(address + " and " + actual + " are not within " + cidr, Ipv4.isInSubnet(address, actual, cidr));
    }

    /**
     * Test oneOf
     */
    public void testOneOf() {
        String template = "test foo {oneOf:a,b,c,d} bar";
        String actual = Substitution.calculateOneOf(template);
        assertEquals(14, actual.length());

    }

    /**
     * Test random
     */
    public void testRandom() {
        String template = "{random:1-6}";
        String actual = Substitution.calculateRandom(template);
        int nr = Integer.parseInt(actual);
        assertTrue(nr >= 1 && nr <= 6);
    }

    /**
     * Test string generation
     */
    public void testString() {
        String template = "{string:a/10}";
        String expected = "aaaaaaaaaa";
        String actual = Substitution.calculateString(template);
        assertEquals(expected, actual);
    }


    /**
     * Test string generation
     */
    public void testString2() {
        String template = "{string:a-z/10}";
        String actual = Substitution.calculateString(template);
        assertNotSame(template, actual);
        assertEquals(10, actual.length());
    }

    /**
     * Test string generation
     */
    public void testString3() {
        String template = "{string:a-z0-9\\-/100}";
        String actual = Substitution.calculateString(template);
        assertNotSame(template, actual);
        assertEquals(100, actual.length());
    }

    /**
     * Test string generation
     */
    public void testString4() {
        String template = "{string:a-c\\-/8}";
        String actual = Substitution.calculateString(template);
        assertNotSame(template, actual);
        assertEquals(8, actual.length());
    }

    /**
     * Test string generation
     */
    public void testString5() {
        String template = "{string:a-z\\-_\\/8}";
        String actual = Substitution.calculateString(template);
        assertNotSame(template, actual);
        assertEquals(8, actual.length());
    }

    /**
     * Test counter generation
     */
    public void testCounter() {
        String template = "{counter:myCounter:40}";
        String expected = "40";
        String actual = Substitution.calculateCounter(template);
        assertEquals(expected, actual);
        expected = "41";
        actual = Substitution.calculateCounter(template);
        assertEquals(expected, actual);
    }

    /**
     * Test lorem
     */
    public void testLorem() {
        String template = "{lorem:1:myword/ }";
        String expected = "myword";
        String actual = Substitution.calculateLorem(template);
        assertEquals(expected, actual);
    }

    /**
     * Test lorem
     */
    public void testLorem2() {
        String template = "{lorem:myword myword2 myword3/ }";
        try {
            Substitution.calculateLorem(template);
            fail("Should throw exception");
        } catch (RuntimeException e) {
            // ignore. Should throw exception
        }

    }

    /**
     * Test lorem
     */
    public void testLorem3() {
        String template = "{lorem:4:myword/ }";
        String expected = "myword myword myword myword";
        String actual = Substitution.calculateLorem(template);
        assertEquals(expected, actual);
    }

    /**
     * Test lorem
     */
    public void testLorem4() {
        String template = "{lorem:42:create a story by scrambling a lot of words/ }";
        String actual = Substitution.calculateLorem(template);
        assertNotSame(template, actual);
    }

    /**
     * Test to extract an expression from a String
     */
    public void testMatchingBrace() {
        String template = "test {first:a,b,{second:d,e}}";
        int pos = template.indexOf("{first:");
        int end = Substitution.getExpressionEnd(template, pos);
        assertEquals(29, end);
    }

    /**
     * Test to extract an expression from a String
     */
    public void testMatchingBrace2() {
        String template = "<{pri:}>{date:MMM DD HH:mm:ss} {oneOf:mymachine,yourmachine,localhost,{ipv4:192.168.0.0/16}} {string:a-z0-9/9}[{random:1-65535}]: ";
        int pos = template.indexOf("{oneOf:");
        int end = Substitution.getExpressionEnd(template, pos);
        assertEquals(92, end);
    }


}
