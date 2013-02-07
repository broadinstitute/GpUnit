package org.genepattern.gpunit.yaml;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.genepattern.gpunit.GpAssertions;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.gpunit.TestFileObj;
import org.genepattern.gpunit.diff.AbstractDiffTest;
import org.genepattern.gpunit.diff.CmdLineDiff;
import org.genepattern.gpunit.diff.NumRowsColsDiff;
import org.genepattern.gpunit.diff.UnixCmdLineDiff;
import org.genepattern.gpunit.download.soap.v2.DownloaderV2;
import org.genepattern.gpunit.test.BatchModuleTestObject;
import org.genepattern.gpunit.test.BatchProperties;
import org.genepattern.webservice.JobResult;
import org.junit.Assert;

/**
 * After the job completes on the server, run through the list of assertions.
 * 
 * @author pcarr
 */
public class JobResultValidator {
    final static String NL = System.getProperty("line.separator");

    final private ModuleTestObject testCase;
    final private int jobNumber;
    final private File downloadDir;
    private boolean saveResultFiles=false;
    private boolean deleteCompletedJobs=true;
    final private boolean actualHasStdError;
    
    private DownloaderV2 downloader;
    
    public JobResultValidator(final BatchProperties props, final BatchModuleTestObject batchTestObject, final JobResult jobResult, final File downloadDir) {
        if (batchTestObject==null) {
            throw new IllegalArgumentException("batchTestObject==null");
        }
        if (jobResult==null) {
            throw new IllegalArgumentException("jobResult==null");
        }
        if (jobResult.getJobNumber() < 0) {
            throw new IllegalArgumentException("jobNumber=="+jobResult.getJobNumber());
        }
        if (downloadDir==null) {
            throw new IllegalArgumentException("downloadDir==null");
        }
        this.testCase = batchTestObject.getTestCase();
        if (this.testCase==null) {
            throw new IllegalArgumentException("testCase==null");
        }
        this.downloadDir = downloadDir;
        this.jobNumber=jobResult.getJobNumber();
        this.actualHasStdError = jobResult.hasStandardError();
        
        this.downloader=new DownloaderV2(downloadDir, props, jobResult);
    }

    public void setSaveResultFiles(final boolean b) {
        this.saveResultFiles=b;
    }
    
    public void setDeleteCompletedJobs(final boolean b) {
        this.deleteCompletedJobs=b;
    }
    
    /**
     * Read the error message by downloading 'stderr.txt' result file and returning 
     * a String containing the first MAX_N lines of the file.
     * 
     * @return
     */
    private String getErrorMessageFromStderrFile() {
        String errorMessage="";
        File stderrFile = null;
        try {
            stderrFile=downloader.getResultFile("stderr.txt");
        }
        catch (Throwable t) {
            errorMessage = "There was an error downloading 'stderr.txt': "+t.getLocalizedMessage();
        }
        if (stderrFile != null) {
            LineNumberReader reader=null;
            try {
                reader = new LineNumberReader(new FileReader(stderrFile));
                int n=0;
                int MAX_N=12;
                String line;
                while ( ((line = reader.readLine()) != null) && n<MAX_N) {
                    ++n;
                    if (errorMessage.length()>0) { errorMessage += NL; }
                    errorMessage += line;
                }
            }
            catch (Throwable t) {
                if (errorMessage.length()>0) { errorMessage += NL; }
                errorMessage += "There was an error reading 'stderr.txt': "+t.getLocalizedMessage();
            }
            finally {
                if (reader!=null) {
                    try {
                        reader.close();
                    }
                    catch (IOException e) {
                        //ignoring
                    }
                }
            }
        }
        return errorMessage;
    }
    
    private void validateJobStatus() {
        GpAssertions assertions = testCase.getAssertions();
        
        boolean expectedHasStdError = false;
        if (assertions != null && assertions.getJobStatus().trim().length() > 0) {
            //check to see if it's a test-case with an expected stderr.txt output
            expectedHasStdError = !"success".equalsIgnoreCase(assertions.getJobStatus());
        }
        
        //case 1: expecting stderr
        if (expectedHasStdError) {
            Assert.assertTrue("job #"+jobNumber+" doesn't have stderr.txt output", actualHasStdError);
            return;
        }
        //case 2: unexpected stderr
        if (actualHasStdError && !expectedHasStdError) {
            String junitMessage = "job #"+jobNumber+" has stderr.txt output: ";
            //try to download the error message
            String errorMessage = getErrorMessageFromStderrFile();
            junitMessage += NL + errorMessage;
            Assert.fail(junitMessage);
        }
    }
    
    public void validate() {
        //1) null jobResult
        //Assert.assertNotNull("jobResult is null", jobResult);
        
        //2) job status
        validateJobStatus();
        
        //if necessary, download result files
        if (this.saveResultFiles) {
            try {
                downloader.downloadResultFiles();
            }
            catch (Exception e) {
                Assert.fail(e.getLocalizedMessage());
            }
        }

        //3) numFiles: ...
        GpAssertions assertions = testCase.getAssertions();
        if (assertions.getNumFiles() >= 0) {
            //Note: when numFiles < 0, it means don't run this assertion
            Assert.assertEquals("Number of result files", assertions.getNumFiles(), downloader.getNumResultFiles());
        }

        //4) outputDir: ...
        if (assertions.getOutputDir() != null) {
            try {
                downloader.downloadResultFiles();
            }
            catch (Throwable t) {
                    t.printStackTrace();
                    Assert.fail("Error downloading result files for job='"+jobNumber+"': "+t.getLocalizedMessage());
            }
            File expectedOutputDir = assertions.getOutputDir();
            directoryDiff(expectedOutputDir, downloadDir);
        }
        
        //5) files: ... 
        //   (may or may not have already downloaded result files; assume that we haven't, because it doesn't make sense
        //    to include outputDir and files assertion in the same test)
        if (assertions.getFiles() != null) {
            //if (outputFilenames == null) {
            //    initOutputFilenames();
            //}
            for(Entry<String,TestFileObj> entry : assertions.getFiles().entrySet()) {
                String filename = entry.getKey();
                Assert.assertTrue("Expecting result file named '"+filename+"'", downloader.hasResultFile(filename));
                TestFileObj testFileObj = entry.getValue();
                if (testFileObj != null) {
                    //need to download the file ...
                    File actual = null;
                    try {
                        actual = downloader.getResultFile(filename);
                    }
                    catch (Throwable t) {
                        t.printStackTrace();
                        Assert.fail("Error downloading result file '"+filename+"': "+t.getLocalizedMessage());
                    }
                    String diff = testFileObj.getDiff();
                    if (diff != null) {
                        File expected = testCase.initFileFromPath(diff);
                        //diff(expected,actual);
                        AbstractDiffTest diffTest = getDiff(testFileObj);
                        diffTest.setJobId(""+jobNumber);
                        diffTest.setInputDir(testCase.getInputdir());
                        diffTest.setExpected(expected);
                        diffTest.setActual(actual);
                        diffTest.diff();
                    }
                    int numCols = testFileObj.getNumCols();
                    int numRows = testFileObj.getNumRows();
                    if (numCols >= 0 || numRows >= 0) {
                        NumRowsColsDiff nf = new NumRowsColsDiff();
                        if (numCols >= 0) {
                            nf.setExpectedNumCols(numCols);
                        }
                        if (numRows >= 0) {
                            nf.setExpectedNumRows(numRows);
                        }
                        nf.setInputDir(testCase.getInputdir());
                        nf.setActual(actual);
                        nf.diff();
                    }
                }
            }
        }
    }

    public void clean(final ModuleRunner runner) throws GpUnitException {
        //optionally, clean downloaded result files
        if (!saveResultFiles) {
            try {
                downloader.cleanDownloadedFiles();
            }
            catch (Exception e) {
                Assert.fail(e.getLocalizedMessage());
            }
        }
        //optionally, remove job from server
        if (deleteCompletedJobs) {
            deleteJob(runner);
        }
    }
    
    private void deleteJob(final ModuleRunner runner) throws GpUnitException {
        if (jobNumber<0) {
            return;
        }
        runner.deleteJob(jobNumber);
    }
    
    private static Comparator<File> filenameComparator = new Comparator<File>() {
        public int compare(File arg0, File arg1) {
            return arg0.getName().toLowerCase().compareTo(arg1.getName().toLowerCase());
        }
    };
    
    private static Comparator<File> filepathComparator = new Comparator<File>() {
        public int compare(File arg0, File arg1) {
            return arg0.getPath().toLowerCase().compareTo(arg1.getPath().toLowerCase());
        }
    };
    
    private static void listFiles(List<File> files, File rootDir, FilenameFilter filter) {
        File[] listing = rootDir.listFiles(filter);
        Arrays.sort(listing, filepathComparator);
        //depth first
        for(File file : listing) {
            if (file.isDirectory()) {
                //don't include directories, just child files
                listFiles(files, file, filter); 
            }
            if (file.isFile()) {
                files.add(file);
            }
        }
    }
    
    private void directoryDiff(File expectedDir, File jobResultDir) {
        //1) create a map of expected output files
        if (!expectedDir.isAbsolute()) {
            //it's relative to test.inputdir
            try {
                expectedDir = new File( testCase.getInputdir(), expectedDir.getPath() ).getCanonicalFile();
            }
            catch (IOException e) {
                Assert.fail("Error initializing expectedDir for '"+expectedDir.getPath()+"': "+e.getLocalizedMessage());
            }
        }
        List<File> expectedFiles = new ArrayList<File>();
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File arg0, String arg1) {
                if (arg1.equalsIgnoreCase(".svn")) {
                    return false;
                }
                return true;
            }
        };
        listFiles(expectedFiles, expectedDir, filter);
        Map<String,File> expectedFilesMap = new HashMap<String,File>();
        for(File file : expectedFiles) {
            expectedFilesMap.put(file.getName(), file);
        }
        //2) create a map of download result files for comparison
        List<File> resultFiles = new ArrayList<File>();
        listFiles(resultFiles, jobResultDir, filter);
        Map<String,File> resultFilesMap = new HashMap<String,File>();
        for(File file : resultFiles) {
            resultFilesMap.put(file.getName(), file);
        }
        
        //3) compare the expected output with the actual output ...
        //TODO: implement folder comparison, could use 'diff -rq unix command ...'            
        for(String filename : expectedFilesMap.keySet()) {
            File actual = resultFilesMap.remove(filename);
            if (actual == null) {
                Assert.fail("Expected result file not found: '"+filename+"'");
            }
            File expected = expectedFilesMap.get(filename);
            //diff(expected,actual);
            AbstractDiffTest diffTest = getDiff( (TestFileObj) null);
            diffTest.setJobId(""+jobNumber);
            diffTest.setInputDir(testCase.getInputdir());
            diffTest.setExpected(expected);
            diffTest.setActual(actual);
            diffTest.diff();
        }
        if (resultFilesMap.size() > 0) {
            Assert.fail("More job result files than expected: "+resultFilesMap.size());
        }
    }
    
    private AbstractDiffTest getDiff(TestFileObj resultFileObj) {
        
        List<String> customDiffCmdArgs = null;
        //for debugging
        Object customDiffCmdObj = null;
        GpAssertions assertions = testCase.getAssertions();

        //1) if there is a custom diff cmd for the individual result file 
        if (resultFileObj != null && resultFileObj.getDiffCmdArgs() != null) {
            customDiffCmdArgs = resultFileObj.getDiffCmdArgs();
            customDiffCmdObj = resultFileObj.getDiffCmd();
        }
        //2) else if there is a custom diff cmd for the test-case (all files)
        else if (assertions.getDiffCmdArgs() != null) {
            customDiffCmdArgs = assertions.getDiffCmdArgs();
            customDiffCmdObj = assertions.getDiffCmd();
        }
        //else default case
        else {
            AbstractDiffTest diffTest = new UnixCmdLineDiff();
            List<String> args = Collections.emptyList();
            diffTest.setArgs(args);
            return diffTest;
        }
        AbstractDiffTest customDiff = null;
        try {
            customDiff = initDiffTestFromCmdArgs(customDiffCmdArgs);
        }
        catch (Throwable t) {
            String message="Error initializing custom diff command, test: "+customDiffCmdObj+". Error: "+t.getLocalizedMessage();
            Assert.fail(message);
        }
        if (customDiff == null) {
            Assert.fail("Error initializing custom diff command, test: "+customDiffCmdObj);
        }
        return customDiff;
    }
    
    private AbstractDiffTest initDiffTestFromCmdArgs(List<String> args) throws Exception {
        if (args == null) {
            throw new IllegalArgumentException("diffCmd==null");
        }
        if (args.size() == 0) {
            throw new IllegalArgumentException("diffCmd.size()==0");
        }
        //the first arg must be a classname, for a class which can be cast to AbstractDiffTest
        //if the first arg is not a classname, use the CmdLineDiff class
        String classname = args.get(0);
        // use reflection 
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        
        Class<?> diffClass = null;
        try {
            diffClass = Class.forName(classname, false, classLoader);
        }
        catch (ClassNotFoundException e) {
            //assume that we want to use the CmdLineDiff class instead
            classname = CmdLineDiff.class.getName();
            //hack: add this to the arg list so that we deal with the arg list consistently
            args.add(0, classname);
        }
        try {
            diffClass = Class.forName(classname, false, classLoader);
        }
        catch (ClassNotFoundException e) {
            throw e;
        }
        if (!AbstractDiffTest.class.isAssignableFrom(diffClass)) {
            throw new Exception("diffCmd class cannot be cast to AbstractDiffTest, classname="+diffClass);
        }
        try {
            AbstractDiffTest customDiff = (AbstractDiffTest) diffClass.newInstance();
            List<String> extraArgs;
            if (args.size()>1) {
                extraArgs = args.subList(1, args.size());
            }
            else {
                extraArgs = Collections.emptyList();
            }
            customDiff.setArgs(extraArgs);
            return customDiff;
        }
        catch (InstantiationException e) {
            throw e;
        }
        catch (IllegalAccessException e) {
            throw e;
        }   
    }
}

