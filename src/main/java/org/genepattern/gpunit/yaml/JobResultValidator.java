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
import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.gpunit.TestFileObj;
import org.genepattern.gpunit.diff.AbstractDiffTest;
import org.genepattern.gpunit.diff.UnixCmdLineDiff;
import org.genepattern.io.IOUtil;
import org.genepattern.matrix.Dataset;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobResult;
import org.junit.Assert;

/**
 * After the job completes on the server, run through the list of assertions.
 * 
 * @author pcarr
 */
public class JobResultValidator {
    final static String NL = System.getProperty("line.separator");

    private ModuleTestObject test = null;
    private JobResult jobResult = null;
    private File rootDownloadDir = new File("./tmp/jobResults");    
    private File downloadDir = null;
    private File[] resultFiles = null;
    private List<String> outputFilenames = null;
    private Map<String,File> resultFilesMap = new HashMap<String,File>();
    
    public JobResultValidator(ModuleTestObject test, JobResult jobResult) {
        this.test = test;
        this.jobResult = jobResult;
        
        if (this.test == null) {
            throw new IllegalArgumentException("test==null");
        }
    }

    public void setRootDownloadDir(File file) {
        this.rootDownloadDir = file;
    }
    
    private void initDownloadDir() throws Exception {
        if (this.jobResult==null) {
            throw new IllegalArgumentException("jobResult==null");
        }
        if (jobResult.getJobNumber() < 0) {
            throw new IllegalArgumentException("jobNumber=="+jobResult.getJobNumber());
        }
        //1) init downloadDir, create a new dir, relative to the rootDownloadDir 
        this.downloadDir = new File(rootDownloadDir, ""+jobResult.getJobNumber());
        this.downloadDir = this.downloadDir.getCanonicalFile();
        if (!downloadDir.exists()) {
            boolean success = downloadDir.mkdirs();
            if (!success) {
                throw new Exception("Unable to create local download directory for jobNumber="+jobResult.getJobNumber()+", downloadDir="+downloadDir.getAbsolutePath());
            }
        }
    }
    
    private void downloadResultFiles() throws Exception {
        initDownloadDir();
        //TODO: could be a lengthy operation, consider running in an interruptible thread
        this.resultFiles = jobResult.downloadFiles(downloadDir.getAbsolutePath());
        
        this.resultFilesMap.clear();
        if (this.resultFiles != null) {
            for(File file : this.resultFiles) {
                this.resultFilesMap.put(file.getName(), file);
            }
        }
    }
    
    private File downloadResultFile(String filename) throws Exception {
        initDownloadDir();
        File file = jobResult.downloadFile(filename, this.downloadDir.getAbsolutePath());
        resultFilesMap.put(file.getName(), file);
        return file;
    }

    private void initOutputFilenames() {
        this.outputFilenames = new ArrayList<String>();
        for(String outputFilename : jobResult.getOutputFileNames() ) {
            outputFilenames.add( outputFilename );
        }
        if (jobResult.hasStandardOut()) {
            outputFilenames.add( GPConstants.STDOUT );
        }
        if (jobResult.hasStandardError()) {
            outputFilenames.add( GPConstants.STDERR );
        }
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
            stderrFile = downloadResultFile("stderr.txt");
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
        GpAssertions assertions = test.getAssertions();
        //boolean hasStandardError = jobResult.hasStandardError();
        
        boolean actualHasStdError = jobResult.hasStandardError();
        boolean expectedHasStdError = false;
        if (assertions != null && assertions.getJobStatus().trim().length() > 0) {
            //check to see if it's a test-case with an expected stderr.txt output
            expectedHasStdError = !"success".equalsIgnoreCase(assertions.getJobStatus());
        }
        
        //case 1: expecting stderr
        if (expectedHasStdError) {
            Assert.assertTrue("job #"+jobResult.getJobNumber()+" doesn't have stderr.txt output", actualHasStdError);
            return;
        }
        //case 2: unexpected stderr
        if (actualHasStdError && !expectedHasStdError) {
            //URL stderrLink = jobResult.getURLForFileName("stderr.txt");
            String junitMessage = "job #"+jobResult.getJobNumber()+" has stderr.txt output: ";
            //try to download the error message
            String errorMessage = getErrorMessageFromStderrFile();
            junitMessage += NL + errorMessage;
            Assert.fail(junitMessage);
        }
    }
    
    public void validate() {
        //1) null jobResult
        Assert.assertNotNull("jobResult is null", jobResult);
        
        //2) job status
        validateJobStatus();

        //3) numFiles: ...
        GpAssertions assertions = test.getAssertions();
        if (assertions.getNumFiles() >= 0) {
            initOutputFilenames();
            //Note: when numFiles < 0, it means don't run this assertion
            Assert.assertEquals("Number of result files", assertions.getNumFiles(), outputFilenames.size());
        }

        //4) outputDir: ...
        if (assertions.getOutputDir() != null) {
            File expectedOutputDir = assertions.getOutputDir();
            try {
                downloadResultFiles();
            }
            catch (Throwable t) {
                    t.printStackTrace();
                    Assert.fail("Error downloading result files for job='"+jobResult.getJobNumber()+"': "+t.getLocalizedMessage());
            }
            directoryDiff(expectedOutputDir, downloadDir);
        }
        
        //5) files: ... 
        //   (may or may not have already downloaded result files; assume that we haven't, because it doesn't make sense
        //    to include outputDir and files assertion in the same test)
        if (assertions.getFiles() != null) {
            if (outputFilenames == null) {
                initOutputFilenames();
            }
            for(Entry<String,TestFileObj> entry : assertions.getFiles().entrySet()) {
                String filename = entry.getKey();
                Assert.assertTrue("Expecting result file named '"+filename+"'", outputFilenames.contains(filename));
                TestFileObj testFileObj = entry.getValue();
                if (testFileObj != null) {
                    //need to download the file ...
                    File actual = null;
                    try {
                        actual = downloadResultFile(filename);
                    }
                    catch (Throwable t) {
                        t.printStackTrace();
                        Assert.fail("Error downloading result file '"+filename+"': "+t.getLocalizedMessage());
                    }
                    String diff = testFileObj.getDiff();
                    if (diff != null) {
                        File expected = test.initFileFromPath(diff);
                        //diff(expected,actual);
                        AbstractDiffTest diffTest = getDiff(testFileObj);
                        diffTest.setInputDir(test.getInputdir());
                        diffTest.setExpected(expected);
                        diffTest.setActual(actual);
                        diffTest.diff();
                    }
                    int numCols = testFileObj.getNumCols();
                    int numRows = testFileObj.getNumRows();
                    if (numCols >= 0 || numRows >= 0) {
                        Dataset dataset = null;
                        try {
                            dataset = IOUtil.readDataset(actual.getAbsolutePath());
                        }
                        catch (Throwable t) {
                            Assert.fail("Error reading dataset for file='"+actual.getAbsolutePath()+"': "+t.getLocalizedMessage());
                        }
                        Assert.assertNotNull(dataset);
                        if (numRows >= 0) {
                            Assert.assertEquals("'"+actual.getName()+"'[numRows]", numRows, dataset.getRowCount());
                        }
                        if (numCols >= 0) {
                            Assert.assertEquals("'"+actual.getName()+"'[numCols]", numCols, dataset.getColumnCount());
                        }
                    }
                }
            }
        }
    }

    public boolean deleteDownloadedResultFiles() {
        if (jobResult == null) {
            throw new IllegalArgumentException("jobResult==null");
        }
        if (jobResult.getJobNumber() < 0) {
            throw new IllegalArgumentException("jobNumber=="+jobResult.getJobNumber());
        }
        //hard-coded path to root download dir
        File downloadDir = new File(rootDownloadDir, ""+jobResult.getJobNumber());
        boolean success = deleteDir(downloadDir);        
        return success;
    }
    
    /**
     * Deletes all files and subdirectories under dir.
     * Returns true if all deletions were successful.
     * If a deletion fails, the method stops attempting to delete and returns false.
     * 
     * @param dir
     * @return
     */
    private static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            for(String child : dir.list()) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
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
                expectedDir = new File( test.getInputdir(), expectedDir.getPath() ).getCanonicalFile();
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
            diffTest.setInputDir(test.getInputdir());
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
        GpAssertions assertions = test.getAssertions();

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
    
    // return null to indicate an exception
    private AbstractDiffTest initDiffTestFromCmdArgs(List<String> args) throws ClassNotFoundException, Exception {
        if (args == null) {
            throw new IllegalArgumentException("diffCmd==null");
        }
        if (args.size() == 0) {
            throw new IllegalArgumentException("diffCmd.size()==0");
        }
        //the first arg must be a classname, for a class which can be cast to AbstractDiffTest
        String classname = args.get(0);
        // use reflection 
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Class<?> diffClass = Class.forName(classname, false, classLoader);
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
            return null;
        }
        catch (IllegalAccessException e) {
            return null;
        }   
    }
}

