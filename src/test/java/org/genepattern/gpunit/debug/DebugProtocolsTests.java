package org.genepattern.gpunit.debug;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.genepattern.gpunit.BatchProperties;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.test.BatchModuleUtil;
import org.genepattern.util.junit.Parallelized;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;


/**
 * Run all of the protocols tests as a suite of tests, one for each yaml file.
 * Use this for debugging GpUnit from an IDE. Created as a copy of BatchModuleTest.
 */
@Ignore @RunWith(Parallelized.class)
public class DebugProtocolsTests { 

    /**
     * Generate the list of tests to run. Each entry references a test-case file in yaml format.
     * E.g.
     *   new Object[] { new File("path/to/test_name.yaml") }
     */
    @Parameters(name="{0}")
    public static Collection<Object[]> data() { 
        final File testDir=new File("tests/protocols");
        final List<File> files = BatchModuleUtil.findTestcases(testDir);
        
        final Collection<Object[]> testCases = new ArrayList<Object[]>();
        for(final File file : files) {
            Object[] next = new Object[] { file };
            testCases.add(next);
        }
        return testCases;
    }

    @BeforeClass
    public static void initGpClient() throws GpUnitException {
        //TODO: change this before running the tests
        gpClient=ModuleTest.initGpClient("username:password", "http://127.0.0.1:8080");
    }

    private static BatchProperties gpClient;
    
    @Parameter(0)
    public File testFile;
    
    @Ignore @Test
    public void gpunit() throws MalformedURLException, GpUnitException {
        ModuleTest.doModuleTest(gpClient, testFile);
    }

}
