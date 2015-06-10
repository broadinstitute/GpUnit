package org.genepattern.gpunit.exec.rest;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;

import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.diff.ServerDiff;
import org.genepattern.gpunit.download.JobResultDownloader;
import org.genepattern.gpunit.exec.rest.json.JobResultObj;
import org.genepattern.gpunit.BatchModuleTestObject;
import org.genepattern.gpunit.BatchProperties;
import org.genepattern.gpunit.GpAssertions;
import org.genepattern.gpunit.TestFileObj;
import org.genepattern.gpunit.validator.JobResultValidatorGeneric;
import org.junit.Assert;

public class JobResultValidatorRest extends JobResultValidatorGeneric {
    private JobResultObj jobResult;
    private final JobResultValidatorRest parentJobValidator; // represents that parent job if this validator is for a server diff job
    private JobRunnerRest restClient;
    private JobResultDownloaderRest downloader=null;

    public JobResultValidatorRest(final BatchProperties props, final BatchModuleTestObject batchTestObject, final File downloadDir, final JobResultValidatorRest parentJobValidator) {
        super(props, batchTestObject, downloadDir);
        this.parentJobValidator = parentJobValidator;
    }
    
    public void setRestClient(JobRunnerRest restClient) {
        this.restClient=restClient;
    }
    
    public void setJobStatus(final JobResultObj jobResult) {
        this.jobResult=jobResult;
    }

    // When running a server diff job that is executed "on behalf of" a real test and fails,
    // we want to retain the specifics of the reason for the diff job's failure, but supplement
    // that failure message with information about the original upstream job since really thats
    // the one that we want to report as failing.
    @Override 
    protected String createErrorMessage(String sourceMessage) {
        if (parentJobValidator != null) {
            return "Error processing diff for job # " + parentJobValidator.getJobId() + ": " + sourceMessage;
        }
        else {
            return super.createErrorMessage(sourceMessage);
        }
    }

    @Override
    protected void validateJobStatus() {
        GpAssertions assertions = getTestCase().getAssertions();

        boolean expectedHasStdError = false;
        if (assertions != null && assertions.getJobStatus().trim().length() > 0) {
            //check to see if it's a test-case with an expected stderr.txt output
            expectedHasStdError = !"success".equalsIgnoreCase(assertions.getJobStatus());
        }

        //case 1: expecting stderr
        if (expectedHasStdError) {
            Assert.assertTrue(createErrorMessage("job #"+getJobId()+" doesn't have stderr.txt output"), hasStdError());
            return;
        }
        //case 2: unexpected stderr
        if (hasStdError() && !expectedHasStdError) {
            String junitMessage = "job #"+getJobId()+" has stderr.txt output: ";
            //try to download the error message
            String errorMessage = getErrorMessageFromStderrFile();
            junitMessage += NL + errorMessage;
            try {
                  parentJobValidator.getDownloader().downloadResultFiles();
            }
            catch (GpUnitException e) {
                Assert.fail(createErrorMessage(e.getLocalizedMessage()));
            }
            Assert.fail(createErrorMessage(junitMessage));
        }
    }

    @Override
    public void checkInit() {
        assertNotNull(createErrorMessage("jobResult==null"), jobResult);
        final String jobId=jobResult.getJobId();
        assertNotNull(createErrorMessage("jobId==null"), jobId);
        super.setJobId(jobId);
        
        Assert.assertNotNull("restClient==null", restClient);
        this.downloader=new JobResultDownloaderRest(getDownloadDir(), getBatchProperties());
        try {
            this.downloader.setRestClient(restClient);
            this.downloader.setJobResult(jobResult);
        }
        catch (Throwable t) {
            Assert.fail(createErrorMessage("Error initializing validator for jobId="+getJobId()+
                    ": "+t.getLocalizedMessage()));
        }
    }

    @Override
    public boolean hasStdError() {
        try {
            return jobResult.hasError();
        }
        catch (Exception e) {
            Assert.fail(createErrorMessage("Error getting 'hasError' for jobId="+
                    getJobId()+": "+e.getLocalizedMessage()));
        }
        catch (Throwable t) {
            Assert.fail(createErrorMessage("Unexpected exception: "+t.getLocalizedMessage()));
        }
        //shouldn't be here
        return true;
    }

    /**
     * Validate a remote server diff job that was run on behalf of an upstream test job that
     * previously succeeded.
     *
     * @author cnorman
     *
     */
    public void validateRemoteDiff() {
        //1) initialization check
        checkInit();

        //2) job status
        validateJobStatus();
    }

    /**
     * Validate the results of a test using remote server diffs; don't download any result files.
     *
     * @author cnorman
     *
     */
    public void validateRemote() {
        //1) initialization check
        checkInit();

        //2) job status
        validateJobStatus();

        GpAssertions assertions = getTestCase().getAssertions();
        if (assertions.getFiles() != null) {
            for(Entry<String,TestFileObj> entry : assertions.getFiles().entrySet()) {
                String filename = entry.getKey();
                Assert.assertTrue(createErrorMessage("job #"+getJobId()+", Expecting result file named '"+filename+"'"), hasResultFile(filename));
                TestFileObj testFileObj = entry.getValue();
                String expected = testFileObj.getDiff();
                if (expected != null) {
                    ServerDiff serverDiff = new ServerDiff(this);
                    serverDiff.setArgs(getRemoteDiffArgs(assertions, testFileObj));
                    serverDiff.setJobId(""+getJobId());
                    String actualURL = getDownloader().getServerURLForFile(filename);
                    serverDiff.diff(actualURL, expected);
                }
            }
        }
    }

    private List<String> getRemoteDiffArgs(GpAssertions assertions, TestFileObj remoteFileObj) {
        List<String> argList = remoteFileObj.getDiffCmdArgs();
        if (null == argList) {
            argList = assertions.getDiffCmdArgs();
        }
        return argList;
    }

    @Override
    public JobResultDownloader getDownloader() {
        if (downloader == null) {
            Assert.fail(createErrorMessage("downloader not initialized"));
        }
        return downloader;
    }

    @Override
    public void deleteJob() throws GpUnitException {
        throw new GpUnitException(createErrorMessage("GpUnit configuration error, deleteJob Method not implemented!"));
    }

}
