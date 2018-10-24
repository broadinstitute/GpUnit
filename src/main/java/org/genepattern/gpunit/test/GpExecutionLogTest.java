package org.genepattern.gpunit.test;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;
import org.genepattern.gpunit.BatchModuleTestObject;


/**
 * Run one unit test for each *test.yaml file in the 'test.dir' directory.
 * Files are searched recursively from the directory set by the 'test.dir' System property.
 * 
 * @author pcarr
 */
public class GpExecutionLogTest extends BatchModuleTest { 

    /**
     * @see BatchModuleUtil#data()
     */
    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return BatchModuleUtil.data(new File("./tests/saved_jobs"));
    }
    
    public GpExecutionLogTest(final int batchIdx, final BatchModuleTestObject testObj) {
        super(batchIdx, testObj);
    }

}
