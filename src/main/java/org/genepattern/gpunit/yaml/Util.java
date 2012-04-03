package org.genepattern.gpunit.yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.genepattern.client.GPClient;
import org.genepattern.gpunit.GpUnitException;
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
        if (assertions.getNumFiles() >= 0) {
            //Note: numFiles < 0, it means don't run this assertion
            int actualNumFiles = -1;
            try {
                File[] files = jobResult.downloadFiles(".");
                for(File file : files) {
                    resultFiles.put(file.getName(), file);
                }
                if (files != null) {
                    actualNumFiles = files.length;
                }
            }
            catch (Throwable t) {
                Assert.fail("Error downloading result files: "+t.getLocalizedMessage());
            }
            Assert.assertEquals("Number of result files", assertions.getNumFiles(), actualNumFiles);
        }
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
                if (numCols >= 0 && numRows >= 0) {
                    File resultFile = resultFiles.get(filename);
                    Dataset dataset = null;
                    try {
                        dataset = IOUtil.readDataset(resultFile.getAbsolutePath());
                    }
                    catch (Throwable t) {
                        Assert.fail("Error reading dataset for file='"+resultFile.getAbsolutePath()+"': "+t.getLocalizedMessage());
                    }
                    Assert.assertNotNull(dataset);
                    Assert.assertEquals(numRows, dataset.getRowCount());
                    Assert.assertEquals(numCols, dataset.getColumnCount());
                }
            }
        }
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
    
    static public Parameter[] initParams(ModuleTestObject test) {
        List<Parameter> params = new ArrayList<Parameter>();
        for(Entry<String,String> entry : test.getParams().entrySet()) {
            Parameter param = new Parameter(
                    entry.getKey(),
                    entry.getValue());
            params.add(param);
        }
        //convert to an array
        return params.toArray(new Parameter[]{});
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
        String nameOrLsid = test.getModule();
        GPClient gpClient = initGpClient();
        Parameter[] params = initParams(test);
        
        JobResult jobResult = null;
        try {
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
        InputStream is = null;
        try {
            is = new FileInputStream(testFile);
            ModuleTestObject test = Util.parse(is);
            return test;
        }
        catch (FileNotFoundException e) {
            throw e;
        }
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
        String testFile = "test_cle_gpurl.yaml";
        InputStream is = null;
        try {
            is = new FileInputStream(new File(testFile));
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
            runTest(new File(testFile));
            System.out.println("Success!");
        }
        catch (Throwable t) {
            System.err.println("Failure!");
            t.printStackTrace();
        }
    }
}
