package org.genepattern.gpunit.test;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;


/**
 * Run a batch of jobs, one for each protocol.
 * 
 * @author pcarr
 */
public class ProtocolsTest extends BatchModuleTest { 

    /**
     * @see BatchModuleTest#data()
     */
    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return BatchModuleUtil.data(new File("./tests/protocols"));
    }
    
    public ProtocolsTest(final int batchIdx, final String testname, final BatchModuleTestObject testObj) {
        super(batchIdx, testname, testObj);
    }

}
