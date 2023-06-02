package nu.sitia.loggenerator.util.gapdetector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for Gap Detector.
 */
public class GapDetectorTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public GapDetectorTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( GapDetectorTest.class );
    }

    /**
     * Gap Test
     */
    public void testGap()
    {
        Gap gap = new Gap(2,44);
        GapDetector detector = new GapDetector();
        detector.addGap(gap);
        detector.setExpectedNumber(46);
        assertTrue(detector.check(1) < 0);
        assertTrue(detector.check(2) < 0);
        assertTrue(detector.check(48) > 0);
        assertTrue(detector.check(46) < 0);
        assertEquals(0, detector.check(49));
        assertTrue(detector.check(5) < 0);
        detector.setExpectedNumber(100);
        assertTrue(detector.check(77) < 0);
        System.out.println(detector);
    }


    /**
     * Gap Test
     */
    public void testNumberOfReceivedNumbers()
    {
        GapDetector detector = new GapDetector();
        detector.setDuplicateDetection(true);
        detector.setExpectedNumber(1);
        detector.check(1);
        detector.check(2);
        detector.check(2);
        detector.check(2);
        detector.check(4);
        detector.check(4);
        detector.check(4);
        detector.check(4);
        detector.check(5);
        detector.setExpectedNumber(7);
        System.out.println(detector);
    }

}
