package org.genepattern.gpunit.test;

import java.io.File;

import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.webservice.JobResult;

/**
 * Helper class to for setting shared properties for a single run of a batch of gp-unit tests.
 */
public class BatchProperties {
    final static public class Factory {
        static public BatchProperties initFromProps() throws GpUnitException {
            return new BatchProperties();
        }
    }
    
    // list of properties, configurable via System.setProperty 
    final static public String PROP_OUTPUT_DIR="gpunit.outputdir";
    final static public String PROP_BATCH_NAME="gpunit.batch.name";
    final static public String PROP_SAVE_DOWNLOADS="gpunit.save.downloads";
    
    private String downloadDir="./tmp/jobResults";
    private String batchName="latest";
    
    /**
     * By default delete downloaded result files, which also means "only download files when needed".
     * When this property is set to true, gp-unit will download all result files from the GP server
     * into a new directory on the local file system.
     */
    boolean saveDownloads=false;
    
    public BatchProperties() throws GpUnitException {
        //initialize values from system properties
        this.downloadDir=System.getProperty(PROP_OUTPUT_DIR, downloadDir);
        this.batchName=System.getProperty(PROP_BATCH_NAME, batchName);
        
        //options for handling result files
        this.saveDownloads=Boolean.getBoolean(PROP_SAVE_DOWNLOADS);
        
        this.batchOutputDir=_initBatchOutputDir();
    }
    
    public boolean getSaveDownloads() {
        return saveDownloads;
    }
    
    private boolean createdBatchOutputDir=false;
    private File batchOutputDir=null;
    //if necessary, create the top level download directory
    //    for all jobs in the batch
    private File _initBatchOutputDir() throws GpUnitException {
        String path=downloadDir;
        if (batchName != null) {
            if (!downloadDir.endsWith("/")) {
                path = downloadDir + "/" + batchName;
            }
            else {
                path = downloadDir + batchName;
            }
        }
        File rval=new File(path);
        if (!rval.exists()) {
            createdBatchOutputDir=rval.mkdirs();
            if (!createdBatchOutputDir) {
                throw new GpUnitException("Failed to initialize parent directory for downloading job results: "+batchOutputDir.getAbsolutePath());
            }
        }
        return rval;
    }
    
    public boolean getCreatedBatchOutputDir() {
        return createdBatchOutputDir;
    }
    
    public File getBatchOutputDir() {
        return batchOutputDir;
    }
    
    /**
     * Get the parent directory into which to download result files for all gp-unit tests in this batch.
     * 
     *     gpunit.download.dir, default=./tmp/jobResults
     *     gpunit.batch.name, default=latest
     *     
     * Rule for creating the download directory for a test-case.
     *     if (batch.name is set) {
     *         <download.dir>/<batch.name>/<test.name | job.name>
     *     }
     *     else {
     *         <download.dir>/<test.name | job.name>
     *     }
     *     
     * @return a directory into which to download job results for the given completed 
     */
    public File getJobResultDir(final ModuleTestObject testCase, final JobResult jobResult) throws GpUnitException {
        String dirname;
        if (testCase != null && testCase.getName() != null && testCase.getName().length() > 0) {
            dirname = testCase.getName();
        }
        else if (jobResult != null) {
            dirname = ""+jobResult.getJobNumber();
        }
        else {
            throw new IllegalArgumentException("Can't create job result directory, testCase and jobResult are not set!");
        }
        
        //File parentDir=initParentDir();
        File jobResultDir=new File(batchOutputDir, dirname);
        return jobResultDir;
    }

}