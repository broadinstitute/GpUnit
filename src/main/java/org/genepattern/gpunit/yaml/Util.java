package org.genepattern.gpunit.yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
        Assert.assertNotNull("jobResult is null", jobResult);
        
        Assert.assertFalse("job #"+jobResult.getJobNumber()+" has stderr", jobResult.hasStandardError());
        
        GpAssertions assertions = testCase.getAssertions();
        if (assertions == null) {
            return;
        }
        
        Map<String,File> resultFiles = new HashMap<String,File>();
        //only download result files if necessary
        if (assertions.getOutputDir() != null 
                || assertions.getNumFiles() >= 0 
                || (assertions.getFiles() != null && assertions.getFiles().size() > 0)) 
        {
            try {
                File[] files = jobResult.downloadFiles(".");
                for(File file : files) {
                    resultFiles.put(file.getName(), file);
                }
            }
            catch (Throwable t) {
                Assert.fail("Error downloading result files: "+t.getLocalizedMessage());
            }
        }

        //compare result files
        if (assertions.getNumFiles() >= 0) {
            //Note: when numFiles < 0, it means don't run this assertion
            Assert.assertEquals("Number of result files", assertions.getNumFiles(), resultFiles.size());
        }
        if (assertions.getFiles() != null) {
            for(Entry<String,TestFileObj> entry : assertions.getFiles().entrySet()) {
                String filename = entry.getKey();
                Assert.assertTrue("Expecting result file named '"+filename+"'", resultFiles.containsKey(filename));
                TestFileObj testFileObj = entry.getValue();
                if (testFileObj != null) {
                    String diff = testFileObj.getDiff();
                    if (diff != null) {
                        throw new GpUnitException("FileDiff test not implemented yet");
                    }
                    int numCols = testFileObj.getNumCols();
                    int numRows = testFileObj.getNumRows();
                    if (numCols >= 0 || numRows >= 0) {
                        File resultFile = resultFiles.get(filename);
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
            //1) create a map of expected output files
            File expectedDir = assertions.getOutputDir();
            if (!expectedDir.isAbsolute()) {
                //it's relative to test.inputdir
                try {
                    expectedDir = new File( testCase.getInputdir(), expectedDir.getPath() ).getCanonicalFile();
                }
                catch (IOException e) {
                    Assert.fail("Error initializing expectedDir for '"+assertions.getOutputDir().getPath()+"': "+e.getLocalizedMessage());
                }
            }
            Map<String,File> expectedFiles = new HashMap<String,File>();
            for(File expectedFile : expectedDir.listFiles()) {
                expectedFiles.put(expectedFile.getName(), expectedFile);
            }

            //2) make a copy of the downloaded result files map for comparison
            Map<String,File> resultFilesCopy = new HashMap<String,File>();
            for(Entry<String,File> entry : resultFiles.entrySet()) {
                resultFilesCopy.put(entry.getKey(), entry.getValue());
            }
            
            //TODO: implement folder comparison, could use 'diff -rq unix command ...'            
            for(String filename : expectedFiles.keySet()) {
                File actual = resultFilesCopy.remove(filename);
                if (actual == null) {
                    Assert.fail("Expected result file not found: '"+filename+"'");
                }
                File expected = expectedFiles.get(filename);
                if (hasDiff(expected,actual)) {
                    Assert.fail("Result file differs from expected: '"+filename+"'");
                }
            }
            if (resultFilesCopy.size() > 0) {
                Assert.fail("More job result files than expected: "+resultFilesCopy.size());
            }
        }
    }
    
    private static boolean hasDiff(File expected, File actual) {
        //TODO: implement diff
        return false;
    }
    
    static public GPClient initGpClient() {
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
    
    static public Parameter[] initParams(ModuleTestObject test) throws IOException {
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

    static public ModuleTestObject parse(InputStream is) throws GpUnitException {
        try {
            Yaml yaml = new Yaml();
            ModuleTestObject obj = yaml.loadAs(is, ModuleTestObject.class);
            return obj;
        }
        catch (Throwable t) {
            throw new GpUnitException("Error parsing test file", t);
        }
    }
    
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
