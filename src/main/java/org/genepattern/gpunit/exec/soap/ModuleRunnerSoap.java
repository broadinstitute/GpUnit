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
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;
import org.junit.Assert;

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
    protected JobResult runJobSoap() {
        JobResult jobResult = null;
        
        if (gpClient == null) {
            throw new AssertionError("GpUnit configuration error, gpClient==null");
        }

        final TaskInfo taskInfo;
        final String nameOrLsid = test.getModule();
        try {
            taskInfo=initTaskInfo(gpClient, nameOrLsid);
        }
        catch (Throwable t) {
            throw new AssertionError("Error getting taskInfo for job ["+test.getName()+", module='"+nameOrLsid+"']: "
                    +t.getLocalizedMessage(), t); 
        }

        final Parameter[] params;
        try {
            params = initParams(batchProps, taskInfo, test);
        }
        catch (Throwable t) {
            throw new AssertionError("Error initializing parameters for job ["+test.getName()+", module='"+nameOrLsid+"']: "
                    +t.getLocalizedMessage(), t);
        }
        try {
            jobResult = gpClient.runAnalysis(nameOrLsid, params);
        }
        catch (Throwable t) {
            throw new AssertionError("Error running job ["+test.getName()+", module='"+nameOrLsid+"']: "+t.getLocalizedMessage(), t);
        }
        if (jobResult == null) {
            Assert.fail("Error running job ["+test.getName()+", module='"+nameOrLsid+"']: No jobResult returned");
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
    
    protected static TaskInfo initTaskInfo(final GPClient gpClient, final String nameOrLsid) 
    throws GpUnitException
    {
        try {
            final TaskInfo taskInfo = gpClient.getModule(nameOrLsid);
            return taskInfo;
        } 
        catch (WebServiceException e) {
            throw new GpUnitException("Error getting taskInfo from SOAP API, module='"+nameOrLsid+"'", e);
        }
        catch (Throwable t) {
            throw new GpUnitException("Unexpected error getting taskInfo from SOAP API, module='"+nameOrLsid+"'", t);
        }
    }

    private static Parameter[] initParams(final BatchProperties batchProps, final TaskInfo taskInfo, final ModuleTestObject test) 
    throws GpUnitException
    {
        InputFileUtil ifutil=new InputFileUtil(batchProps, taskInfo);
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
