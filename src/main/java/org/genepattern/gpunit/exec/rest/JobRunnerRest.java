package org.genepattern.gpunit.exec.rest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.gpunit.test.BatchProperties;
import org.genepattern.gpunit.yaml.InputFileUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;


/**
 * Run a job on a GP server, using the REST API.
 * 
 * @author pcarr
 *
 */
public class JobRunnerRest {
    private BatchProperties batchProps;
    private ModuleTestObject test;
    private URL addFileUrl;
    private URL addJobUrl;
    private URL getTaskUrl;
    
    public JobRunnerRest(final BatchProperties batchProps, final ModuleTestObject test) throws GpUnitException {
        if (batchProps==null) {
            throw new IllegalArgumentException("batchProps==null");
        }
        this.batchProps=batchProps;
        if (test==null) {
            throw new IllegalArgumentException("test==null");
        }
        this.test=test;
        this.addFileUrl=initAddFileUrl();
        this.addJobUrl=initAddJobUrl();
        this.getTaskUrl=initGetTaskUrl();
    }
    
    
    private Map<String,URL> uploadFiles() throws IOException, GpUnitException {
        Set<String> inputFileParams=new HashSet<String>();
        //TODO: load the ParameterInfo so that we can handle file uploads and substitutions
        //TODO: implement this method, at the moment it is hard-coded and only works for PreprocessDataset
        if ("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00020:4".equals(test.getModule())) {
            inputFileParams.add("input.filename");
        }
        
        if (inputFileParams.size()==0) {
            return Collections.emptyMap();
        }
        
        Map<String,URL> inputfileMap=new HashMap<String,URL>();
        for(Entry<String,Object> paramEntry : test.getParams().entrySet()) {
            final String pname=paramEntry.getKey();
            //final ParameterInfo pinfo=null;
            if (inputFileParams.contains(pname)) {
                //it's an input file
                final Object initialValue=paramEntry.getValue();
                String updatedValue=InputFileUtil.getParamValueForInputFile(batchProps, test, initialValue);
                //upload the file here, TODO: make this an interruptible task
                URL url=uploadFileIfNecessary(updatedValue);
                if (url != null) {
                    inputfileMap.put(pname, url);
                }
            }
        }
        return inputfileMap;        
    }

    private URL initAddFileUrl() throws GpUnitException {
        String gpUrl=batchProps.getGpUrl();
        if (!gpUrl.endsWith("/")) {
            gpUrl += "/";
        }
        //TODO: context path is hard-coded
        gpUrl += "gp/";
        gpUrl += "rest/v1/data/upload/job_input";
        try {
            return new URL(gpUrl);
        }
        catch (MalformedURLException e) {
            throw new GpUnitException(e);
        }
    }

    private URL initAddJobUrl() throws GpUnitException {
        String gpUrl=batchProps.getGpUrl();
        if (!gpUrl.endsWith("/")) {
            gpUrl += "/";
        }
        //TODO: context path is hard-coded
        gpUrl += "gp/";
        gpUrl += "rest/v1/jobs";
        try {
            return new URL(gpUrl);
        }
        catch (MalformedURLException e) {
            throw new GpUnitException(e);
        }
    }
    
    private  URL initGetTaskUrl() throws GpUnitException {
        String gpUrl=batchProps.getGpUrl();
        if (!gpUrl.endsWith("/")) {
            gpUrl += "/";
        }
        //TODO: context path is hard-coded
        gpUrl += "gp/";
        gpUrl += "rest/v1/tasks";
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
    private JSONObject initJsonObject(final String lsid, final Map<String,URL> file_map) throws JSONException, IOException {
        JSONObject obj=new JSONObject();
        //String lsid = test.getModule();
        obj.put("lsid", lsid);
        JSONArray params=new JSONArray();
        for(Entry<String,Object> paramEntry : test.getParams().entrySet()) {
            final String pname=paramEntry.getKey();
            final String value;
            if (file_map.containsKey(pname)) {
                value=file_map.get(pname).toExternalForm();
            }
            else {
                //TODO: change method, this is getting the value for all input parameter types, including text
                value=InputFileUtil.getParamValueForInputFile(batchProps, test, paramEntry.getValue());
            }

            JSONObject paramObj=new JSONObject();
            paramObj.put("name", pname);
            JSONArray valuesArr=new JSONArray();
            valuesArr.put(value);
            paramObj.put("values", valuesArr);
            paramObj.put("values", valuesArr);
            params.put(paramObj);
        }
        obj.put("params", params);
        return obj;
    }
    
    private URL uploadFileIfNecessary(final String value) throws GpUnitException {
        try {
            URL url=new URL(value);
            return url;
        }
        catch (MalformedURLException e) {
            //expecting this
        }
        
        //make rest api call to gp server
        
        File localFile=new File(value);
        if (!localFile.exists()) {
            //file does not exist, must be a server file path
            return null;
        }
        
        return uploadFile(localFile);        
    }
    
    private HttpMessage setAuthHeaders(HttpMessage message) {
        //for basic auth, use a header like this
        //Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==
        String orig = batchProps.getGpUsername()+":"+batchProps.getGpPassword();
        //encoding  byte array into base 64
        byte[] encoded = Base64.encodeBase64(orig.getBytes());
        //System.out.println("Original String: " + orig );
        //System.out.println("Base64 Encoded String : " + new String(encoded));

        final String basicAuth="Basic "+new String(encoded);
        message.setHeader("Authorization", basicAuth);
        message.setHeader("Content-type", "application/json");
        message.setHeader("Accept", "application/json");
        
        return message;
    }

    private HttpGet setAuthHeaders(HttpGet get) {
        return (HttpGet) setAuthHeaders((HttpMessage)get);
    }

    private HttpPost setAuthHeaders(HttpPost post) {
        return (HttpPost) setAuthHeaders((HttpMessage)post);
    }
    
    private URL uploadFile(File localFile) throws GpUnitException {
        if (localFile==null) {
            throw new IllegalArgumentException("localFile==null");
        }
        if (!localFile.exists()) {
            throw new GpUnitException("File does not exist: "+localFile.getAbsolutePath());
        }
        if (localFile.isDirectory()) {
            throw new GpUnitException("File is a directory: "+localFile.getAbsolutePath());
        }
        
        HttpClient client = new DefaultHttpClient();
        
        String urlStr=addFileUrl.toExternalForm();
        final String encFilename;
        try {
            encFilename=URLEncoder.encode(localFile.getName(), "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new GpUnitException(e);
        }
        
        urlStr+="?name="+encFilename; 
        HttpPost post = new HttpPost(urlStr);
        post = setAuthHeaders(post);
        FileEntity entity = new FileEntity(localFile, "binary/octet-stream");
        post.setEntity(entity);
        HttpResponse response = null;
        try {
            response=client.execute(post);
        }
        catch (ClientProtocolException e) {
            throw new GpUnitException(e);
        }
        catch (IOException e) {
            throw new GpUnitException(e);
        }
        int statusCode=response.getStatusLine().getStatusCode();
        if (statusCode>=200 && statusCode <300) {
            Header[] locations=response.getHeaders("Location");
            if (locations != null && locations.length==1) {
                String location=locations[0].getValue();
                try {
                    return new URL(location);
                }
                catch (MalformedURLException e) {
                    throw new GpUnitException(e);
                }
            }
        }
        else {
            throw new GpUnitException("Error uploading file '"+localFile.getAbsolutePath()+"', "+
                    statusCode+": "+response.getStatusLine().getReasonPhrase());
        }
        throw new GpUnitException("Unexpected error uploading file '"+localFile.getAbsolutePath()+"'");
    }
    
    private String getTaskLsid(final String taskNameOrLsid) throws GpUnitException {
        try {
            JSONObject taskObj = getTask(taskNameOrLsid);
            JSONObject moduleObj=taskObj.getJSONObject("module");
            String lsid=moduleObj.getString("LSID");
            return lsid;
        }
        catch (JSONException e) {
            throw new GpUnitException("Error parsing JSON response for GET task, taskNameOrLsid="+taskNameOrLsid);
        }
    }
    
    private JSONObject getTask(final String taskNameOrLsid) throws GpUnitException {
        HttpClient client = new DefaultHttpClient();
        final String urlStr=getTaskUrl.toExternalForm()+"/"+taskNameOrLsid;
        
        HttpGet get = new HttpGet(urlStr);
        get = setAuthHeaders(get);
        HttpResponse response=null;
        try {
            response=client.execute(get);
        }
        catch (ClientProtocolException e) {
            throw new GpUnitException("Error executing HTTP request, GET "+urlStr, e);
        }
        catch (IOException e) {
            throw new GpUnitException("Error executing HTTP request, GET "+urlStr, e);
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
            String message="GET "+urlStr+" failed! "+statusCode+": "+response.getStatusLine().getReasonPhrase();
            throw new GpUnitException(message);
        }
        
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            //the response should contain an entity
            throw new GpUnitException("The response should contain an entity");
        }

        BufferedReader reader=null;
        try {
            reader=new BufferedReader(
                    new InputStreamReader( response.getEntity().getContent() ));
            JSONTokener jsonTokener=new JSONTokener(reader);
            JSONObject task=new JSONObject(jsonTokener);
            return task;
        }
        catch (IOException e) {
            throw new GpUnitException("Error getting HTTP content from GET "+urlStr, e);
        }
        catch (JSONException e) {
            throw new GpUnitException("Error parsing JSON response from GET "+urlStr, e);
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e) {
                    throw new GpUnitException("Unexpected exception thrown closing reader!");
                }
            }
        }
    }

    public URI submitJob() throws JSONException, UnsupportedEncodingException, IOException, Exception {
        // make REST call to validate that the module.lsid (which could be a taskName or LSID)
        // is installed on the server
        final String taskNameOrLsid = test.getModule();
        final String lsid=getTaskLsid(taskNameOrLsid);
        
        // upload data files, for each file input parameter, if it's a local file, upload it and save the URL
        // use that url as the value when adding the job to GP
        final Map<String,URL> file_map=uploadFiles();
        
        JSONObject job = initJsonObject(lsid, file_map);
        
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(addJobUrl.toExternalForm());
        post = setAuthHeaders(post);
        post.setEntity(new StringEntity(job.toString()));

        HttpResponse response = client.execute(post);
        final int statusCode=response.getStatusLine().getStatusCode();
        final boolean success;
        //when adding a job, expecting a status code of ...
        //   200, OK
        //   201, created
        //   202, accepted
        if (statusCode >= 200 && statusCode < 300) {
            success=true;
        }
        else {
            success=false;
        }
        if (!success) {
            String message="POST "+addJobUrl.toExternalForm()+" failed! "+statusCode+": "+response.getStatusLine().getReasonPhrase();
            throw new Exception(message);
        }
        
        String jobLocation=null;
        Header[] locations=response.getHeaders("Location");
        if (locations.length > 0) {
            jobLocation=locations[0].getValue();
        }
        if (jobLocation==null) {
            throw new Exception("Missing required response header: Location");
        }
        URI jobUri=new URI(jobLocation);
        return jobUri;
    }
    
    public JSONObject getJob(final URI jobUri) throws Exception {
        HttpClient client = new DefaultHttpClient();
        //String urlStr=getJobUrl.toExternalForm()+"/"+jobId;
        
        HttpGet get = new HttpGet(jobUri);
        get = setAuthHeaders(get);
        
        HttpResponse response=client.execute(get);
        final int statusCode=response.getStatusLine().getStatusCode();
        final boolean success;
        if (statusCode >= 200 && statusCode < 300) {
            success=true;
        }
        else {
            success=false;
        }
        if (!success) {
            String message="GET "+jobUri.toString()+" failed! "+statusCode+": "+response.getStatusLine().getReasonPhrase();
            //String message="GET "+urlStr+" failed! "+statusCode+": "+response.getStatusLine().getReasonPhrase();
            throw new Exception(message);
        }
        
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            //the response should contain an entity
            throw new Exception("The response should contain an entity");
        }

        BufferedReader reader=null;
        try {
            reader=new BufferedReader(
                    new InputStreamReader( response.getEntity().getContent() ));
            JSONTokener jsonTokener=new JSONTokener(reader);
            JSONObject job=new JSONObject(jsonTokener);
            return job;
        }
        finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
    
    public void downloadFile(final URL from, final File toFile) throws Exception {
        HttpClient client = new DefaultHttpClient();        
        HttpGet get = new HttpGet(from.toExternalForm());
        get = setAuthHeaders(get);
        //HACK: in order to by-pass the GP login page, and use Http Basic Authentication,
        //     need to spoof the 'IGV' client
        get.setHeader("User-Agent", "IGV");
        
        HttpResponse response=client.execute(get);
        final int statusCode=response.getStatusLine().getStatusCode();
        final boolean success;
        if (statusCode >= 200 && statusCode < 300) {
            success=true;
        }
        else {
            success=false;
        }
        
        if (!success) {
            String message="GET "+from.toString()+" failed! "+statusCode+": "+response.getStatusLine().getReasonPhrase();
            throw new Exception(message);
        }
        
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            //the response should contain an entity
            throw new Exception("The response should contain an entity");
        }
        
        InputStream in = response.getEntity().getContent();
        try {
            writeToFile(in, toFile, Long.MAX_VALUE);
        }
        finally {
            if (in != null) {
                in.close();
            }
        }
    }
    
    private void writeToFile( final InputStream in, final File toFile, final long maxNumBytes) 
    throws IOException
    //throws MaxFileSizeException, WriteToFileException
    {
        OutputStream out=null;
        try {
            long numBytesRead = 0L;
            int read = 0;
            byte[] bytes = new byte[1024];

            out = new FileOutputStream(toFile);
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
                numBytesRead += read;
                if (numBytesRead > maxNumBytes) {
                    //TODO: log.debug("maxNumBytes reached: "+maxNumBytes);
                    //throw new MaxFileSizeException("maxNumBytes reached: "+maxNumBytes);
                    break; 
                } 
            }
            out.flush();
            out.close();
        } 
        //catch (IOException e) {
            //log.error("Error writing to file: "+toFile.getAbsolutePath());
            //throw new WriteToFileException(e);
        //}
        finally {
            if (out != null) {
                try {
                    out.close();
                }
                catch (IOException e) {
                    //TODO:  log.error("Error closing output stream in finally clause", e);
                }
            }
        }
    }


}
