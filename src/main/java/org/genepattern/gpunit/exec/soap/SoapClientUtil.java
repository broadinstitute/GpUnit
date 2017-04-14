package org.genepattern.gpunit.exec.soap;

import java.io.File;

import org.genepattern.client.GPClient;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.BatchModuleTestObject;
import org.genepattern.gpunit.BatchProperties;
import org.genepattern.webservice.JobResult;


public class SoapClientUtil {
    static public void runTest(final BatchProperties batchProps, final BatchModuleTestObject testObject) 
    throws GpUnitException
    {
        if (batchProps == null) {
            throw new GpUnitException("batchProps is null");
        }
        if (testObject == null) {
            throw new GpUnitException("testObject is null");
        }
        if (testObject.hasInitExceptions()) {
            Throwable initError = testObject.getInitException();
            throw new GpUnitException("testObject initialization error", initError);
        }
        if (testObject.getTestCase() == null) {
            throw new GpUnitException("testObject.testCase is null");
        }
        
        //initialize gpClient
        GPClient gpClient = ModuleRunner.initGpClient(batchProps);

        ModuleRunner runner = new ModuleRunner(testObject.getTestCase());
        runner.setGpClient(gpClient);
        runner.setBatchProperties(batchProps);
        runner.runJobAndWait();
        JobResult jobResult = runner.getJobResult();
        
        File jobResultDir=testObject.getJobResultDir(batchProps.getBatchOutputDir(), jobResult);
        
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
