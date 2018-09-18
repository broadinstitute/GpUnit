package org.genepattern.gpunit.soap.debug;

import java.io.File;

import org.genepattern.gpunit.BatchModuleTestObject;
import org.genepattern.gpunit.BatchProperties;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.debug.AbstractModuleTest;
import org.genepattern.gpunit.soap.SoapClientUtil;

import org.junit.Ignore;

@Ignore
public class SoapModuleTest extends AbstractModuleTest {

    public SoapModuleTest(final BatchProperties gpClient, final File testFile) {
        super(gpClient, testFile);
    }

    @Override
    public void runTest(BatchProperties gpClient, BatchModuleTestObject testObj) throws GpUnitException {
        SoapClientUtil.runTest(gpClient,testObj);       
    }

}
