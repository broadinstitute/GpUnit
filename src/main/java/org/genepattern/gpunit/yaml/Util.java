package org.genepattern.gpunit.yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.genepattern.client.GPClient;
import org.genepattern.gpunit.GpAssertions;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.gpunit.TestFileObj;
import org.genepattern.io.IOUtil;
import org.genepattern.matrix.Dataset;
import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.Parameter;
import org.junit.Assert;
import org.yaml.snakeyaml.Yaml;

public class Util {

    static public void runTest(File testFile) throws GpUnitException, FileNotFoundException, AssertionError {
        ModuleTestObject testCase = Util.parse(testFile);
        JobResult jobResult = runJobSoap(testCase);
        validateResults(testCase, jobResult);
    }

    //########### module test file parser ....
    static public ModuleTestObject parse(File testFile) throws GpUnitException, FileNotFoundException {
        ModuleTestObject test = null;
        InputStream is = null;
        try {
            is = new FileInputStream(testFile);
            test = Util.parse(is);
            test = initInputdir(testFile, test);
            return test;
        }
        catch (FileNotFoundException e) {
            throw e;
        }
    }
    
    private static ModuleTestObject initInputdir(final File testFile, final ModuleTestObject test) {
        if (test == null) {
            return test;
        }
        if (test.getInputdir() == null) {
            //by default, the inputdir is the parent dir of the testFile
            test.setInputdir( testFile.getParentFile().getAbsoluteFile() );
            return test;
        }

        //else, convert relative path to absolute path
        if (!test.getInputdir().isAbsolute()) {
            //relative paths are relative to the testFile's parent directory
            File nd = new File(testFile.getAbsoluteFile(), test.getInputdir().getPath()).getAbsoluteFile();
            test.setInputdir(nd);
        }
        return test;
    }

    static private ModuleTestObject parse(InputStream is) throws GpUnitException {
        try {
            Yaml yaml = new Yaml();
            ModuleTestObject obj = yaml.loadAs(is, ModuleTestObject.class);
            return obj;
        }
        catch (Throwable t) {
            throw new GpUnitException("Error parsing test file", t);
        }
    }
    
    // --> end ########### module test file parser

    
    // ############ module job runner ... 
    /**
     * Submit a job to the GP server via the SOAP client, 
     * wait for the job to complete,
     * and return the JobResult object.
     * 
     * @param test
     * @return
     */
    static public JobResult runJobSoap(ModuleTestObject test) {
        JobResult jobResult = null;
        String nameOrLsid = test.getModule();
        try {
            GPClient gpClient = initGpClient();
            Parameter[] params = initParams(test);
        
            jobResult = gpClient.runAnalysis(nameOrLsid, params);
            if (jobResult == null) {
                throw new Exception("jobResult==null");
            }
        }
        catch (Throwable t) {
            throw new AssertionError("Error submitting job ["+test.getName()+", nameOrLsid='"+nameOrLsid+"']: "+t.getLocalizedMessage());
        }
        return jobResult;
    }

    static private GPClient initGpClient() {
        final String gpUrl = "http://gpdev.broadinstitute.org";
        final String gpUsername = "jntest";
        final String gpPassword = "jntest";
        
        GPClient gpClient = null;
        try {
            gpClient = new GPClient(gpUrl, gpUsername, gpPassword);
        }
        catch (Throwable t) {
            throw new AssertionError("Error initializing gpClient for gpUrl='"+gpUrl+"': "+t.getLocalizedMessage());
        }
        return gpClient;
    }
    
    static private Parameter[] initParams(ModuleTestObject test) throws IOException {
        List<Parameter> params = new ArrayList<Parameter>();
        for(Entry<String,Object> entry : test.getParams().entrySet()) {
            Parameter param = initParam(test, entry);
            params.add(param);
        }
        
        //convert to an array
        return params.toArray(new Parameter[]{});
    }
    
    static private Parameter initParam(ModuleTestObject test, Entry<String,Object> paramEntry) throws IOException {
        String pName = paramEntry.getKey();
        Object pValue = paramEntry.getValue();
        
        if (pValue == null) {
            //convert to empty String
            pValue = "";
        }

        Parameter param = null;
        if (pValue instanceof File) {
            File inputFile = (File) pValue;
            if (!inputFile.isAbsolute()) {
                //it's relative to test.inputdir
                inputFile = new File( test.getInputdir(), inputFile.getPath() ).getCanonicalFile();
            }
            param = new Parameter( pName, inputFile );
        }
        else {
            param = new Parameter( pName, pValue.toString() );
        }
        return param;
    }

    // --> end ############ module job runner 
    
    // ############ module job validation ...
    private static void validateResults(ModuleTestObject testCase, JobResult jobResult) {
        Assert.assertNotNull("jobResult is null", jobResult);
        
        Assert.assertFalse("job #"+jobResult.getJobNumber()+" has stderr", jobResult.hasStandardError());
        
        GpAssertions assertions = testCase.getAssertions();
        if (assertions == null) {
            return;
        }
        
        Map<String,File> resultFilesMap = new HashMap<String,File>();
        //only download result files if necessary
        if (assertions.getOutputDir() != null 
                || assertions.getNumFiles() >= 0 
                || (assertions.getFiles() != null && assertions.getFiles().size() > 0)) 
        {
            try {
                File[] resultFiles = downloadResultFiles(jobResult);
                for(File file : resultFiles) {
                    resultFilesMap.put(file.getName(), file);
                }
            }
            catch (Throwable t) {
                Assert.fail("Error downloading result files: "+t.getLocalizedMessage());
            }
        }

        //compare result files
        if (assertions.getNumFiles() >= 0) {
            //Note: when numFiles < 0, it means don't run this assertion
            Assert.assertEquals("Number of result files", assertions.getNumFiles(), resultFilesMap.size());
        }
        if (assertions.getFiles() != null) {
            for(Entry<String,TestFileObj> entry : assertions.getFiles().entrySet()) {
                String filename = entry.getKey();
                Assert.assertTrue("Expecting result file named '"+filename+"'", resultFilesMap.containsKey(filename));
                TestFileObj testFileObj = entry.getValue();
                if (testFileObj != null) {
                    String diff = testFileObj.getDiff();
                    if (diff != null) {
                        Assert.fail("FileDiff test not implemented yet");
                    }
                    int numCols = testFileObj.getNumCols();
                    int numRows = testFileObj.getNumRows();
                    if (numCols >= 0 || numRows >= 0) {
                        File resultFile = resultFilesMap.get(filename);
                        Dataset dataset = null;
                        try {
                            dataset = IOUtil.readDataset(resultFile.getAbsolutePath());
                        }
                        catch (Throwable t) {
                            Assert.fail("Error reading dataset for file='"+resultFile.getAbsolutePath()+"': "+t.getLocalizedMessage());
                        }
                        Assert.assertNotNull(dataset);
                        if (numRows >= 0) {
                            Assert.assertEquals(numRows, dataset.getRowCount());
                        }
                        if (numCols >= 0) {
                            Assert.assertEquals(numCols, dataset.getColumnCount());
                        }
                    }
                }
            }
        }
        
        //compare output directory
        if (assertions.getOutputDir() != null) {
            File expectedOutputDir = assertions.getOutputDir();
            File downloadDir = getDownloadResultFileDir(jobResult);
            try {
                File[] downloadedResultFiles = downloadResultFiles(downloadDir, jobResult);
            }
            catch (Exception e) {
                e.printStackTrace();
                Assert.fail("Error downloading result files for job='"+jobResult.getJobNumber()+"': "+e.getLocalizedMessage());
            }
            directoryDiff(testCase, expectedOutputDir, downloadDir);
//
//            
//            
//            
//            //1) create a map of expected output files
//            File expectedDir = assertions.getOutputDir();
//            if (!expectedDir.isAbsolute()) {
//                //it's relative to test.inputdir
//                try {
//                    expectedDir = new File( testCase.getInputdir(), expectedDir.getPath() ).getCanonicalFile();
//                }
//                catch (IOException e) {
//                    Assert.fail("Error initializing expectedDir for '"+assertions.getOutputDir().getPath()+"': "+e.getLocalizedMessage());
//                }
//            }
//            Map<String,File> expectedFiles = new HashMap<String,File>();
//            //define a filter of files to ignore
//            File[] expectedFiles = expectedDir.l
//            for(File expectedFile : expectedDir.listFiles()) {
//                expectedFiles.put(expectedFile.getName(), expectedFile);
//            }
//
//            //2) make a copy of the downloaded result files map for comparison
//            Map<String,File> resultFilesCopy = new HashMap<String,File>();
//            for(Entry<String,File> entry : resultFilesMap.entrySet()) {
//                resultFilesCopy.put(entry.getKey(), entry.getValue());
//            }
//            
//            //TODO: implement folder comparison, could use 'diff -rq unix command ...'            
//            for(String filename : expectedFiles.keySet()) {
//                File actual = resultFilesCopy.remove(filename);
//                if (actual == null) {
//                    Assert.fail("Expected result file not found: '"+filename+"'");
//                }
//                File expected = expectedFiles.get(filename);
//                if (hasDiff(expected,actual)) {
//                    Assert.fail("Result file differs from expected: '"+filename+"'");
//                }
//            }
//            if (resultFilesCopy.size() > 0) {
//                Assert.fail("More job result files than expected: "+resultFilesCopy.size());
//            }
        }
    }

    static File rootDownloadDir = new File("./tmp/jobResults");
    private static File getDownloadResultFileDir(JobResult jobResult) {
        if (jobResult == null) {
            throw new IllegalArgumentException("jobResult==null");
        }
        if (jobResult.getJobNumber() < 0) {
            throw new IllegalArgumentException("jobNumber=="+jobResult.getJobNumber());
        }
        //hard-coded path to root download dir
        File downloadDir = new File(rootDownloadDir, ""+jobResult.getJobNumber());
        return downloadDir;
    }
    
    private static File[] downloadResultFiles(JobResult jobResult) throws Exception {
        File downloadDir = getDownloadResultFileDir(jobResult);
        return downloadResultFiles(downloadDir, jobResult);
    }
    private static File[] downloadResultFiles(File downloadDir, JobResult jobResult) throws Exception {
        downloadDir = downloadDir.getCanonicalFile();
        if (!downloadDir.exists()) {
            boolean success = downloadDir.mkdirs();
            if (!success) {
                throw new Exception("Unable to create local download directory for jobNumber="+jobResult.getJobNumber()+", downloadDir="+downloadDir.getAbsolutePath());
            }
        }
        //TODO: could be a lengthy operation, consider running in an interruptible thread
        File[] files = jobResult.downloadFiles(downloadDir.getAbsolutePath());
        return files;
    }

    private static boolean deleteDownloadedResultFiles(JobResult jobResult) {
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
    
    private static void directoryDiff(ModuleTestObject testCase, File expectedOutputDir, JobResult jobResult) throws Exception {
        File downloadDir = getDownloadResultFileDir(jobResult);
        File[] downloadedResultFiles = downloadResultFiles(downloadDir, jobResult);
        directoryDiff(testCase, expectedOutputDir, downloadDir);
    }

    private static void directoryDiff(ModuleTestObject testCase, File expectedDir, File jobResultDir) {
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
            if (hasDiff(expected,actual)) {
                Assert.fail("Result file differs from expected: '"+filename+"'");
            }
        }
        if (resultFilesMap.size() > 0) {
            Assert.fail("More job result files than expected: "+resultFilesMap.size());
        }
    }

    private static boolean hasDiff(File expected, File actual) {
        //TODO: implement diff
        return false;
    }
    // --> end ############ module job validation
    
    static public void main(String[] args) {
        ModuleTestObject test = null;
        //String testFile = "example_test.yaml";
        //String testFilepath = "test_cle_gpurl.yaml";
        //String testFilepath = "./tests/RemoveDuplicatesGct/first_dupe.test/default_test.yaml";
        //String testFilepath = "./tests/RemoveDuplicatesGct/first_dupe.test/verbose_test.yaml";
        String testFilepath = "./tests/protocols/step1/test.yaml";
        
        if (args.length > 0) {
            //first arg is an optional test file
            testFilepath = args[0];
        }
        File testFile = new File(testFilepath);
        
        InputStream is = null;
        try {
            is = new FileInputStream(testFile);
            test = Util.parse(is);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (GpUnitException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        System.out.println("test.name="+test.getName());

        try {
            System.out.println("starting test "+testFile+" ... ");
            runTest(testFile);
            System.out.println("Success!");
        }
        catch (Throwable t) {
            System.err.println("Failure!");
            t.printStackTrace();
        }
    }
}
