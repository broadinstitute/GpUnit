package org.genepattern.gpunit.download.soap.v1;

import java.io.File;

import org.genepattern.gpunit.GpUnitException;
import org.genepattern.webservice.JobResult;

/**
 * Original implementation of job result downloader, circa first iteration of gp-unit development.
 * This code uses the GP client library up to and including the GP 3.5.0 release.
 * 
 * @author pcarr
 *
 */
public class DownloaderV1 {
    final private JobResult jobResult;
    public DownloaderV1(final JobResult jobResult) {
        this.jobResult=jobResult;
    }
    
    public File downloadFile(String filename, File downloadDir) throws GpUnitException {
        try {
            final File file = jobResult.downloadFile(filename, downloadDir.getAbsolutePath());
            return file;
        }
        catch (Throwable t) {
            throw new GpUnitException(t);
        }
    }

    public File[] downloadFiles(File downloadDir) throws GpUnitException {
        //TODO: could be a lengthy operation, consider running in an interruptible thread
        try {
            final File[] resultFiles=jobResult.downloadFiles(downloadDir.getAbsolutePath());
            return resultFiles;
        }
        catch (Throwable t) {
            throw new GpUnitException(t);
        } 
    }
}
