package org.genepattern.gpunit.validator;

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
import org.genepattern.gpunit.diff.LocalDiffTest;
import org.genepattern.gpunit.diff.CmdLineDiff;
import org.genepattern.gpunit.diff.NumRowsColsDiff;
import org.genepattern.gpunit.diff.UnixCmdLineDiff;
import org.genepattern.gpunit.download.JobResultDownloader;
import org.genepattern.gpunit.BatchModuleTestObject;
import org.genepattern.gpunit.BatchProperties;
import org.junit.Assert;

/**
 * After the job completes on the server, run through the list of assertions.
 * 
 * This code was originally in a dedicated JobResultValidator class, which incorporated
 * GP SOAP client calls.
 * 
 * It was re-factored into a more abstract class, so that it can be used with both the REST and SOAP
 * clients.
 * 
 * 
 * @author pcarr
 */
public abstract class JobResultValidatorGeneric {
    final protected static String NL = System.getProperty("line.separator");
    
    public static final class DiffUtil {
        public static LocalDiffTest initDiffTestFromCmdArgs(List<String> args) throws Exception {
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
            if (!LocalDiffTest.class.isAssignableFrom(diffClass)) {
                throw new Exception("diffCmd class cannot be cast to LocalDiffTest, classname="+diffClass);
            }
            try {
                LocalDiffTest customDiff = (LocalDiffTest) diffClass.newInstance();
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

        public static Object getDiffCmdObj(final ModuleTestObject testCase, final TestFileObj resultFileObj) {
            //for debugging
            Object customDiffCmdObj = null;
            final GpAssertions assertions = testCase.getAssertions();

            //1) if there is a custom diff cmd for the individual result file 
            if (resultFileObj != null && resultFileObj.getDiffCmdArgs() != null) {
                //customDiffCmdArgs = resultFileObj.getDiffCmdArgs();
                customDiffCmdObj = resultFileObj.getDiffCmd();
            }
            //2) else if there is a custom diff cmd for the test-case (all files)
            else if (assertions.getDiffCmdArgs() != null) {
                //customDiffCmdArgs = assertions.getDiffCmdArgs();
                customDiffCmdObj = assertions.getDiffCmd();
            }
            return customDiffCmdObj;
        }

        public static LocalDiffTest getDiff(final ModuleTestObject testCase, final TestFileObj resultFileObj) throws Exception {
            List<String> customDiffCmdArgs = null;
            GpAssertions assertions = testCase.getAssertions();

            //1) if there is a custom diff cmd for the individual result file 
            if (resultFileObj != null && resultFileObj.getDiffCmdArgs() != null) {
                customDiffCmdArgs = resultFileObj.getDiffCmdArgs();
            }
            //2) else if there is a custom diff cmd for the test-case (all files)
            else if (assertions.getDiffCmdArgs() != null) {
                customDiffCmdArgs = assertions.getDiffCmdArgs();
            }
            //else default case
            else {
                LocalDiffTest diffTest = new UnixCmdLineDiff();
                List<String> args = Collections.emptyList();
                diffTest.setArgs(args);
                return diffTest;
            }
            return initDiffTestFromCmdArgs(customDiffCmdArgs);
        } 
    }

    final private BatchProperties props;
    final private ModuleTestObject testCase;
    private String jobId="";
    final private File downloadDir;
    
    public JobResultValidatorGeneric(
            final BatchProperties props, 
            final BatchModuleTestObject batchTestObject, 
            final File downloadDir
    ) {
        if (props==null) {
            throw new IllegalArgumentException("batchProps==null");
        }
        this.props=props;
        if (batchTestObject==null) {
            throw new IllegalArgumentException("batchTestObject==null");
        }
        if (downloadDir==null) {
            throw new IllegalArgumentException("downloadDir==null");
        }
        this.testCase = batchTestObject.getTestCase();
        if (this.testCase==null) {
            throw new IllegalArgumentException("testCase==null");
        }
        this.downloadDir = downloadDir;
    }

    public BatchProperties getBatchProperties() {
        return props;
    }
    public File getDownloadDir() {
        return downloadDir;
    }
    public void setJobId(final String jobId) {
        this.jobId=jobId;
    }
    public String getJobId() {
        return jobId;
    }
    public ModuleTestObject getTestCase() {
        return this.testCase;
    }
    abstract public void checkInit();
    abstract public boolean hasStdError();
    abstract public JobResultDownloader getDownloader();
    abstract public void deleteJob() throws GpUnitException;

    // Allow subclasses to override this in order to add a supplementary message (see JobValidatorRest)
    protected String createErrorMessage(String sourceMessage) {
        return sourceMessage;
    }

    /**
     * Read the error message by downloading 'stderr.txt' result file and returning 
     * a String containing the first MAX_N lines of the file.
     * 
     * @return
     */
    public String getErrorMessageFromStderrFile() {
        String errorMessage="";
        File stderrFile = null;
        try {
            stderrFile=getDownloader().getResultFile("stderr.txt");
            errorMessage = getErrorMessageFromStderrFile(stderrFile);
        }
        catch (Throwable t) {
            errorMessage = "There was an error downloading 'stderr.txt': "+t.getLocalizedMessage();
        }
        return errorMessage;
    }

    /**
     * Read the error message by downloading 'stderr.txt' result file and returning 
     * a String containing the first MAX_N lines of the file.
     * 
     * @return
     */
    static public String getErrorMessageFromStderrFile(final File stderrFile) {
        String errorMessage="";
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

    // TODO: Why does this method not use it's only argument ??
    private void downloadResultFiles(File toDir) throws GpUnitException {
        getDownloader().downloadResultFiles();
    }

    private int getNumResultFiles() {
        return getDownloader().getNumResultFiles();
    }

    public boolean hasResultFile(String relativePath) {
        return getDownloader().hasResultFile(relativePath);
    }

    public File getResultFile(File toDir, String relativePath) throws GpUnitException {
        return getDownloader().getResultFile(relativePath);
    }

    public void cleanDownloadedFiles() throws GpUnitException {
        getDownloader().cleanDownloadedFiles();
    }

    protected void validateJobStatus() {
        GpAssertions assertions = testCase.getAssertions();
        
        boolean expectedHasStdError = false;
        if (assertions != null && assertions.getJobStatus().trim().length() > 0) {
            //check to see if it's a test-case with an expected stderr.txt output
            expectedHasStdError = !"success".equalsIgnoreCase(assertions.getJobStatus());
        }
        
        //case 1: expecting stderr
        if (expectedHasStdError) {
            Assert.assertTrue(createErrorMessage("job #"+jobId+" doesn't have stderr.txt output"), hasStdError());
            return;
        }
        //case 2: unexpected stderr
        if (hasStdError() && !expectedHasStdError) {
            String junitMessage = "job #"+jobId+" has stderr.txt output: ";
            //try to download the error message
            String errorMessage = getErrorMessageFromStderrFile();
            junitMessage += NL + errorMessage;
            Assert.fail(createErrorMessage(junitMessage));
        }
    }
    
    
    /**
     * Validate any assertions the old fashioned way, using local diffs.
     */
    public void validate() {

        //1) initialization check
        checkInit();

        //2) job status
        validateJobStatus();

        //if necessary, download result files
        if (props.getSaveDownloads()) {
            try {
                downloadResultFiles(downloadDir);
            }
            catch (Exception e) {
                Assert.fail(createErrorMessage("job #"+jobId+", Error downloading result files: "+ e.getLocalizedMessage()));
            }
        }

        //3) numFiles: ...
        GpAssertions assertions = testCase.getAssertions();
        if (assertions.getNumFiles() >= 0) {
            //Note: when numFiles < 0, it means don't run this assertion
            Assert.assertEquals(createErrorMessage("job #"+jobId+", Number of result files"), assertions.getNumFiles(), getNumResultFiles());
        }

        //4) outputDir: ...
        final File expectedOutputDir=assertions.getOutputDir();
        if (expectedOutputDir != null) {
            try {
                downloadResultFiles(downloadDir);
            }
            catch (Throwable t) {
                t.printStackTrace();
                Assert.fail(createErrorMessage("job #"+jobId+", Error downloading result files: "+t.getLocalizedMessage()));
            }
            directoryDiff(expectedOutputDir, downloadDir);
        }

        //5) files: ... 
        //   (may or may not have already downloaded result files; assume that we haven't, because it doesn't make sense
        //    to include outputDir and files assertion in the same test)
        if (assertions.getFiles() != null) {
            for(Entry<String,TestFileObj> entry : assertions.getFiles().entrySet()) {
                String filename = entry.getKey();
                Assert.assertTrue(createErrorMessage("job #"+jobId+", Expecting result file named '"+filename+"'"), hasResultFile(filename));
                TestFileObj testFileObj = entry.getValue();
                if (testFileObj != null) {
                    //need to download the file ...
                    File actual = null;
                    try {
                        actual = getResultFile(downloadDir, filename);
                    }
                    catch (Throwable t) {
                        Assert.fail(createErrorMessage("job #"+jobId+", Error downloading result file '"+filename+"': "+t.getLocalizedMessage()));
                    }
                    String diff = testFileObj.getDiff();
                    if (diff != null) {
                        File expected = testCase.initFileFromPath(diff);
                        LocalDiffTest diffTest = getDiff(testFileObj);
                        diffTest.setJobId(""+jobId);
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
 
    public void clean() throws GpUnitException {
        final boolean saveDownloads=props.getSaveDownloads();
        final boolean deleteJobs=props.getDeleteJobs();
        if (!saveDownloads) {
            cleanDownloadedFiles();
        }
        if (deleteJobs) {
            deleteJob();
        }
    }

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
                Assert.fail(createErrorMessage("job #"+jobId+", Error initializing expectedDir for '"+expectedDir.getPath()+"': "+e.getLocalizedMessage()));
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
                Assert.fail(createErrorMessage("job #"+jobId+", Expected result file not found: '"+filename+"'"));
            }
            File expected = expectedFilesMap.get(filename);
            //diff(expected,actual);
            LocalDiffTest diffTest = getDiff( (TestFileObj) null);
            diffTest.setJobId(""+jobId);
            diffTest.setInputDir(testCase.getInputdir());
            diffTest.setExpected(expected);
            diffTest.setActual(actual);
            diffTest.diff();
        }
        if (resultFilesMap.size() > 0) {
            Assert.fail(createErrorMessage("job #"+jobId+", More job result files than expected: "+resultFilesMap.size()));
        }
    }

    private LocalDiffTest getDiff(final TestFileObj resultFileObj) {
      LocalDiffTest customDiff = null;
      try {
          customDiff=DiffUtil.getDiff(testCase, resultFileObj);
      }
      catch (Throwable t) {
          final Object customDiffCmdObj=DiffUtil.getDiffCmdObj(testCase, resultFileObj);
          String message="job #"+jobId+", Error initializing custom diff command, test: "+customDiffCmdObj+". Error: "+t.getLocalizedMessage();
          Assert.fail(createErrorMessage(message));
      }
      if (customDiff == null) {
          final Object customDiffCmdObj=DiffUtil.getDiffCmdObj(testCase, resultFileObj);
          Assert.fail(createErrorMessage("job #"+jobId+", Error initializing custom diff command, test: "+customDiffCmdObj));
      }
      return customDiff;
  }

}
