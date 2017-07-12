package org.genepattern.gpunit.test;

import static org.junit.Assert.assertNotNull;

import java.net.URISyntaxException;

import org.genepattern.gpunit.BatchProperties;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.exec.rest.RestClient;
import org.junit.Test;

import com.google.gson.JsonObject;

/**
 * validate the connection to the server.
 * @author pcarr
 *
 */
public class ConnectionTest {

    public static void doConnectionTest(final BatchProperties batchProps) throws URISyntaxException, GpUnitException {
        RestClient restClient=new RestClient(batchProps);
        JsonObject json=restClient.getJson("/rest/v1/config/gp-version");
        assertNotNull("Expecting json response", json);
    }
    
    @Test
    public void restApiConnection_initFromProps() throws GpUnitException, URISyntaxException {
        final BatchProperties batchProps = BatchProperties.initFromProps();
        doConnectionTest(batchProps);
    }

}
