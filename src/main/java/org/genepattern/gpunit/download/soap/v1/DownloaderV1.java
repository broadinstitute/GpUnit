package org.genepattern.gpunit.download.soap.v1;

import java.io.File;
import java.io.IOException;

import org.genepattern.gpunit.download.JobResultDownloader;
import org.genepattern.webservice.JobResult;

/**
 * Original implementation of job result downloader, circa first iteration of gp-unit development.
 * This code uses the GP client library up to and including the GP 3.5.0 release.
 * 
 * @author pcarr
 *
 */
public class DownloaderV1 implements JobResultDownloader {
    final private JobResult jobResult;
    public DownloaderV1(final JobResult jobResult) {
        this.jobResult=jobResult;
    }
    
    public File downloadFile(String filename, File downloadDir) throws IOException {
        final File file = jobResult.downloadFile(filename, downloadDir.getAbsolutePath());
        return file;
    }

    public File[] downloadFiles(File downloadDir) throws IOException {
        //TODO: could be a lengthy operation, consider running in an interruptible thread
        final File[] resultFiles=jobResult.downloadFiles(downloadDir.getAbsolutePath());
        return resultFiles;
    }
}
