package org.genepattern.gpunit.soap.test;

import org.genepattern.gpunit.BatchModuleTestObject;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.soap.SoapClientUtil;
import org.genepattern.gpunit.test.AbstractBatchModuleTest;

public class GpUnitSoap extends AbstractBatchModuleTest {
    public GpUnitSoap(final int batchIdx, final BatchModuleTestObject testObj) {
        super(batchIdx, testObj);
    }
    
    /** run test with legacy SOAP API */
    public void runTest() throws GpUnitException {
        SoapClientUtil.runTest(getBatchProps(), getTestObj());
    }

}
