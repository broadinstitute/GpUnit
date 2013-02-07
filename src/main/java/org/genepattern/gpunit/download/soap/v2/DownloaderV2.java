package org.genepattern.gpunit.download.soap.v2;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.genepattern.gpunit.download.JobResultDownloader;
import org.genepattern.gpunit.test.BatchProperties;
import org.genepattern.util.JobDownloader;
import org.genepattern.webservice.JobResult;

/**
 * Updated job result downloader, with bug fix for special characters in job result filenames.
 * 
 * @author pcarr
 *
 */
public class DownloaderV2 implements JobResultDownloader {
    final private BatchProperties props;
    final private int jobNumber;
    final private String[] outputFileNames;
    
    public DownloaderV2(final BatchProperties props, final JobResult jobResult) {
        this.props=props;
        this.jobNumber=jobResult.getJobNumber();
        this.outputFileNames=jobResult.getOutputFileNames();
    }
    
    public File downloadFile(final String filename, final File downloadDir) throws IOException {
        final File toFile=new File(downloadDir, filename);
        final String encodedFilename=encodeURIcomponent(filename);
        
        final String contextPath="gp";
        final String url=props.getGpUrl()+"/"+contextPath+"/jobResults/" + jobNumber + "/" + encodedFilename;
        String server=props.getGpUrl();
        if (server.endsWith("/")) {
            server=server.substring(0, server.length()-1);
        }
        if (server.endsWith("/gp")) {
            server=server.substring(0, server.length()-3);
        }
        JobDownloader d = new JobDownloader(server, props.getGpUsername(), props.getGpPassword());
        d.download(url, toFile);
        return toFile;
    }

    public File[] downloadFiles(final File downloadDir) throws IOException {
        //TODO: could be a lengthy operation, consider running in an interruptible thread
        final List<File> downloadedFiles=new ArrayList<File>();
        for(final String filename : outputFileNames) {
            final File toFile = downloadFile(filename, downloadDir);
            downloadedFiles.add(toFile);
        }
        return downloadedFiles.toArray(new File[downloadedFiles.size()]); 
    }
    
    /** Converts a string into something you can safely insert into a URL. */
    public static String encodeURIcomponent(String str) {
        String encoded = str;
        try {
            encoded = URLEncoder.encode(str, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            //log.error("UnsupportedEncodingException for enc=UTF-8", e);
            encoded = URLEncoder.encode(str);
        }
        
        //replace all '+' with '%20'
        encoded = encoded.replace("+", "%20");        
        return encoded;
    }


}
