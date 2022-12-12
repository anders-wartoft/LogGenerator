package nu.sitia.loggenerator.filter.substituters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import nu.sitia.loggenerator.util.Ipv4;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit test for simple App.
 */
public class Ipv4SubstituteTest
    extends TestCase {

    private final Map<String, String> testData = new HashMap<>();

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public Ipv4SubstituteTest(String testName )
    {
        super( testName );
        testData.put("foo", "bar");
        testData.put("x", "y");
        testData.put("some", "all");
        testData.put("ipv4", "{ipv4:0.0.0.0/0}");
    }
    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( Ipv4SubstituteTest.class );
    }




    /**
     * Test ipv4 substitution
     */
    public void testSubstitutionIpv4a()
    {
        String template = "Test if one {ipv4:192.168.1.1/32} can be substituted";
        String expected = "Test if one 192.168.1.1 can be substituted";
        String actual = new Ipv4Substitute().substitute(template);
        assertEquals(expected, actual);
    }

    /**
     * Test ipv4 substitution
     */
    public void testAllSubstitutionIpv4()
    {
        String template = "{ipv4:0.0.0.0/0}";
        String actual = new Ipv4Substitute().substitute(template);
        System.out.println(actual);
        assertTrue(actual.length() <= 15 && actual.length() >= 7);
    }

    /**
     * Test ipv4 substitution
     */
    public void testIpv4Shorthand()
    {
        String template = "{ipv4}";
        String actual = new Substitution().substitute(template, testData, new Date());
        System.out.println(actual);
        assertTrue(actual.length() <= 15 && actual.length() >= 7);
    }

    /**
     * Test ipv4 substitution
     */
    public void testSubstitutionIpv4b()
    {
        String template = "Test if one {oneOf:{ipv4:192.168.0.0/16},{ipv4:172.16.0.0/12},{ipv4:10.0.0.0/8}} can be substituted";
        String left = "Test if one ";
        String right = " can be substituted";
        String actual = new Ipv4Substitute().substitute(template);
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


}
