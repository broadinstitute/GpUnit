package org.genepattern.gpunit.exec.rest;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.download.JobResultDownloaderGeneric;
import org.genepattern.gpunit.exec.rest.json.JobResultObj;
import org.genepattern.gpunit.exec.rest.json.JobResultObj.OutputFile;
import org.genepattern.gpunit.BatchProperties;

public class JobResultDownloaderRest extends JobResultDownloaderGeneric {
    public JobResultDownloaderRest(final File downloadDir, final BatchProperties props) {
        super(downloadDir, props);
    }

    private JobRunnerRest runner;
    
    public void setJobResult(final JobResultObj jobResult) throws Exception {
        if (jobResult==null) {
            throw new IllegalArgumentException("jobResult==null");
        }

        // initialize the list of result files
        List<OutputFile> outputFiles=jobResult.getOutputFiles();
        for(final OutputFile outputFile : outputFiles) {
            final String pathname=outputFile.getName();
            final URL url = new URL(outputFile.getHref());
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
            throw new GpUnitException("Failed to download file from url="+url, e);
        }
     }

}
