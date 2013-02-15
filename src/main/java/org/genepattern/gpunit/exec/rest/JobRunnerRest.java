package org.genepattern.gpunit.exec.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map.Entry;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.gpunit.test.BatchProperties;
import org.genepattern.gpunit.yaml.InputFileUtil;
import org.genepattern.webservice.ParameterInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Run a job on a GP server, using the REST API.
 * 
 * @author pcarr
 *
 */
public class JobRunnerRest {
    private BatchProperties batchProps;
    private ModuleTestObject test;
    private URL url;
    
    public JobRunnerRest(final BatchProperties batchProps, final ModuleTestObject test) throws GpUnitException {
        if (batchProps==null) {
            throw new IllegalArgumentException("batchProps==null");
        }
        this.batchProps=batchProps;
        if (test==null) {
            throw new IllegalArgumentException("test==null");
        }
        this.test=test;
        this.url=initRestUrl();
    }
    
    
    public void runJobAndWait() {
        //TODO: REST call to add the job
        //TODO: REST call to poll for job completion
        //TODO: REST call to get the job results
    }

    private URL initRestUrl() throws GpUnitException {
        String gpUrl=batchProps.getGpUrl();
        if (!gpUrl.endsWith("/")) {
            gpUrl += "/";
        }
        //TODO: context path is hard-coded
        gpUrl += "gp/";
        gpUrl += "rest/jobs";
        try {
            return new URL(gpUrl);
        }
        catch (MalformedURLException e) {
            throw new GpUnitException(e);
        }
    }
    
    /**
     * Initialize the JSONObject to PUT into the /jobs resource on the GP server.
     * 
     * <pre>
       {"lsid":"urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00020:4", 
        "params": [
             {"name": "input.filename", 
              "values": 
                ["ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.gct"]
             }
         ]
       }
     * </pre>
     */
    private JSONObject initJsonObject() throws JSONException, IOException {
        JSONObject obj=new JSONObject();
        String lsid = test.getModule();
        obj.put("lsid", lsid);
        JSONArray params=new JSONArray();
        for(Entry<String,Object> paramEntry : test.getParams().entrySet()) {
            //TODO: load the ParameterInfo so that we can handle file uploads and substitutions
            final ParameterInfo pinfo=null;
            String value=InputFileUtil.getParamValue(batchProps, pinfo, test, paramEntry);
            //JSONArray arr=new JSONArray();
            JSONObject paramObj=new JSONObject();
            paramObj.put("name", paramEntry.getKey());
            JSONArray valuesArr=new JSONArray();
            valuesArr.put(value);
            paramObj.put("values", valuesArr);
            paramObj.put("values", valuesArr);
            params.put(paramObj);
        }
        obj.put("params", params);
        return obj;
    }
    
    public String submitJob() throws JSONException, UnsupportedEncodingException, IOException {
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(url.toExternalForm());
        
        //for basic auth, use a header like this
        //Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==
        String orig = batchProps.getGpUsername()+":"+batchProps.getGpPassword();
        //encoding  byte array into base 64
        byte[] encoded = Base64.encodeBase64(orig.getBytes());
        //System.out.println("Original String: " + orig );
        //System.out.println("Base64 Encoded String : " + new String(encoded));

        final String basicAuth="Basic "+new String(encoded);
        post.setHeader("Authorization", basicAuth);
        post.setHeader("Content-type", "application/json");
        post.setHeader("Accept", "application/json");
        
        JSONObject job = initJsonObject();
        post.setEntity(new StringEntity(job.toString()));

        HttpResponse response = client.execute(post);
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String line = "";
        while ((line = rd.readLine()) != null) {
            System.out.println(line);
        }
        return "-1";
    }

}
