package org.genepattern.gpunit.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.genepattern.gpunit.yaml.Util;
import org.genepattern.util.junit.Parallelized;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;


/**
 * Run one unit test for each *test.yaml file in the 'test.dir' directory.
 * Files are searched recursively from the directory set by the 'test.dir' System property.
 * 
 * @author pcarr
 */
@RunWith(Parallelized.class)
public class BatchModuleTest { 

    /**
     * This parameterized test runs a single unit test for each test case in the Collection.
     */
    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        /*
        @see build.xml file for an example of how to configure the list of test-cases from ant,
        for example,
        <path id="gpunit.testcase.path">
            <pathelement location="./tests/protocols/01_Run" />
            <pathelement location="./tests/saved_jobs" />
            <pathelement location="./tests/protocols/02_Differential/03_CMSViewer/test.yaml">
        </path>
        <property name="gpunit.testcase.dirs" refid="gpunit.testcase.path" /> 
        */
        
        //for debugging ...
//        if (System.getProperty("gpunit.testcase.dirs") == null) { 
//        System.setProperty("gpunit.testcase.dirs",
//                // "/Broad/Projects/gp2-trunk/modules/util/gp-unit/tests/protocols/01_Run:/Broad/Projects/gp2-trunk/modules/util/gp-unit/tests/saved_jobs"
//                //"/Broad/Projects/gp2-trunk/modules/util/gp-unit/tests/protocols/01_Run:/Broad/Projects/gp2-trunk/modules/util/gp-unit/tests/saved_jobs:/Broad/Projects/gp2-trunk/modules/util/gp-unit/tests/protocols/02_Differential/03_CMSViewer/test.yaml"
//                //"/Broad/Projects/gp2-trunk/modules/util/gp-unit/tests/protocols/05_SNP"
//                "/Broad/Projects/gp2-trunk/modules/util/gp-unit/tests/protocols"
//                //"/Broad/Projects/gp2-trunk/modules/util/gp-unit/tests/protocols/05_SNP/02_XChromosomeCorrect/test.yaml"
//        );
//        }
        //for debugging ...
        String numThreadsProp = System.getProperty("junit.parallel.threads");
        if (numThreadsProp == null) {
            System.setProperty("junit.parallel.threads", "32");
        }
        String gpunitTestcaseDirsProp = System.getProperty("gpunit.testcase.dirs");
        if (gpunitTestcaseDirsProp != null) {
            System.out.println("gpunit.testcase.dirs: "+gpunitTestcaseDirsProp);
            
            //parse the list of one or more test cases
            List<File> fileset = new ArrayList<File>();
            String[] testCaseDirs = gpunitTestcaseDirsProp.split(Pattern.quote(File.pathSeparator));
            for(String testCaseDir : testCaseDirs) {
                fileset.add(new File(testCaseDir));
            }
            return BatchModuleUtil.data(fileset);
        }

        //TODO: change this back to more generic path
        //return BatchModuleUtil.data(new File("./tests/saved_jobs"));
        //return BatchModuleUtil.data(new File("./tests/protocols"));
        return BatchModuleUtil.data(new File("./tests/DiffTest"));
    }

    @BeforeClass 
    public static void beforeClass() {
        //Note: to change the gp server and user account you have two choices:
        //   1) launch from ant, see build.xml, and set these properties, or
        //   2) for debugging from an IDE, uncomment the following lines and set accordingly
        //System.setProperty("genePatternUrl", "http://genepattern.broadinstitute.org");
        System.setProperty("genePatternUrl", "http://genepatternbeta.broadinstitute.org");
        System.setProperty("username", "test");
        System.setProperty("password", "test"); 
        //System.setProperty("gpunit.deleteDownloadedResultFiles", "false");
    }
    
    @AfterClass
    public static void afterClass() {
    }

    //testname is the way to get the name for the test to show up in the junit report
    private String testname;
    private BatchModuleTestObject testObj;
    
    public BatchModuleTest(String testname, BatchModuleTestObject testObj) {
        this.testname = testname;
        this.testObj = testObj;
    }

    @Test
    public void runJobAndWait() throws Exception {
        Util.runTest(testObj);
    }

}
