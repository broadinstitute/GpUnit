package org.genepattern.gpunit.debug;

import static org.junit.Assert.fail;

import org.genepattern.gpunit.BatchModuleTestObject;
import org.genepattern.gpunit.BatchProperties;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.exec.rest.RestClientUtil;
import org.genepattern.gpunit.exec.soap.SoapClientUtil;

/**
 * for debugging, do a GpUnit test for a single ModuleTestObject, typically a single test_yaml file.
 */
public class ModuleTest {
    public static void doModuleTest(final BatchProperties batchProps, final BatchModuleTestObject testObj) throws GpUnitException {
        if (testObj == null) {
            fail("testObj == null");
        }

        if (testObj.hasInitExceptions()) {
            fail(testObj.getInitException().getLocalizedMessage());
        }

        //submit a job via new REST API
        if (batchProps.getClient().equals(BatchProperties.GpUnitClient.REST)) {
            RestClientUtil.runTest(batchProps, testObj, null);
            return;
        }

        // otherwise use legacy SOAP API
        SoapClientUtil.runTest(batchProps,testObj);
    }

}
