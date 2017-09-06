package org.genepattern.gpunit.exec.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.genepattern.gpunit.BatchProperties;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.gpunit.exec.rest.json.JobResultObj;
import org.genepattern.gpunit.exec.rest.json.TaskObj;
import org.genepattern.gpunit.yaml.InputFileUtil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Run a job on a GP server, using the REST API.
 * 
 * @author pcarr
 *
 */
public class JobRunnerRest {
    //context path is hard-coded
    private final String gpContextPath="gp";
    private BatchProperties batchProps;
    private ModuleTestObject test;
    private URL addFileUrl;
    private URL jobsUrl;
    private URL getTaskUrl;
    private RestClient restClient;

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
        this.addFileUrl=initEndpointUrl(batchProps, "/data/upload/job_input");
        this.jobsUrl=initEndpointUrl(batchProps, "/jobs");
        this.getTaskUrl=initEndpointUrl(batchProps, "/tasks");
        this.restClient=new RestClient(batchProps);
    }

    private URL initEndpointUrl(final BatchProperties batchProps, final String endpoint) throws GpUnitException
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

    /**
     * Prepare the (list of) input file(s) from the given yamlValue. 
     * If necessary upload each input file to the server. This method blocks while files are being transferred.
     * 
     * @param yamlValue, this is an object from the right-hand side of the parameter declaration in the
     *     yaml file. It can be a String, a File, a List of String, a List of File, or a Map of file groupings.
     *     
     * @return a List of parameters, as ParamEntry instances. 
     *     
     * @throws GpUnitException
     */
    protected List<ParamEntry> prepareInputValues(String pname, Object yamlValue) throws GpUnitException {
        if (yamlValue==null) {
            //special-case, handle null yamlValue
            return null;
        }
        
        // if it's an array ...
        if (yamlValue instanceof List<?>) {
            // expecting a List<String,Object>
            ParamEntry values=initParamEntryFromYamlList(pname, yamlValue);
            return Arrays.asList(new ParamEntry[]{values});
        }
        // or a map of grouped values ...
        else if (yamlValue instanceof Map<?,?>) {
            // expecting a Map<String,Object>
            return initParamEntriesFromYamlMap(pname, yamlValue);
        }
        ParamEntry paramEntry = new ParamEntry(pname);
        String value=initJsonValueFromYamlObject(yamlValue);
        paramEntry.addValue(value);
        return Arrays.asList(new ParamEntry[]{paramEntry});
    }
    
    @SuppressWarnings("unchecked")
    protected ParamEntry initParamEntryFromYamlList(final String pname, final Object yamlValue) throws GpUnitException {
        List<Object> yamlList;
        try {
            yamlList = (List<Object>) yamlValue;
        }
        catch (Throwable t) {
            throw new GpUnitException("yaml format error, expecting List<Object>", t);
        }
        ParamEntry paramEntry=new ParamEntry(pname);
        for(final Object yamlEntry : yamlList) {
            String value=initJsonValueFromYamlObject(yamlEntry);
            paramEntry.addValue(value);
        }
        return paramEntry;
    }

    @SuppressWarnings("unchecked")
    protected List<ParamEntry> initParamEntriesFromYamlMap(final String pname, final Object yamlValue) throws GpUnitException {
        Map<String,List<Object>> yamlValueMap;
        try {
            yamlValueMap = (Map<String,List<Object>>) yamlValue;
        }
        catch (Throwable t) {
            throw new GpUnitException("yaml format error, expecting Map<String,Object>", t);
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
            throw new GpUnitException("Error initializing input file value from yamlEntry="+yamlEntry, t);
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
    private JsonObject initJobInputJsonObject(final String lsid) throws GpUnitException {
        final JsonObject obj=new JsonObject();
        obj.addProperty("lsid", lsid);
        final JsonArray paramsJsonArray=new JsonArray();
        for(final Entry<String,Object> paramYamlEntry : test.getParams().entrySet()) {
            Object value = paramYamlEntry.getValue();
            List<ParamEntry> paramValues=null;            
            if (value!=null) {
                paramValues=prepareInputValues(paramYamlEntry.getKey(), value);
            }
            if (value==null) {
                //special-case, ignore parameter with null value in yaml file
            }
            else if (paramValues==null || paramValues.size()==0) {
                // replace empty list with list containing the empty string
                JsonObject paramObj=new JsonObject();
                paramObj.addProperty("name", paramYamlEntry.getKey());
                JsonArray valuesArr=new JsonArray();
                valuesArr.add(new JsonPrimitive(""));
                paramObj.add("values", valuesArr);
                paramsJsonArray.add(paramObj);
            }
            else {
                Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                for(final ParamEntry paramValue : paramValues) {
                    JsonElement jsonElement=gson.toJsonTree(paramValue);
                    paramsJsonArray.add(jsonElement);
                }
            }
        }
        obj.add("params", paramsJsonArray);
        return obj;
    }

    private URL uploadFileIfNecessary(final String value) throws GpUnitException {
        if (value==null) {
            // null arg, should be ignored
            return null;
        }
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
        
        final HttpClient client = HttpClients.createDefault();
        
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
        post = restClient.setAuthHeaders(post);
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
        
        JsonObject jsonObject=restClient.readJsonObjectFromUri(taskUri);
        return new TaskObj.Builder().fromJsonObject(jsonObject).build();
    }

    public URI submitJob() throws GpUnitException {
        // make REST call to validate that the module.lsid (which could be a taskName or LSID)
        // is installed on the server
        final String taskNameOrLsid = test.getModule();
        final TaskObj taskInfo=getTaskObj(taskNameOrLsid);
        final String lsid=taskInfo.getLsid();
        
        JsonObject job;
        try {
            job = initJobInputJsonObject(lsid);
        }
        catch (Exception e) {
            throw new GpUnitException("Error preparing JSON object to POST to "+jobsUrl, e);
        }
        
        final HttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(jobsUrl.toExternalForm());
        post = restClient.setAuthHeaders(post);
        try {
            post.setEntity(new StringEntity(job.toString()));
        }
        catch (UnsupportedEncodingException e) {
            throw new GpUnitException("Error preparing HTTP request, POST "+jobsUrl, e);
        }

        HttpResponse response;
        try {
            response = client.execute(post);
        }
        catch (ClientProtocolException e) {
            throw new GpUnitException("Error executing HTTP request, POST "+jobsUrl, e);
        }
        catch (IOException e) {
            throw new GpUnitException("Error executing HTTP request, POST "+jobsUrl, e);
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
            String message="POST "+jobsUrl.toExternalForm()+" failed! "+statusCode+": "+response.getStatusLine().getReasonPhrase();
            throw new GpUnitException(message);
        }
        
        String jobLocation=null;
        Header[] locations=response.getHeaders("Location");
        if (locations.length > 0) {
            jobLocation=locations[0].getValue();
        }
        if (jobLocation==null) {
            final String message="POST "+jobsUrl.toExternalForm()+" failed! Missing required response header: Location";
            throw new GpUnitException(message);
        }
        URI jobUri;
        try {
            jobUri = new URI(jobLocation);
            return jobUri;
        }
        catch (URISyntaxException e) {
            final String message="POST "+jobsUrl.toExternalForm()+" failed!";
            throw new GpUnitException(message, e);
        }
    }
    
    /**
     * Delete the job with id jobID from the server.
     */
    public void deleteJob(String jobID) throws GpUnitException {
        final String urlStr=jobsUrl.toExternalForm() + "/" + jobID + "/delete";
        URI deleteURI;
        try {
            deleteURI = new URI(urlStr);
        }
        catch (URISyntaxException e) {
            throw new GpUnitException("URI syntax exception in "+urlStr, e);
        }

        final HttpClient client = HttpClients.createDefault();
        HttpDelete delete = new HttpDelete(deleteURI);

        // set auth headers
        delete=restClient.setAuthHeaders(delete);
//        String orig = batchProps.getGpUsername()+":"+batchProps.getGpPassword();
//        //encoding  byte array into base 64
//        final byte[] encoded = Base64.encodeBase64(orig.getBytes());
//        final String basicAuth="Basic "+new String(encoded);
//        delete.setHeader("Authorization", basicAuth);
//        delete.setHeader("Content-type", "application/json");
//        delete.setHeader("Accept", "application/json");

        HttpResponse response;
        try {
            response = client.execute(delete);
        }
        catch (ClientProtocolException e) {
            throw new GpUnitException("Error executing HTTP request, POST "+jobsUrl, e);
        }
        catch (IOException e) {
            throw new GpUnitException("Error executing HTTP request, POST "+jobsUrl, e);
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
            String message="POST "+jobsUrl.toExternalForm()+" failed! "+statusCode+": "+response.getStatusLine().getReasonPhrase();
            throw new GpUnitException(message);
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
        JsonObject jsonObject=restClient.readJsonObjectFromUri(uri);
        JobResultObj jobResultObj=new JobResultObj.Builder().gsonObject(jsonObject).build();
        return jobResultObj;
    }
    
    public void downloadFile(final URL from, final File toFile) throws Exception {
        final HttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(from.toExternalForm());
        get = restClient.setAuthHeaders(get);
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
