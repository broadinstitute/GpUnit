package org.genepattern.gpunit.debug;

import java.io.File;
import java.net.MalformedURLException;

import org.genepattern.gpunit.BatchProperties;
import org.genepattern.gpunit.GpUnitException;
import org.junit.Ignore;
import org.junit.Test;

/**
 * for debugging, run ConvertLineEndings as a GpUnit test.
 * 
 * Use this class as an example starting point to create a one-off
 * test that you can run from your debugger, when testing
 * GpUnit integration issues.
 */
public class CleTest {
    
    protected static BatchProperties initGpClient_cloud() throws GpUnitException {
        return ModuleTest.initGpClient("test:test", "https://cloud.genepattern.org/gp");        
    }

    protected static BatchProperties initGpClient_gpprod() throws GpUnitException {
        return ModuleTest.initGpClient("test:test", "https://genepattern.broadinstitute.org/gp");        
    }
    
    protected static BatchProperties initClientIu() throws GpUnitException {
        return ModuleTest.initGpClient("broadtest:citest", "https://gp.indiana.edu/gp");        
    }

    @Ignore @Test
    public void cle_upload_local() throws GpUnitException {
        final File testFile=new File("src/test/data/cle_upload.yaml");
        ModuleTest.doModuleTest(testFile);
    }

    @Ignore @Test
    public void cle_upload_cloud_gp() throws GpUnitException {
        final File testFile=new File("src/test/data/cle_upload.yaml");
        ModuleTest.doModuleTest("test:test", "https://cloud.genepattern.org/gp", testFile);
    }

    @Ignore @Test
    public void cle_upload_beta_gp() throws GpUnitException {
        final File testFile=new File("src/test/data/cle_upload.yaml");
        ModuleTest.doModuleTest("test:test", "https://beta.genepattern.org/gp", testFile);
    }

    @Ignore @Test
    public void cle_upload_gpprod() throws GpUnitException {
        final File testFile=new File("src/test/data/cle_upload.yaml");
        ModuleTest.doModuleTest("test:test", "https://genepattern.broadinstitute.org/gp", testFile);
    }

    @Ignore @Test
    public void cle_upload_iu() throws GpUnitException {
        final File testFile=new File("src/test/data/cle_upload.yaml");
        ModuleTest.doModuleTest("broadtest:citest", "https://gp.indiana.edu/gp", testFile);
    }

}
