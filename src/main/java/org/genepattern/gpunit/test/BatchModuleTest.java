package org.genepattern.gpunit.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.genepattern.client.GPClient;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.yaml.ModuleRunner;
import org.genepattern.gpunit.yaml.Util;
import org.genepattern.util.junit.Parallelized;
import org.junit.AfterClass;
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
    static GPClient gpClient;
    static BatchProperties batchProps;

    final static private String PROP_TESTCASE_DIRS="gpunit.testcase.dirs";

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
        String numThreadsProp = System.getProperty("junit.parallel.threads");
        if (numThreadsProp == null) {
            System.setProperty("junit.parallel.threads", "32");
        }
        
        Collection<Object[]> testCases;

        String gpunitTestcaseDirsProp = System.getProperty(PROP_TESTCASE_DIRS);
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
            //final String path="./tests/testGpUnit"; 
            testCases=BatchModuleUtil.data(new File(path));
        }
        
        
        //validate the testCases, make sure we have a unique download dir for each test
        Set<String> testNames=new HashSet<String>();
        for(Object[] row : testCases) {
            final BatchModuleTestObject batchTestObj=(BatchModuleTestObject) row[2];
            final File testFile=batchTestObj.getTestFile();
            final String testName=Util.getTestNameFromFile(testFile);
            boolean added=testNames.add(testName);
            if (!added) {
                //error, duplicate testName
                throw new GpUnitException("Error duplicate testName, testName="+testName+", testFile="+batchTestObj.getTestFile().getAbsolutePath());
            }
        }
        return testCases;
    }

    @BeforeClass 
    public static void beforeClass() throws GpUnitException {
        //TODO: comment out all settings below this line, they are here for debugging
        //Note: to change the gp server and user account you have two choices:
        //   1) launch from ant, see build.xml, and set these properties, or
        //   2) for debugging from an IDE, set some system properties
        
        //final String gpUrl="http://genepattern.broadinstitute.org";
        //final String gpUrl="http://genepatternbeta.broadinstitute.org";
        //final String gpUrl="http://gpdev.broadinstitute.org";
        //final String gpUrl="http://127.0.0.1:8080";
        
        //System.setProperty("genePatternUrl", gpUrl);
        //System.setProperty("username", "jntest");
        //System.setProperty("password", "jntest"); 

        //System.setProperty(BatchProperties.PROP_OUTPUT_DIR, "./jobResults"); 
        //System.setProperty(BatchProperties.PROP_BATCH_NAME, "run-"+new Date().getTime()); 
        //System.setProperty(BatchProperties.PROP_SAVE_DOWNLOADS, "false"); 

        gpClient = ModuleRunner.initGpClient();
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

    private int batchIdx;
    //testname is the way to get the name for the test to show up in the junit report
    private String testname;
    private BatchModuleTestObject testObj;
    
    public BatchModuleTest(final int batchIdx, final String testname, final BatchModuleTestObject testObj) {
        this.batchIdx = batchIdx;
        this.testname = testname;
        this.testObj = testObj;
    }

    @Test
    public void runJobAndWait() throws Exception {
        Util.runTest(gpClient,batchProps,testObj);
    }

}
