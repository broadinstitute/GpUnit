package org.genepattern.gpunit.debug;

import java.io.File;

import org.genepattern.gpunit.GpUnitException;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Example single module test. 
 * Use this as a starting point for running GpUnit test directly from
 * your IDE.
 */
public class ExampleTest {
    
    @Ignore @Test
    public void protocols_01_run_01_preprocessdataset() throws GpUnitException {
        final File testFile=new File("tests/protocols/01_Run/01_PreprocessDataset/test.yaml");
        ModuleTest.doModuleTest(testFile);
    }

}
