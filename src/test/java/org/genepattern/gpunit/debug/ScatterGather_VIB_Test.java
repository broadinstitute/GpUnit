package org.genepattern.gpunit.debug;

import java.io.File;

import org.genepattern.gpunit.GpUnitException;
import org.junit.Ignore;
import org.junit.Test;

/**
 * for debugging the scatter-gather pipeline bug (circa GP 3.9.10) for VIB.
 */
public class ScatterGather_VIB_Test {
    @Ignore @Test
    public void scatter_gather_bug() throws GpUnitException {
        final File testFile=new File("tests/testGpUnit/scatter_gather/scatter_gather_bug.yaml").getAbsoluteFile();
        ModuleTest.doModuleTest("local-exec:local-exec", testFile);
    }
}
