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

import org.genepattern.gpunit.BatchModuleTestObject;
import org.genepattern.gpunit.BatchProperties;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.gpunit.exec.rest.RestClientUtil;
import org.genepattern.gpunit.exec.soap.SoapClientUtil;
import org.genepattern.util.junit.Parallelized;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
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
        
        initProperties();

        Collection<Object[]> testCases;
        String gpunitTestcaseDirsProp = System.getProperty(BatchProperties.PROP_TESTCASE_DIRS);
        if (gpunitTestcaseDirsProp != null) { 
            //parse the list of one or more test cases
            List<File> fileset = new ArrayList<File>();
            // File.pathSeparator returns ";" on Windows and ":" on Linux, but ant filesets have ";"
            // as the separator on both platforms unless you use the pathconvert task.  That causes ant
            // to transform the separator to match the value returned by this call. So in order for
            // this to work the fileset must have been transformed by pathconvert.
            String[] testCaseEntries = gpunitTestcaseDirsProp.split(Pattern.quote(File.pathSeparator));

            // NOTE: these entries may be directories or individual files
            for(String testCaseEntry : testCaseEntries) {
                fileset.add(new File(testCaseEntry));
            }
            testCases=BatchModuleUtil.data(fileset);
        }
        else {
            final String path="./tests/protocols"; 
            testCases=BatchModuleUtil.data(new File(path));
        }

        //validate the testCases, make sure we have a unique download dir for each test
        uniqueifyTestCaseNames(testCases);

        return testCases;
    }

    /**
     * Since we use the testcase name as the name of the folder for jobResults, we need to
     * ensure that all the names are unique. When running against large test sets, we hit cases
     * where we have tests with identical names in parallel folders so we "uniqueify" the names
     * to avoid name collision.
     */
    private static void uniqueifyTestCaseNames(Collection<Object[]> testCases) throws GpUnitException {
        Set<String> testNames=new HashSet<String>();
        for(Object[] row : testCases) {
            final BatchModuleTestObject batchTestObj=(BatchModuleTestObject) row[1];
            final String originalName=batchTestObj.getTestName();
            String uniqueName = originalName;
            for (int count = 2; !testNames.add(uniqueName); count++) {
                uniqueName = originalName + "_" + Integer.toString(count);
            }
            if (!originalName.equals(uniqueName)) {  // update the testcase name with the new unique name
               ModuleTestObject testCase = batchTestObj.getTestCase();
               if (null != testCase) {
                   testCase.setName(uniqueName);
               }
               else if (null == batchTestObj.getInitException()) {
                   // There is no valid testcase, but no Exception was generated during initialization
                   throw new GpUnitException("Error processing test names for file: testName="+originalName+", testFile="+batchTestObj.getTestFile().getAbsolutePath());
               }
               // Otherwise, this testcase already has an init exception (i.e., it might be a .yaml file that isn't a test),
               // so the test representing will "fail" anyway since it won't even execute.
           }
        }
    }

    /**
     * Initialize System Properties
     * 
     * In order to ensure that we ALWAYS propagate any properties that:
     *
     *  - are not known at compile time
     *  - and are not specified on the ant *command line*
     *  - and are not listed in gpunit.default.properties
     *
     * (i.e., dynamic substitution properties used in yaml that are initialized in
     * gpunit.properties), we need to enumerate all properties specified in gpunit.properties
     * and/or gpunit.default.properties and add them to the System properties (if they're not
     * already there - we don't want to clobber any that were overridden on the command line
     * or in a custom properties file). This ensures that that they'll be picked up when queried
     * through BatchProperties.
    */
    private static void initProperties() throws GpUnitException {
        String defPropFileName = null;
        // Match what the ant script does and try to load gpunit.default.properties from the
        // directory containing the gp-unit project (project.basedir)
        String propBaseDirName = System.getProperty("project.basedir");
        if (propBaseDirName == null) {
            // "project.basedir" must be defined in the gpunit build.xml ant script
            defPropFileName = "gpunit.default.properties";
        }
        else {
            defPropFileName = propBaseDirName + File.separator + "gpunit.default.properties";
        }
        loadGpunitProperties(defPropFileName);

        // Now load any properties from the user-defined properties file that we haven't already seen
        String propFileName = System.getProperty("gpunit.properties");
        if (propFileName == null) {
            // must be defined in the gpunit build.xml ant script
            propFileName = "gpunit.properties";
        }
        loadGpunitProperties(propFileName);
    }


    private static boolean loadGpunitProperties(String fileName) {
        File gpunitPropsFile=new File(fileName);
        if (!gpunitPropsFile.canRead()) {
            return false;
        }

        // load props from the properties file
        try {
            Properties props=new Properties();
            props.load(new FileInputStream(gpunitPropsFile));
            for(final Entry<Object, Object> entry : props.entrySet()) {
                String key=(String)entry.getKey();
                String value = (String)entry.getValue();

                // special handling to account for the way test cases are declared, one way in the properties file and
                // another from ant...if the antfile has specified gpunit.testcase.dirs, that takes precedence over
                // gpunit.testfolder, so don't overwrite it
                if ("gpunit.testfolder".equals(key)) {
                    String testDirs = System.getProperty(BatchProperties.PROP_TESTCASE_DIRS);
                    String testFolder = System.getProperty("gpunit.testfolder");
                    if (null == testDirs) {
                        // this path will normally only be taken when the tests are not being run
                        // through the run-tests macro in build.xml, since it sets the gpunit.testdirs
                        // property
                        if (null == testFolder) {  // propagate the new value from the properties file
                            System.setProperty(BatchProperties.PROP_TESTCASE_DIRS, value);
                            System.setProperty("gpunit.testfolder", value);
                        }
                        else {  // take the existing testFolder value found in the environment
                            System.setProperty(BatchProperties.PROP_TESTCASE_DIRS, testFolder);
                        }
                    }
                }
                else if (!System.getProperties().containsKey(key)) {
                    System.setProperty(key, value);
                }
            }
        }
        catch (Exception e) {
            Assert.fail("Error loading properties from "+gpunitPropsFile+": "+e.getLocalizedMessage());
        }
        return true;
    }

    protected int getTestTimeout() {
        return 1000*batchProps.getTestTimeout();
    }
    
    @Rule
    public Timeout timeout = new Timeout(getTestTimeout());

    @BeforeClass 
    public static void beforeClass() throws GpUnitException {
        batchProps = BatchProperties.Factory.initFromProps();
    }
    
    @AfterClass
    public static void afterClass() throws GpUnitException {
        //if necessary, delete the batch output directory
        //only do this for directories created specifically for this batch run of 
        //    gp-unit tests
        if (batchProps != null && !batchProps.getSaveDownloads()) {
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
    public void gpunit() throws Exception {
        if (testObj.getInitException() != null) {
            Assert.fail(testObj.getInitException().getLocalizedMessage());
        }

        try {
            //submit a job via new REST API
            if (batchProps.getClient().equals(BatchProperties.GpUnitClient.REST)) {
                RestClientUtil.runTest(batchProps, testObj, null);
                return;
            }

            SoapClientUtil.runTest(batchProps,testObj);
        }
        catch (Throwable t) {
            Assert.fail(t.getLocalizedMessage());
        }
    }

}
