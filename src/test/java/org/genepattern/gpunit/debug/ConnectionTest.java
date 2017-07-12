package org.genepattern.gpunit.debug;

import static org.junit.Assert.assertNotNull;

import java.net.URISyntaxException;

import org.genepattern.gpunit.BatchProperties;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.exec.rest.RestClient;
import org.junit.Ignore;
import org.junit.Test;

import com.google.gson.JsonObject;

/**
 * junit tests to validate the connection to the server.
 * @author pcarr
 *
 */
public class ConnectionTest {

    public void doConnectionTest(final BatchProperties batchProps) throws URISyntaxException, GpUnitException {
        RestClient restClient=new RestClient(batchProps);
        JsonObject json=restClient.getJson("/rest/v1/config/gp-version");
        assertNotNull("Expecting json response", json);
    }
    
    // TODO: start your local server before running this test
    @Ignore @Test
    public void restApiConnection_initFromProps() throws GpUnitException, URISyntaxException {
        final BatchProperties batchProps = BatchProperties.initFromProps();
        doConnectionTest(batchProps);
    }
    
    @Test
    public void restApiConnection_gpprod() throws GpUnitException, URISyntaxException {
        final BatchProperties batchProps = new BatchProperties.Builder()
            .scheme("https")
            .host("genepattern.broadinstitute.org")
            .username("test")
            .password("test")
        .build();
        doConnectionTest(batchProps);
    }

    // TODO: set username/password before running this test
    @Ignore @Test
    public void restApiConnection_IU() throws GpUnitException, URISyntaxException {
        final BatchProperties batchProps = new BatchProperties.Builder()
            .scheme("https")
            .host("gp.indiana.edu")
            .username("")
            .password("")
        .build();
        doConnectionTest(batchProps);
    }

}
