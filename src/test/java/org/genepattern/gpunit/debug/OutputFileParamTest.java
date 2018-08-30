package org.genepattern.gpunit.debug;

import java.io.File;

import org.genepattern.gpunit.GpUnitException;
import org.junit.Ignore;
import org.junit.Test;

/**
 * for debugging a GpUnit bug,
 * when a text parameter happens to have the same value as the parent directory
 */
public class OutputFileParamTest {
    
    @Ignore @Test
    public void output_file_param_test() throws GpUnitException {
        final File testFile=new File("output_file.txt/output_file_param.yml").getAbsoluteFile();
        ModuleTest.doModuleTest("local:local", testFile);
    }

}
