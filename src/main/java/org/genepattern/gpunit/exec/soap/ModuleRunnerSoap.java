package org.genepattern.gpunit.exec.soap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.genepattern.client.GPClient;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.gpunit.BatchProperties;
import org.genepattern.gpunit.yaml.InputFileUtil;
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
public class ModuleRunnerSoap {
    private final BatchProperties batchProps;
    private final ModuleTestObject test;
    private final GPClient gpClient;
    private AnalysisWebServiceProxy webService;

    public ModuleRunnerSoap(final BatchProperties batchProps, final ModuleTestObject test) {
        this.batchProps=batchProps;
        this.test = test;
        this.gpClient = ModuleRunnerSoap.initGpClient(batchProps);
    }
    
    public JobResult runJobAndWait() {
        return runJobSoap();
    }
    
    public void deleteJob(int jobId) throws GpUnitException {
        if (webService==null) {
            this.webService=initWebService(batchProps);
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
            throw new AssertionError("GpUnit configuration error, gpClient==null");
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

    protected static GPClient initGpClient(final BatchProperties batchProps) {
        GPClient gpClient = null;
        try {
            gpClient = new GPClient(batchProps.getGpUrl(), batchProps.getGpUsername(), batchProps.getGpPassword());
        }
        catch (Throwable t) {
            t.printStackTrace();
            throw new AssertionError("Error initializing gpClient for gpUrl='"+batchProps.getGpUrl()+"': "+t.getLocalizedMessage());
        }
        return gpClient;
    }
    
    protected static AnalysisWebServiceProxy initWebService(final BatchProperties batchProps) throws GpUnitException {
        AnalysisWebServiceProxy analysisProxy = null;
        try {
            analysisProxy = new AnalysisWebServiceProxy(batchProps.getGpUrl(), batchProps.getGpUsername(), batchProps.getGpPassword());
            //in milliseconds
            final int timeout=60*1000;
            analysisProxy.setTimeout(timeout);
            return analysisProxy;
        }
        catch (WebServiceException e) {
            throw new GpUnitException("Error initializing  AnalysisWebServiceProxy", e);
        }
    }
    
    private static Parameter[] initParams(final GPClient gpClient, final BatchProperties batchProps, final String nameOrLsid, final ModuleTestObject test) 
            throws WebServiceException, GpUnitException
    {
        InputFileUtil ifutil=new InputFileUtil(gpClient, batchProps, nameOrLsid);
        List<Parameter> params = new ArrayList<Parameter>();
        if (test.getParams() != null) {
            for(Entry<String,Object> entry : test.getParams().entrySet()) {
                Parameter param = ifutil.initParam(test, entry);
                params.add(param);
            }
        }
        
        //convert to an array
        return params.toArray(new Parameter[]{});
    }
}
