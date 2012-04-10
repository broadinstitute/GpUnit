package org.genepattern.gpunit.yaml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.genepattern.client.GPClient;
import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.Parameter;

/**
 * Run a single job on a GP server. 
 * When the job is complete, use the JobResult object for validation.
 * 
 * @author pcarr
 *
 */
public class ModuleRunner {
    private GPClient gpClient;
    private ModuleTestObject test;
    private JobResult jobResult;

    public ModuleRunner(ModuleTestObject test) {
        this.test = test;
    }
    
    public void setGpClient(GPClient gpClient) {
        this.gpClient = gpClient;
    }
    
    public void setModuleTestObject(ModuleTestObject test) {
        this.test = test;
    }
    
    public JobResult getJobResult() {
        return jobResult;
    }
    
    public void run() {
        this.jobResult = runJobSoap();
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
            //GPClient gpClient = initGpClient();
            Parameter[] params = initParams(test);
        
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

    static public GPClient initGpClient() {
        final String gpUrl = "http://gpdev.broadinstitute.org";
        final String gpUsername = "jntest";
        final String gpPassword = "jntest";
        
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
    
    static private Parameter[] initParams(ModuleTestObject test) throws IOException {
        List<Parameter> params = new ArrayList<Parameter>();
        for(Entry<String,Object> entry : test.getParams().entrySet()) {
            Parameter param = initParam(test, entry);
            params.add(param);
        }
        
        //convert to an array
        return params.toArray(new Parameter[]{});
    }
    
    static private Parameter initParam(ModuleTestObject test, Entry<String,Object> paramEntry) throws IOException {
        String pName = paramEntry.getKey();
        Object pValue = paramEntry.getValue();
        
        if (pValue == null) {
            //convert to empty String
            pValue = "";
        }

        Parameter param = null;
        if (pValue instanceof File) {
            File inputFile = (File) pValue;
            if (!inputFile.isAbsolute()) {
                //it's relative to test.inputdir
                inputFile = new File( test.getInputdir(), inputFile.getPath() ).getCanonicalFile();
            }
            param = new Parameter( pName, inputFile );
        }
        else {
            param = new Parameter( pName, pValue.toString() );
        }
        return param;
    }
}
