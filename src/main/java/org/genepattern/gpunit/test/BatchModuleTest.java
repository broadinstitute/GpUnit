package org.genepattern.gpunit.test;

import org.genepattern.gpunit.BatchModuleTestObject;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.exec.rest.RestClientUtil;

public class BatchModuleTest extends AbstractBatchModuleTest {
    public BatchModuleTest(final int batchIdx, final BatchModuleTestObject testObj) {
        super(batchIdx, testObj);
    }
    
    /** Run a test job with REST API */
    public void runTest() throws GpUnitException {
        RestClientUtil.runTest(getBatchProps(), getTestObj(), null);
    }

}
