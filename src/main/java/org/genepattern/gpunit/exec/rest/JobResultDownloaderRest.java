package org.genepattern.gpunit.exec.rest;

import java.io.File;
import java.net.URL;

import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.download.JobResultDownloaderGeneric;
import org.genepattern.gpunit.test.BatchProperties;
import org.json.JSONArray;
import org.json.JSONObject;

public class JobResultDownloaderRest extends JobResultDownloaderGeneric {
    public JobResultDownloaderRest(final File downloadDir, final BatchProperties props) {
        super(downloadDir, props);
    }

    private JobRunnerRest runner;
    
    public void setJobResult(final JSONObject jobResult) throws Exception {
        if (jobResult==null) {
            throw new IllegalArgumentException("jobResult==null");
        }
        
        //here is where we initialize the list of result files
        JSONArray outputFiles=jobResult.getJSONArray("outputFiles");
        int numFiles=outputFiles.length();
        for(int i=0; i<numFiles; ++i) {
            final JSONObject outputFile=outputFiles.getJSONObject(i);
            final JSONObject link=outputFile.getJSONObject("link");
            final String href=link.getString("href");
            final String pathname=link.getString("name");            
            final URL url = new URL(href);
            addOutputFilename(pathname, url);
        }
    }
    
    public void setRestClient(final JobRunnerRest runner) {
        this.runner=runner;
    }

    @Override
    public File downloadFile(URL url, File toFile) throws GpUnitException {
        if (runner==null) {
            throw new GpUnitException("runner==null");
        }
        try {
            runner.downloadFile(url, toFile);
            return toFile;
        }
        catch (Exception e) {
            throw new GpUnitException("Failed to download file from url="+url+": "
                    +e.getLocalizedMessage());
        }
     }

}
