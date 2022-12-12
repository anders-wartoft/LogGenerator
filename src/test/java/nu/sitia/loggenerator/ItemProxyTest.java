package nu.sitia.loggenerator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import nu.sitia.loggenerator.io.MemoryOutputItem;
import nu.sitia.loggenerator.io.WrappedFileInputItem;
import nu.sitia.loggenerator.util.Configuration;

import java.util.*;

/**
 * Unit test for simple App.
 */
public class ItemProxyTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ItemProxyTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( ItemProxyTest.class );
    }

    /**
     * Test file read and write to "Cmd" output
     */
    public void testItemProxy()
    {
        Configuration config = new Configuration();
        config.setInputName("src/test/data/test.txt");
        WrappedFileInputItem fii = new WrappedFileInputItem(config);
        MemoryOutputItem moi = new MemoryOutputItem();
        ItemProxy proxy = new ItemProxy(fii, moi, new ArrayList<>(), config);
        proxy.pump();
        List<String> result = moi.getData();
        List<String> fileContents = fii.getData();
        System.out.println("File contains " + result.size() + " rows");
        assertEquals(result.size(), fileContents.size());

        Iterator<String> ri = result.iterator();
        Iterator<String> fi = fileContents.iterator();
        while ( ri.hasNext() && fi.hasNext() ) {
            String x = ri.next();
            String y = fi.next();
            assertEquals(x, y);
        }
    }


    /**
     * Test file read and write to "Cmd" output with batches
     */
    public void testItemProxyBatches()
    {
        int batchSize = 3;
        Configuration config = new Configuration();
        config.setInputName("src/test/data/test.txt");
        // This is a FileInputItem that also caches all data, so we can get it with fii.getData() later
        WrappedFileInputItem fii = new WrappedFileInputItem(config);
        fii.setBatchSize(batchSize);
        MemoryOutputItem moi = new MemoryOutputItem();
        moi.setBatchSize(batchSize);
        ItemProxy proxy = new ItemProxy(fii, moi, new ArrayList<>(), config);
        proxy.pump();
        List<String> result = moi.getData();
        List<String> fileContents = fii.getData();
        System.out.println("File contains " + result.size() + " rows");

        // Since batches adds strings together in sending and/or receiving, we must
        // unpack the sent and received strings before we compare.
        // First, the sent strings:
        Iterator<String> fi = fileContents.iterator();
        List<String> fileStrings = new ArrayList<>();
        while ( fi.hasNext() ) {
            String data = fi.next();
            Collections.addAll(fileStrings, data.split("\n"));
        }
        // Now we do the same with the received strings:
        Iterator<String> ri = result.iterator();
        List<String> receivedStrings = new ArrayList<>();
        while ( ri.hasNext() ) {
            String data = ri.next();
            receivedStrings.addAll(Arrays.asList(data.split("\n")));
        }
        // Now they should be equal:
        System.out.println(fileStrings);
        System.out.println(receivedStrings);
        assertEquals(fileStrings, receivedStrings);
    }
}
