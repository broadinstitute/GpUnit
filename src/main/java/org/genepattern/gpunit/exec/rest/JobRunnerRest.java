package org.genepattern.gpunit.exec.rest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
import org.genepattern.gpunit.exec.rest.json.JobResultObj;
import org.genepattern.gpunit.exec.rest.json.TaskObj;
import org.genepattern.gpunit.test.BatchProperties;
import org.genepattern.gpunit.yaml.InputFileUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Run a job on a GP server, using the REST API.
 * 
 * @author pcarr
 *
 */
public class JobRunnerRest {
    //context path is hard-coded
    private String gpContextPath="gp";
    private BatchProperties batchProps;
    private ModuleTestObject test;
    private URL addFileUrl;
    private URL addJobUrl;
    private URL getTaskUrl;

    public JobRunnerRest(final BatchProperties batchProps, final ModuleTestObject test) throws GpUnitException 
    {
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

    private URL initAddFileUrl() throws GpUnitException 
    {
        String gpUrl=batchProps.getGpUrl();
        if (!gpUrl.endsWith("/")) {
            gpUrl += "/";
        }
        gpUrl += gpContextPath+"/rest/v1/data/upload/job_input";
        try {
            return new URL(gpUrl);
        }
        catch (MalformedURLException e) {
            throw new GpUnitException("Error initializing path to the 'job_input' endpoint", e);
        }
    }

    private URL initAddJobUrl() throws GpUnitException {
        String gpUrl=batchProps.getGpUrl();
        if (!gpUrl.endsWith("/")) {
            gpUrl += "/";
        }
        gpUrl += gpContextPath+"/rest/v1/jobs";
        try {
            return new URL(gpUrl);
        }
        catch (MalformedURLException e) {
            throw new GpUnitException("Error initializing path to the 'jobs' endpoint", e);
        }
    }
    
    private  URL initGetTaskUrl() throws GpUnitException {
        String gpUrl=batchProps.getGpUrl();
        if (!gpUrl.endsWith("/")) {
            gpUrl += "/";
        }
        gpUrl += gpContextPath+"/rest/v1/tasks";
        try {
            return new URL(gpUrl);
        }
        catch (MalformedURLException e) {
            throw new GpUnitException("Error initializing path to the 'tasks' endpoint", e);
        }
    }
    
    /**
     * Prepare the (list of) input file(s) from the given yamlValue. 
     * If necessary upload each input file to the server. This method blocks while files are being transferred.
     * 
     * @param yamlValue, this is an object from the right-hand side of the parameter declaration in the
     *     yaml file. It can be a String, a File, a List of String, a List of File, or a Map of file groupings.
     *     
     * @return jsonValue, the JSON representation to be uploaded to the GenePattern REST API. It can be one of these types
     *     (based on the JSON.org spec): Boolean, Double, Integer, JSONArray, JSONObject, Long, String, or the JSONObject.NULL object.
     *     
     * @throws GpUnitException
     * @throws JSONException
     */
    protected List<ParamEntry> prepareInputValues(String pname, Object yamlValue) throws GpUnitException {
        // if it's an array ...
        if (yamlValue instanceof List<?>) {
            // expecting a List<String,Object>
            ParamEntry values=initJsonValueFromYamlList(pname, yamlValue);
            return Arrays.asList(new ParamEntry[]{values});
        }
        // or a map of grouped values ...
        else if (yamlValue instanceof Map<?,?>) {
            // expecting a Map<String,Object>
            return initJsonValueFromYamlMap(pname, yamlValue);
        }
        ParamEntry paramEntry = new ParamEntry(pname);
        String value=initJsonValueFromYamlObject(yamlValue);
        paramEntry.addValue(value);
        return Arrays.asList(new ParamEntry[]{paramEntry});
    }
    
    @SuppressWarnings("unchecked")
    protected ParamEntry initJsonValueFromYamlList(final String pname, final Object yamlValue) throws GpUnitException {
        List<Object> yamlList;
        try {
            yamlList = (List<Object>) yamlValue;
        }
        catch (Throwable t) {
            throw new GpUnitException("yaml format error, expecting List<Object> "+t.getLocalizedMessage());
        }
        ParamEntry paramEntry=new ParamEntry(pname);
        for(final Object yamlEntry : yamlList) {
            String value=initJsonValueFromYamlObject(yamlEntry);
            paramEntry.addValue(value);
        }
        return paramEntry;
    }

    @SuppressWarnings("unchecked")
    protected List<ParamEntry> initJsonValueFromYamlMap(final String pname, final Object yamlValue) throws GpUnitException {
        Map<String,List<Object>> yamlValueMap;
        try {
            yamlValueMap = (Map<String,List<Object>>) yamlValue;
        }
        catch (Throwable t) {
            throw new GpUnitException("yaml format error, expecting Map<String,Object> "+t.getLocalizedMessage());
        }
        List<ParamEntry> groups=new ArrayList<ParamEntry>();
        for(final Entry<String,List<Object>> entry : yamlValueMap.entrySet()) {
            String groupId=entry.getKey();
            GroupedParamEntry paramEntry = new GroupedParamEntry(pname, groupId);
            for(final Object yamlEntry : entry.getValue()) {
                // convert file input value into a URL if necessary
                String value=initJsonValueFromYamlObject(yamlEntry);
                paramEntry.addValue(value);
            }
            groups.add(paramEntry);
        }
        return groups;
    }
    
    /**
     * This method uploads the file from the local machine to the GP server when a local file path is specified.
     * In all cases it returns the value to be submitted to the GP server via the REST API call.
     * 
     * @param yamlEntry
     * @return
     * @throws GpUnitException
     */
    protected String initJsonValueFromYamlObject(final Object yamlEntry) throws GpUnitException {
        String updatedValue;
        try {
            updatedValue=InputFileUtil.getParamValueForInputFile(batchProps, test, yamlEntry);
        }
        catch (Throwable t) {
            throw new GpUnitException("Error initializing input file value from yamlEntry="+yamlEntry+": "+t.getLocalizedMessage());
        }
        URL url=uploadFileIfNecessary(updatedValue);
        if (url != null) {
            return url.toExternalForm();
        }
        return updatedValue;
    }

    /**
     * Initialize the JSONObject to PUT into the /jobs resource on the GP server.
     * Upload data files when necessary. For each file input parameter, if it's a local file, upload it and save the URL.
     * Use that url as the value when adding the job to GP.
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
    private JSONObject initJsonObject(final String lsid) throws GpUnitException, JSONException {
        final JSONObject obj=new JSONObject();
        obj.put("lsid", lsid);
        final JSONArray paramsJsonArray=new JSONArray();
        for(final Entry<String,Object> paramYamlEntry : test.getParams().entrySet()) {
            final List<ParamEntry> paramValues=prepareInputValues(paramYamlEntry.getKey(), paramYamlEntry.getValue());
            if (paramValues==null || paramValues.size()==0) {
                // replace empty list with list containing the empty string
                JSONObject paramObj=new JSONObject();
                paramObj.put("name", paramYamlEntry.getKey());
                JSONArray valuesArr=new JSONArray();
                valuesArr.put("");
                paramObj.put("values", valuesArr);
                paramsJsonArray.put(paramObj);
            }
            else {
                for(final ParamEntry paramValue : paramValues) {
                    JSONObject paramValueToJson=new JSONObject(paramValue);
                    paramsJsonArray.put(paramValueToJson);
                }
            }
        }
        obj.put("params", paramsJsonArray);
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
    
    private TaskObj getTaskObj(final String taskNameOrLsid) throws GpUnitException {
        final String urlStr=getTaskUrl.toExternalForm()+"/"+taskNameOrLsid;
        URI taskUri;
        try {
            taskUri = new URI(urlStr);
        }
        catch (URISyntaxException e) {
            throw new GpUnitException("URI syntax exception in "+urlStr, e);
        }
        
        JsonObject jsonObject=readJsonObjectFromUri(taskUri);
        return new TaskObj.Builder().fromJsonObject(jsonObject).build();
    }
    
    private String getInputFilePname(final JSONObject param) throws GpUnitException {
        if (param==null) {
            throw new GpUnitException("param==null");
        }
        final String[] names=JSONObject.getNames(param);
        if (names==null) {
            throw new GpUnitException("names==null");
        }
        if (names.length==0) {
            throw new GpUnitException("names.length==0");
        }
        if (names.length>1) {
            throw new GpUnitException("names.length=="+names.length);
        }
        final String pname=names[0];
        try {
            final String type=param.getJSONObject(pname).getJSONObject("attributes").getString("type");
            if (type.equals("java.io.File")) {
                return pname;
            }
        }
        catch (Throwable t) {
            throw new GpUnitException("Error getting type for parameter="+pname, t);
        }
        return null;
    }
    
    protected Set<String> getInputFileParamNames(JSONObject taskInfo) throws JSONException, GpUnitException {
        Set<String> inputFileParams=new HashSet<String>();
        JSONArray params=taskInfo.getJSONArray("params");
        for(int i=0; i<params.length(); ++i) {
            final JSONObject param=params.getJSONObject(i);
            final String pname=getInputFilePname(param);
            if (pname != null) {
                inputFileParams.add(pname);
            }
        }
        return inputFileParams;
    }

    public URI submitJob() throws GpUnitException {
        // make REST call to validate that the module.lsid (which could be a taskName or LSID)
        // is installed on the server
        final String taskNameOrLsid = test.getModule();
        final TaskObj taskInfo=getTaskObj(taskNameOrLsid);
        final String lsid=taskInfo.getLsid();
        
        JSONObject job;
        try {
            job = initJsonObject(lsid);
        }
        catch (Exception e) {
            throw new GpUnitException("Error preparing JSON object to POST to "+addJobUrl, e);
        }
        
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(addJobUrl.toExternalForm());
        post = setAuthHeaders(post);
        try {
            post.setEntity(new StringEntity(job.toString()));
        }
        catch (UnsupportedEncodingException e) {
            throw new GpUnitException("Error preparing HTTP request, POST "+addJobUrl, e);
        }

        HttpResponse response;
        try {
            response = client.execute(post);
        }
        catch (ClientProtocolException e) {
            throw new GpUnitException("Error executing HTTP request, POST "+addJobUrl, e);
        }
        catch (IOException e) {
            throw new GpUnitException("Error executing HTTP request, POST "+addJobUrl, e);
        }
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
            throw new GpUnitException(message);
        }
        
        String jobLocation=null;
        Header[] locations=response.getHeaders("Location");
        if (locations.length > 0) {
            jobLocation=locations[0].getValue();
        }
        if (jobLocation==null) {
            final String message="POST "+addJobUrl.toExternalForm()+" failed! Missing required response header: Location";
            throw new GpUnitException(message);
        }
        URI jobUri;
        try {
            jobUri = new URI(jobLocation);
            return jobUri;
        }
        catch (URISyntaxException e) {
            final String message="POST "+addJobUrl.toExternalForm()+" failed! "+e.getLocalizedMessage();
            throw new GpUnitException(message, e);
        }
    }
    
    /**
     * Helper method to GET the response from the web server as a JobResultObj.
     * Use this, for example, to GET the taskInfo.json object from, the server.
     * <pre>
       GET 127.0.0.1:8080/gp/rest/v1/tasks/ConvertLineEndings
     * </pre>
     * 
     * @param uri
     * @return
     * @throws Exception
     */
    public JobResultObj getJobResultObj(final URI uri) throws GpUnitException {
        JsonObject jsonObject=readJsonObjectFromUri(uri);
        JobResultObj jobResultObj=new JobResultObj.Builder().gsonObject(jsonObject).build();
        return jobResultObj;
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
    
    public void downloadFile(final URL from, final File toFile) throws Exception {
        HttpClient client = new DefaultHttpClient();        
        HttpGet get = new HttpGet(from.toExternalForm());
        get = setAuthHeaders(get);
        //HACK: in order to by-pass the GP login page, and use Http Basic Authentication,
        //     need to set the User-Agent to start with 'GenePatternRest'
        get.setHeader("User-Agent", "GenePatternRest");
        
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
    
    private void writeToFile(final InputStream in, final File toFile, final long maxNumBytes) 
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
