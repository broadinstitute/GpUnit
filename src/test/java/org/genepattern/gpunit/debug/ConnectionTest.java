package org.genepattern.gpunit.debug;

import static org.genepattern.gpunit.test.ConnectionTest.doConnectionTest;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.genepattern.gpunit.BatchProperties;
import org.genepattern.gpunit.GpUnitException;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Makes a REST API call to test the connection to the server.
 * Use this for testing and debugging.
 */
public class ConnectionTest {
    // TODO: set username/password before running this test
    @Ignore @Test
    public void cloud_gp_connectionTest() throws MalformedURLException, GpUnitException, URISyntaxException {
        final BatchProperties batchProps=ModuleTest.initGpClient("{test-user}:{test-password}", "https://cloud.genepattern.org/gp");
        doConnectionTest(batchProps);
    }

}
