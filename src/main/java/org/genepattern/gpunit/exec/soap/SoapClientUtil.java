package org.genepattern.gpunit.exec.soap;

import java.io.File;
import java.io.FileNotFoundException;

import org.genepattern.client.GPClient;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.test.BatchModuleTestObject;
import org.genepattern.gpunit.test.BatchProperties;
import org.genepattern.webservice.JobResult;


public class SoapClientUtil {
    static public void runTest(final BatchProperties batchProps, final BatchModuleTestObject testObject) throws GpUnitException, FileNotFoundException, AssertionError, Exception {
        if (batchProps == null) {
            throw new GpUnitException("batchProps is null");
        }
        if (testObject == null) {
            throw new GpUnitException("testObject is null");
        }
        if (testObject.getTestCase() == null) {
            throw new GpUnitException("testObject.testCase is null");
        }
        Throwable initError = testObject.getInitException();
        if (initError != null) {
            throw new Exception(initError.getMessage(), initError);
        }
        if (testObject.getTestCase() == null) {
            throw new Exception("testObject is null");
        }
        
        //initialize gpClient
        GPClient gpClient = ModuleRunner.initGpClient(batchProps);

        ModuleRunner runner = new ModuleRunner(testObject.getTestCase());
        runner.setGpClient(gpClient);
        runner.setBatchProperties(batchProps);
        runner.runJobAndWait();
        JobResult jobResult = runner.getJobResult();
        
        File jobResultDir=batchProps.getJobResultDir(testObject, jobResult);
        
        JobResultValidatorSoap validator=new JobResultValidatorSoap(batchProps, testObject, jobResultDir);
        validator.setJobResult(jobResult);
        validator.setModuleRunner(runner);
        try {
            validator.validate();
        }
        finally {
            if (jobResult != null) {
                validator.clean();
            }
        }
    }


}
