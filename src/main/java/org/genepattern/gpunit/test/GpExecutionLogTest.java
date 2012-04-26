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
public class GpExecutionLogTest { 

    /**
     * This parameterized test runs a single unit test for each test case in the Collection of TestData.
     * Each Object[] element of the data array is passed as the arg to the constructor for a new testcase.
     */
    @Parameters
    public static Collection<Object[]> data() {
        return BatchModuleUtil.data(new File("./tests/saved_jobs"));
    }

    @BeforeClass 
    public static void beforeClass() {
        //System.out.println("beforeClass");
    }
    
    @AfterClass
    public static void afterClass() {
        //System.out.println("afterClass");
    }

    private File moduleTestFile;
    
    public GpExecutionLogTest(File moduleTestFile) {
        this.moduleTestFile = moduleTestFile;
    }
    
    @Test
    public void testModule() throws Exception {
        Util.runTest(moduleTestFile);
    }

}
