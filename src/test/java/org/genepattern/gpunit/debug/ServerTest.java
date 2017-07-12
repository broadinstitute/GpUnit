package org.genepattern.gpunit.debug;

import static org.genepattern.gpunit.test.ConnectionTest.doConnectionTest;

import java.net.URISyntaxException;

import org.genepattern.gpunit.BatchProperties;
import org.genepattern.gpunit.GpUnitException;
import org.junit.Ignore;
import org.junit.Test;

/**
 * for testing/debugging, make a REST API call to test the connection to the server
 */
public class ServerTest {
    // TODO: set username/password before running this test
    @Ignore @Test
    public void restApiConnection_gpprod() throws GpUnitException, URISyntaxException {
        final BatchProperties batchProps = new BatchProperties.Builder()
            .scheme("https")
            .host("genepattern.broadinstitute.org")
            .username("test")
            .password("****")
        .build();
        doConnectionTest(batchProps);
    }

    // TODO: set username/password before running this test
    @Ignore @Test
    public void restApiConnection_IU() throws GpUnitException, URISyntaxException {
        final BatchProperties batchProps = new BatchProperties.Builder()
            .scheme("https")
            .host("gp.indiana.edu")
            .username("broadtest")
            .password("******")
        .build();
        doConnectionTest(batchProps);
    }

}
