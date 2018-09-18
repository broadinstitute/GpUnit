package org.genepattern.gpunit.debug;

import java.io.File;

import org.genepattern.gpunit.BatchModuleTestObject;
import org.genepattern.gpunit.BatchProperties;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.exec.rest.RestClientUtil;
import org.junit.Ignore;

@Ignore
public class RestModuleTest extends AbstractModuleTest {

    public RestModuleTest(final BatchProperties gpClient, final File testFile) {
        super(gpClient, testFile);
    }

    @Override
    public void runTest(BatchProperties gpClient, BatchModuleTestObject testObj) throws GpUnitException {
        RestClientUtil.runTest(gpClient, testObj, null);        
    }

}
