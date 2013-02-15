package org.genepattern.gpunit.yaml;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.genepattern.client.GPClient;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.gpunit.test.BatchProperties;
import org.genepattern.webservice.AnalysisWebServiceProxy;
import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.Parameter;
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
}
