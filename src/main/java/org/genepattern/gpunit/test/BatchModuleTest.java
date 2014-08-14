package org.genepattern.gpunit.test;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.exec.rest.RestClientUtil;
import org.genepattern.gpunit.exec.soap.SoapClientUtil;
import org.genepattern.util.junit.Parallelized;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;


/**
 * This is the parallelized junit test for a batch of gp-unit test cases.
 * 
 * Run one unit test for each gp-unit test file in the given fileset.
 * 
 * See the example build.xml file for details on how to set the fileset.
 * 
 * @author pcarr
 */
@RunWith(Parallelized.class)
public class BatchModuleTest { 
    static BatchProperties batchProps;

    /**
     * This parameterized test runs a single unit test for each test case in the Collection.
     * Each Object[] instance in the collection is used to call the BatchModuleTest constructor,
     *     e.g. new BatchModuleTest(Object[0], Object[1], Object[2])
     *     
     * see the build.xml file for an example of how to configure the list of test-cases from ant,
     * for example,
     * <pre>
        <path id="gpunit.testcase.path">
            <pathelement location="./tests/protocols/01_Run" />
            <pathelement location="./tests/saved_jobs" />
            <pathelement location="./tests/protocols/02_Differential/03_CMSViewer/test.yaml">
        </path>
        <property name="gpunit.testcase.dirs" refid="gpunit.testcase.path" /> 
        </pre>
     *
     */
    @Parameters(name="{1}")
    public static Collection<Object[]> data() throws GpUnitException {
        final boolean debug=false;
        if (debug) {
            initDebug();
        }
        
        String numThreadsProp = System.getProperty("junit.parallel.threads");
        if (numThreadsProp == null) {
            System.setProperty("junit.parallel.threads", "32");
        }
        
        Collection<Object[]> testCases;

        String gpunitTestcaseDirsProp = System.getProperty(BatchProperties.PROP_TESTCASE_DIRS);
        if (gpunitTestcaseDirsProp != null) { 
            //parse the list of one or more test cases
            List<File> fileset = new ArrayList<File>();
            String[] testCaseDirs = gpunitTestcaseDirsProp.split(Pattern.quote(File.pathSeparator));
            for(String testCaseDir : testCaseDirs) {
                fileset.add(new File(testCaseDir));
            }
            testCases=BatchModuleUtil.data(fileset);
        }
        else {
            final String path="./tests/protocols"; 
            testCases=BatchModuleUtil.data(new File(path));
        }
        
        
        //validate the testCases, make sure we have a unique download dir for each test
        Set<String> testNames=new HashSet<String>();
        for(Object[] row : testCases) {
            final BatchModuleTestObject batchTestObj=(BatchModuleTestObject) row[1];
            final String testName=batchTestObj.getTestName();
            boolean added=testNames.add(testName);
            if (!added) {
                //error, duplicate testName
                throw new GpUnitException("Error duplicate testName, testName="+testName+", testFile="+batchTestObj.getTestFile().getAbsolutePath());
            }
        }
        return testCases;
    }

    private static void initDebug() {
        //load props from the properties file
        try {
            Properties props=new Properties();
            props.load(new FileInputStream(new File("gpunit.default.properties")));
            for(final Entry<Object, Object> entry : props.entrySet()) {
                System.setProperty((String)entry.getKey(), (String)entry.getValue());
            }
        }
        catch (Exception e) {
            Assert.fail(""+e.getLocalizedMessage());
        }
        
        
        // workaround for the way test cases are declared, one way in the properties file and another from ant
        System.setProperty(BatchProperties.PROP_TESTCASE_DIRS, System.getProperty("gpunit.testfolder", "./tests/protocols/01_Run"));

        // example custom properties for debugging
        //System.setProperty(BatchProperties.PROP_GP_URL, "http://genepatternbeta.broadinstitute.org");
        //System.setProperty(BatchProperties.PROP_GP_USERNAME, "test");
        //System.setProperty(BatchProperties.PROP_GP_PASSWORD, "test");
        //System.setProperty(BatchProperties.PROP_TESTCASE_DIRS, "./tests/testGpUnit/testStep");
        //System.setProperty(BatchProperties.PROP_CLIENT, BatchProperties.GpUnitClient.REST.toString());
        //System.setProperty(BatchProperties.PROP_SERVER_DIR, "http://genepatternbeta.broadinstitute.org/gp/data//xchip/sqa/Modules/TestSuiteData/");
        
        //System.setProperty(BatchProperties.PROP_SAVE_DOWNLOADS, "true");
        //System.setProperty(BatchProperties.PROP_DELETE_JOBS, "false");
        //System.setProperty(BatchProperties.PROP_OUTPUT_DIR, "./jobResults"); 
        //System.setProperty(BatchProperties.PROP_BATCH_NAME, "run-"+new Date().getTime());
    }

    @BeforeClass 
    public static void beforeClass() throws GpUnitException {
        batchProps = BatchProperties.Factory.initFromProps();
    }
    
    @AfterClass
    public static void afterClass() throws GpUnitException {
        //if necessary, delete the batch output directory
        //only do this for directories created specifically for this batch run of 
        //    gp-unit tests
        if (!batchProps.getSaveDownloads()) {
            if (batchProps.getCreatedBatchOutputDir()) {
                File batchOutputDir=batchProps.getBatchOutputDir();
                if (batchOutputDir != null) {
                    boolean success=batchOutputDir.delete();
                    if (!success) {
                        String message="failed to delete batch output directory: "+batchOutputDir.getAbsolutePath();
                        throw new GpUnitException(message);
                    }
                }
            }
        }
    }

    private BatchModuleTestObject testObj;
    
    public BatchModuleTest(final int batchIdx, final BatchModuleTestObject testObj) {
        this.testObj = testObj;
    }
    
    @Test
    public void runJobAndWait() throws Exception {
        try {
            //submit a job via new REST API
            if (batchProps.getClient().equals(BatchProperties.GpUnitClient.REST)) {
                RestClientUtil.runTest(batchProps, testObj);
                return;
            }

            SoapClientUtil.runTest(batchProps,testObj);
        }
        catch (Throwable t) {
            Assert.fail(t.getLocalizedMessage());
        }
    }

}
