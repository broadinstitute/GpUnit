package org.genepattern.gpunit.yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.genepattern.client.GPClient;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.gpunit.test.BatchProperties;
import org.genepattern.webservice.AnalysisWebServiceProxy;
import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.Parameter;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * Run a single job on a GP server. 
 * When the job is complete, use the JobResult object for validation.
 * 
 * @author pcarr
 *
 */
public class ModuleRunner {
    private GPClient gpClient;
    private BatchProperties batchProps;
    private AnalysisWebServiceProxy webService;
    private ModuleTestObject test;
    private JobResult jobResult;
    private int jobId = -1;

    public ModuleRunner(ModuleTestObject test) {
        this.test = test;
    }
    
    public void setGpClient(GPClient gpClient) {
        this.gpClient = gpClient;
    }
    
    public void setBatchProperties(final BatchProperties batchProps) {
        this.batchProps=batchProps;
    }
    
    public void setModuleTestObject(ModuleTestObject test) {
        this.test = test;
    }
    
    public JobResult getJobResult() {
        return jobResult;
    }
    
    public int getJobId() {
        return jobId;
    }
    
    public void runJobAndWait() {
        this.jobResult = runJobSoap();
        if (jobResult != null) {
            this.jobId = jobResult.getJobNumber();
        }
    }
    
    public void deleteJob(int jobId) throws GpUnitException {
        if (webService==null) {
            this.webService=initWebService();
        }
        try {
            webService.deleteJob(jobId);
        }
        catch (Throwable t) {
            throw new GpUnitException("Error deleting job, jobId="+jobId, t);
        }
    }
    
    /**
     * Submit a job to the GP server via the SOAP client, 
     * wait for the job to complete,
     * and return the JobResult object.
     * 
     * @param test
     * @return
     */
    private JobResult runJobSoap() {
        JobResult jobResult = null;
        String nameOrLsid = test.getModule();
        
        if (gpClient == null) {
            this.gpClient = initGpClient();
        }
        try {
            Parameter[] params = initParams(gpClient, batchProps, nameOrLsid, test);
            jobResult = gpClient.runAnalysis(nameOrLsid, params);
            if (jobResult == null) {
                throw new Exception("jobResult==null");
            }
        }
        catch (Throwable t) {
            throw new AssertionError("Error submitting job ["+test.getName()+", module='"+nameOrLsid+"']: "+t.getLocalizedMessage());
        }
        return jobResult;
    }

    synchronized static public GPClient initGpClient() {
        final String gpUrl = System.getProperty("genePatternUrl", "http://gpdev.broadinstitute.org");
        final String gpUsername = System.getProperty("username", "jntest");
        final String gpPassword = System.getProperty("password", "jntest");
        
        GPClient gpClient = null;
        try {
            gpClient = new GPClient(gpUrl, gpUsername, gpPassword);
        }
        catch (Throwable t) {
            t.printStackTrace();
            throw new AssertionError("Error initializing gpClient for gpUrl='"+gpUrl+"': "+t.getLocalizedMessage());
        }
        return gpClient;
    }
    
    synchronized static public AnalysisWebServiceProxy initWebService() throws GpUnitException {
        final String gpUrl = System.getProperty("genePatternUrl", "http://gpdev.broadinstitute.org");
        final String gpUsername = System.getProperty("username", "jntest");
        final String gpPassword = System.getProperty("password", "jntest");
        AnalysisWebServiceProxy analysisProxy = null;
        try {
            analysisProxy = new AnalysisWebServiceProxy(gpUrl, gpUsername, gpPassword);
            //final int timeout=Integer.MAX_VALUE; //in milliseconds
            final int timeout=60*1000;
            analysisProxy.setTimeout(timeout);
            return analysisProxy;
        }
        catch (WebServiceException e) {
            throw new GpUnitException("Error initializing  AnalysisWebServiceProxy", e);
        }
    }
    
    static private Parameter[] initParams(final GPClient gpClient, final BatchProperties batchProps, final String nameOrLsid, final ModuleTestObject test) 
            throws IOException, FileNotFoundException, WebServiceException {
        InputFileUtil ifutil=new InputFileUtil(gpClient, batchProps, nameOrLsid);
        List<Parameter> params = new ArrayList<Parameter>();
        for(Entry<String,Object> entry : test.getParams().entrySet()) {
            Parameter param = ifutil.initParam(test, entry);
            params.add(param);
        }
        
        //convert to an array
        return params.toArray(new Parameter[]{});
    }

    /**
     * Helper class for initializing input file parameters.
     * 
     * How does gp-unit decide what to do with input file parameters?
     * 

If the value is a fully qualified path, e.g. /MyData/input.txt

0) if it's a URL, literal value
1) if it's a fully qualified path:
    a) if the file exists locally, upload it
    b) else, pass the path literally to the server (assume it's a server file path)

2) otherwise ... it's a relative path
    a) if the file exists relative to the directory which contains the test-case file, upload it
    b) else if 'local.path.prefix' is set AND if <local.path.prefix>/<relativepath> exists, upload it
    c) else if 'server.path.prefix' is set, literal path '<server.path.prefix>/<relativepath>
    d) else [WARNING!] ... literal value

     * @author pcarr
     *
     */
    public static class InputFileUtil {
        final private BatchProperties props;
        final private TaskInfo taskInfo;
        final private Map<String, ParameterInfo> pinfoMap;

        public InputFileUtil(final GPClient gpClient, final BatchProperties props, final String moduleNameOrLsid) throws WebServiceException {
            this.props=props;
            taskInfo=gpClient.getModule(moduleNameOrLsid);
            //initialize parameter map
            pinfoMap=new HashMap<String,ParameterInfo>();
            final ParameterInfo[] formalParameters = taskInfo.getParameterInfoArray();
            if (formalParameters==null) {
                throw new IllegalArgumentException("Error initializing parameter map from task, formalParameters==null");
            }
            for(ParameterInfo param : formalParameters) {
                pinfoMap.put(param.getName(), param);
            }
        }
        
        public TaskInfo getTaskInfo() {
            return taskInfo;
        }
        
        private Parameter initParam(final ModuleTestObject test, final Entry<String,Object> paramEntry) throws IOException, FileNotFoundException {
            String pName = paramEntry.getKey();
            Object pValue = paramEntry.getValue();
        
            if (pValue == null) {
                //convert to empty String
                return new Parameter( pName, "" );
            }
            if (pValue.toString().length()==0) {
                return new Parameter( pName, "" );
            }
            
            ParameterInfo pinfo=getParameterInfo(pName);
            //special handling for input files
            if (pinfo.isInputFile()) {
                //0) if it's a URL
                if (pValue instanceof String) {
                    try {
                        URL url = new URL( (String) pValue);
                        //it's a url
                        return new Parameter( pName, (String) pValue);
                    }
                    catch (MalformedURLException e) {
                        //it's not a URL, continue
                    }
                }
                
                final File file;
                if (pValue instanceof File) {
                    file = (File) pValue;
                }
                else if (pValue instanceof String) {
                    file = new File((String)pValue);
                }
                else {
                    //TODO: error, invalid type
                    throw new IllegalArgumentException("invalid type for "+pName+"="+pValue+", type is "+pValue.getClass().getName());
                }
                
                //1) if it's a fully qualified path:
                if (file.isAbsolute()) {
                    //a) if the file exists locally, upload it
                    if (file.exists()) {
                        //it's a local file, upload it
                        return new Parameter( pName, file );
                    }
                    //b) else, it's a server file path, pass by reference
                    return new Parameter( pName, file.getPath() );
                }
                
                //2) otherwise ... it's a relative path
                //a) if the file exists relative to the directory which contains the test-case file, upload it
                final File relativeToInputDir = new File( test.getInputdir(), file.getPath() ).getCanonicalFile();
                if (relativeToInputDir.exists()) {
                    //it's a local file, upload it
                    return new Parameter( pName, relativeToInputDir );
                }
                
                //b) else if 'local.path.prefix' is set AND if <local.path.prefix>/<relativepath> exists, upload it
                if (BatchProperties.isSet(props.getUploadDir())) {
                    final File localDir=new File(props.getUploadDir());
                    final File relativeToLocalPathPrefix = new File(localDir, file.getPath());
                    if (relativeToLocalPathPrefix.exists()) {
                        return new Parameter( pName, relativeToLocalPathPrefix );
                    }
                }

                //c) else if 'server.path.prefix' is set, literal path '<server.path.prefix>/<relativepath>
                if (BatchProperties.isSet(props.getServerDir())) {
                    final File serverDir=new File(props.getServerDir());
                    final File serverPath=new File(serverDir, file.getPath());
                    return new Parameter( pName, serverPath.getPath() );
                }

                //d) else [WARNING!] ... literal value
                return new Parameter( pName, file.getPath() );
            }

            return new Parameter( pName, pValue.toString() );
        }
        
        public ParameterInfo getParameterInfo(final String pname) {
            ParameterInfo pinfo=pinfoMap.get(pname);
            return pinfo;
        }

    }
    

}
