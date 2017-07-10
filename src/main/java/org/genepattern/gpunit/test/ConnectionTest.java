package org.genepattern.gpunit.test;

import static org.junit.Assert.assertNotNull;

import java.net.URISyntaxException;

import org.genepattern.gpunit.BatchProperties;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.exec.rest.RestClient;
import org.junit.Test;

import com.google.gson.JsonObject;

/**
 * junit tests to validate the connection to the server.
 * @author pcarr
 *
 */
public class ConnectionTest {
    
    @Test
    public void restApiConnection() throws GpUnitException, URISyntaxException {
        final BatchProperties batchProps = BatchProperties.Builder.initFromProps();
        RestClient restClient=new RestClient(batchProps);
        JsonObject json=restClient.getJson("/rest/v1/config/gp-version");
        assertNotNull("Expecting json response", json);
    }

}
