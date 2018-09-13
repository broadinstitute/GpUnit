package org.genepattern.gpunit.soap;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.genepattern.client.GPClient;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.gpunit.BatchProperties;
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
public class ModuleRunnerSoap {
    private final BatchProperties batchProps;
    private final ModuleTestObject test;
    private AnalysisWebServiceProxy webService;

    public ModuleRunnerSoap(final BatchProperties batchProps, final ModuleTestObject test) {
        this.batchProps=batchProps;
        this.test = test;
    }
    
    public JobResult runJobAndWait() {
        return runJobSoap();
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

    protected static GPClient initGpClient(final BatchProperties batchProps) {
        GPClient gpClient = null;
        try {
            gpClient = new GPClient(batchProps.getGpUrl(), batchProps.getGpUsername(), batchProps.getGpPassword());
        }
        catch (Throwable t) {
            throw new AssertionError("Error initializing gpClient for gpUrl='"+batchProps.getGpUrl()+"': "+t.getLocalizedMessage());
        }
        return gpClient;
    }

    private static Map<String, ParameterInfo> initPinfoMap(final TaskInfo taskInfo) {
        ParameterInfo[] formalParameters=taskInfo.getParameterInfoArray();
        if (formalParameters==null) {
            throw new IllegalArgumentException("Error initializing parameter map from task, formalParameters==null");
        }
        final Map<String, ParameterInfo> pinfoMap=new HashMap<String,ParameterInfo>();
        for(ParameterInfo param : formalParameters) {
            pinfoMap.put(param.getName(), param);
        }
        return pinfoMap;
    }

    private static Parameter[] initParams(final BatchProperties batchProps, final TaskInfo taskInfo, final ModuleTestObject test) 
    throws GpUnitException
    {
        // special-case: no test params
        if (test.getParams()==null || test.getParams().size()==0) {
            return new Parameter[0];
        }
        
        final Map<String, ParameterInfo> pinfoMap=initPinfoMap(taskInfo);
        final List<Parameter> params = new ArrayList<Parameter>();
        for(final Entry<String,Object> entry : test.getParams().entrySet()) {
            final String pName=entry.getKey();
            final Object pValue=entry.getValue();
            final ParameterInfo pinfo=pinfoMap.get(pName);
            final Parameter param = InputFileUtilSoap.initParam(batchProps, test, pName, pValue, pinfo);
            params.add(param);
        }
        
        //convert to an array
        return params.toArray(new Parameter[]{});
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
        // initialize SOAP client
        final GPClient gpClient = ModuleRunnerSoap.initGpClient(batchProps);
        if (gpClient == null) {
            throw new AssertionError("GpUnit configuration error, gpClient==null");
        }
        final String nameOrLsid = test.getModule();
        // make SOAP call to fetch TaskInfo
        final TaskInfo taskInfo;
        try {
            taskInfo = gpClient.getModule(nameOrLsid);
        } 
        catch (WebServiceException e) {
            throw new AssertionError("Error getting taskInfo from SOAP API, ["+test.getName()+", module='"+nameOrLsid+"']", e);
        }
        catch (Throwable t) {
            throw new AssertionError("Unexpected error getting taskInfo from SOAP API, ["+test.getName()+", module='"+nameOrLsid+"']", t);
        }
        // initialize parameters for SOAP API call
        final Parameter[] params;
        try {
            params = initParams(batchProps, taskInfo, test);
        }
        catch (Throwable t) {
            throw new AssertionError("Error initializing parameters for job ["+test.getName()+", module='"+nameOrLsid+"']: "
                    +t.getLocalizedMessage(), t);
        }
        // make SOAP call to run the job
        JobResult jobResult = null;
        try {
            jobResult = gpClient.runAnalysis(nameOrLsid, params);
        }
        catch (Throwable t) {
            throw new AssertionError("Error running job ["+test.getName()+", module='"+nameOrLsid+"']: "+t.getLocalizedMessage(), t);
        }
        if (jobResult == null) {
            fail("Error running job ["+test.getName()+", module='"+nameOrLsid+"']: No jobResult returned");
        }
        return jobResult;
    }

}
