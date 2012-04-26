package org.genepattern.gpunit.test;

import java.io.File;
import java.util.Collection;

import org.genepattern.gpunit.yaml.Util;
import org.genepattern.util.junit.LabelledParameterized;
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
@RunWith(LabelledParameterized.class)
public class BatchModuleTest { 

    /**
     * This parameterized test runs a single unit test for each test case in the Collection of TestData.
     * Each Object[] element of the data array is passed as the arg to the constructor for a new testcase.
     */
    @Parameters
    public static Collection<Object[]> data() {
        //return BatchModuleUtil.data();
        //TODO: switch back to generic no-arg call to #data)
        return BatchModuleUtil.data(new File("./tests/saved_jobs"));
    }

    @BeforeClass 
    public static void beforeClass() {
        //Note: to change the gp server and user account you have two choices:
        //   1) launch from ant, see build.xml, and set these properties, or
        //   2) for debugging from an IDE, uncomment the following lines and set accordingly
        //System.setProperty("genePatternUrl", "http://genepatternbeta.broadinstitute.org");
        //System.setProperty("username", "jntest");
        //System.setProperty("password", "jntest");
    }
    
    @AfterClass
    public static void afterClass() {
    }

    private File moduleTestFile;
    
    public BatchModuleTest(File moduleTestFile) {
        this.moduleTestFile = moduleTestFile;
    }
    
    @Test
    public void testModule() throws Exception {
        Util.runTest(moduleTestFile);
    }

}