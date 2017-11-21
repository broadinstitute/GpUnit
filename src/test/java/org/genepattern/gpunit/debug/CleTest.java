package org.genepattern.gpunit.debug;

import java.io.File;

import org.genepattern.gpunit.BatchModuleTestObject;
import org.genepattern.gpunit.BatchProperties;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.BatchProperties.GpUnitClient;
import org.genepattern.gpunit.test.BatchModuleUtil;
import org.junit.Ignore;
import org.junit.Test;

/**
 * for debugging, run ConvertLineEndings as a GpUnit test.
 */
public class CleTest {

    /** run an example GpUnit test on the production server at IU. */
    @Ignore @Test
    public void cle_upload_IU() throws GpUnitException {
        final BatchProperties batchProps = new BatchProperties.Builder()
            .client(GpUnitClient.REST)
            .scheme("https")
            .host("gp.indiana.edu")
            //TODO: set valid username/password
            .username("broadtest")
            .password("******")
            // gpunit.delete.jobs=false
            .deleteJobs(false)
            .saveDownloads(true)
            .testTimeout(-1)
        .build();

        final File testFile=new File("src/test/data/cle_upload.yaml");
        final BatchModuleTestObject testObj=BatchModuleUtil.initBatchModuleTestObject(testFile);
        ModuleTest.doModuleTest(batchProps, testObj);
    }

}
