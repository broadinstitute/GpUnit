package org.genepattern.gpunit.debug;

import static org.junit.Assert.fail;

import java.io.File;

import org.genepattern.gpunit.BatchModuleTestObject;
import org.genepattern.gpunit.BatchProperties;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.test.BatchModuleUtil;
import org.junit.Ignore;

/**
 * for debugging, do a GpUnit test for a single ModuleTestObject, typically a single test_yaml file.
 */
@Ignore
abstract public class AbstractModuleTest {
    private BatchProperties gpClient;
    private File testFile;
    
    public AbstractModuleTest(final BatchProperties gpClient, final File testFile) {
        this.gpClient=gpClient;
        this.testFile=testFile;
    }
    
    abstract public void runTest(final BatchProperties gpClient, final BatchModuleTestObject testObj) throws GpUnitException;
    
    public void doModuleTest() throws GpUnitException {
        final BatchModuleTestObject testObj=BatchModuleUtil.initBatchModuleTestObject(testFile);
        if (testObj == null) {
            fail("testObj == null");
        }
        if (testObj.hasInitExceptions()) {
            fail(testObj.getInitException().getLocalizedMessage());
        }
        runTest(gpClient, testObj);
    }

}
