package org.genepattern.gpunit.soap;

import java.io.File;

import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.BatchModuleTestObject;
import org.genepattern.gpunit.BatchProperties;
import org.genepattern.webservice.JobResult;


public class SoapClientUtil {
    /**
     * initialize the jobId from the soap client JobResult object
     *
     * @param jobResult
     * @return the jobId or null if not set
     * @throws GpUnitException
     */
    protected static String getJobId(final JobResult jobResult) throws GpUnitException {
        if (jobResult != null && jobResult.getJobNumber()>=0) {
            return ""+jobResult.getJobNumber();
        }
        return null;
    }

    public static void runTest(final BatchProperties batchProps, final BatchModuleTestObject testObject) 
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
        
        ModuleRunnerSoap runner = new ModuleRunnerSoap(batchProps, testObject.getTestCase());
        JobResult jobResult = runner.runJobAndWait();
        
        final String jobId=getJobId(jobResult);
        File jobResultDir=testObject.getJobResultDir(batchProps.getBatchOutputDir(), jobId);
        
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
