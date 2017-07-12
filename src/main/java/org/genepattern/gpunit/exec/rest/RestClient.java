package org.genepattern.gpunit.exec.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.genepattern.gpunit.BatchProperties;
import org.genepattern.gpunit.GpUnitException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Generic REST API client
 * @author pcarr
 *
 */
public class RestClient {
    private final BatchProperties batchProps;
    //context path is hard-coded
    private String gpContextPath="gp";
    
    public RestClient(final BatchProperties batchProps) {
        this.batchProps=batchProps;
    }

    /**
     * 
     * @param relativePath, e.g. getJson('/rest/v1/config/gp-version')
     * @return
     * @throws URISyntaxException 
     * @throws GpUnitException 
     */
    public JsonObject getJson(final String relativePath) throws URISyntaxException, GpUnitException {
        String uriStr=batchProps.getGpUrl();
        if (!uriStr.endsWith("/")) {
            uriStr += "/";
        }
        uriStr += gpContextPath+relativePath;
        final URI uri=new URI(uriStr);
        final JsonObject json=readJsonObjectFromUri(uri);
        return json;
    }

    protected URL initEndpointUrl(String endpoint) throws GpUnitException
    {
        String gpUrl=batchProps.getGpUrl();
        if (!gpUrl.endsWith("/")) {
            gpUrl += "/";
        }
        gpUrl += gpContextPath+"/rest/v1"+endpoint;
        try {
            return new URL(gpUrl);
        }
        catch (MalformedURLException e) {
            throw new GpUnitException("Error initializing path to the " + endpoint + " endpoint", e);
        }
    }

    public HttpMessage setAuthHeaders(final HttpMessage message) {
        //for basic auth, use a header like this
        //Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==
        final String orig = batchProps.getGpUsername()+":"+batchProps.getGpPassword();
        //encoding  byte array into base 64
        byte[] encoded = Base64.encodeBase64(orig.getBytes());

        final String basicAuth="Basic "+new String(encoded);
        message.setHeader("Authorization", basicAuth);
        message.setHeader("Content-type", "application/json");
        message.setHeader("Accept", "application/json");
        
        return message;
    }

    public HttpGet setAuthHeaders(HttpGet get) {
        return (HttpGet) setAuthHeaders((HttpMessage)get);
    }

    public HttpPost setAuthHeaders(HttpPost post) {
        return (HttpPost) setAuthHeaders((HttpMessage)post);
    }
    
    public HttpDelete setAuthHeaders(HttpDelete delete) {
        return (HttpDelete) setAuthHeaders((HttpMessage)delete);
    }
    
    /**
     * GET the JSON representation of the contents at the given URI.
     * This is a general purpose helper method for working with the GenePattern REST API.
     * 
     * @param uri
     * @return
     * @throws GpUnitException
     */
    protected JsonObject readJsonObjectFromUri(final URI uri) throws GpUnitException {
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(uri);
        get = setAuthHeaders(get);
        
        final HttpResponse response;
        try {
            response=client.execute(get);
        }
        catch (ClientProtocolException e) {
            throw new GpUnitException("Error getting contents from uri="+uri, e);
        }
        catch (IOException e) {
            throw new GpUnitException("Error getting contents from uri="+uri, e);
        }
        final int statusCode=response.getStatusLine().getStatusCode();
        final boolean success;
        if (statusCode >= 200 && statusCode < 300) {
            success=true;
        }
        else {
            success=false;
        }
        if (!success) {
            // for debugging
            for(final Header header : response.getAllHeaders()) {
                String str=header.toString();
                System.out.println("    "+str);
            }
            String message="GET "+uri.toString()+" failed! "+statusCode+": "+response.getStatusLine().getReasonPhrase();
            throw new GpUnitException(message);
        }
        
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            final String message="GET "+uri.toString()+" failed! The response should contain an entity";
            throw new GpUnitException(message);
        }

        BufferedReader reader=null;
        try {
            reader=new BufferedReader(
                    new InputStreamReader( response.getEntity().getContent() )); 
            JsonObject jsonObject=readJsonObject(reader);
            return jsonObject;
        }
        catch (IOException e) {
            final String message="GET "+uri.toString()+", I/O error handling response";
            throw new GpUnitException(message, e);
        }
        catch (Exception e) {
            final String message="GET "+uri.toString()+", Error parsing JSON response";
            throw new GpUnitException(message, e);
        }
        catch (Throwable t) {
            final String message="GET "+uri.toString()+", Unexpected error reading response";
            throw new GpUnitException(message, t);
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e) {
                    final String message="GET "+uri.toString()+", I/O error closing reader";
                    throw new GpUnitException(message, e);
                }
            }
        }
    }

    /**
     * Helper class which creates a new JsonObject by parsing the contents from the
     * given Reader.
     * 
     * @param reader, an open and initialized reader, for example from an HTTP response.
     *     The calling method must close the reader.
     * @return
     * @throws GpUnitException
     */
    protected JsonObject readJsonObject(final Reader reader) throws GpUnitException {
        JsonParser parser = new JsonParser();
        JsonElement jsonElement=parser.parse(reader);
        if (jsonElement == null) {
            throw new GpUnitException("JsonParser returned null JsonElement");
        }
        return jsonElement.getAsJsonObject();
    }

}
