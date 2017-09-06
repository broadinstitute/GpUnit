package org.genepattern.gpunit.download.soap;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.download.JobResultDownloader;
import org.genepattern.gpunit.BatchProperties;
import org.genepattern.util.GPConstants;
import org.genepattern.util.JobDownloader;
import org.genepattern.webservice.JobResult;

/**
 * Updated job result downloader, with bug fix for special characters in job result filenames.
 * This uses the SOAP API for downloading result files.
 * 
 * @author pcarr
 *
 */
public class JobResultDownloaderSoap implements JobResultDownloader {
    final private BatchProperties props;
    final private int jobNumber;
    final private List<String> outputFileNames;
    final private File downloadDir;
    final private Map<String,File> resultFilesMap = new ConcurrentHashMap<String,File>();
    
    public JobResultDownloaderSoap(final File downloadDir, final BatchProperties props, final JobResult jobResult) {
        this.downloadDir=downloadDir;
        this.props=props;
        this.jobNumber=jobResult.getJobNumber();
        this.outputFileNames=_initOutputFilenames(jobResult);
    }

    private List<String> _initOutputFilenames(final JobResult jobResult) {
        List<String> rval = new ArrayList<String>();
        for(String outputFilename : jobResult.getOutputFileNames() ) {
            rval.add( outputFilename );
        }
        if (jobResult.hasStandardOut()) {
            rval.add( GPConstants.STDOUT );
        }
        if (jobResult.hasStandardError()) {
            rval.add( GPConstants.STDERR );
        }
        return rval;
    }
    
    public int getNumResultFiles() {
        return outputFileNames.size();
    }
    
    public boolean hasResultFile(final String filename) {
        return outputFileNames.contains(filename);
    }

    public String getServerURLForFile(String fileName) {
        throw new IllegalStateException("Illegal to retrieve server URL from SOAP Downloader");
    }

    private boolean downloadDirInited=false;
    private boolean downloadDirCreated=false;
    private synchronized void _initDownloadDir() throws GpUnitException {
        if (downloadDirInited) {
            return;
        }
        if (downloadDir==null) {
            throw new IllegalArgumentException("downloadDir=null");
        }
        if (!downloadDir.exists()) {
            downloadDirCreated = downloadDir.mkdirs();
            if (!downloadDirCreated) {
                throw new GpUnitException("Unable to create local download directory for jobNumber="+jobNumber+", downloadDir="+downloadDir.getAbsolutePath());
            }
        }
        downloadDirInited=true;
    }

    public File getResultFile(String filename) throws GpUnitException {
        File file=resultFilesMap.get(filename);
        if (file != null) {
            return file;
        }
        file = downloadFile(filename, this.downloadDir);
        resultFilesMap.put(filename, file);
        return file;
    }
    
    private File downloadFile(final String filename, final File downloadDir) throws GpUnitException {
        _initDownloadDir();
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
        try {
            d.download(url, toFile);
            return toFile;
        }
        catch (Throwable t) {
            throw new GpUnitException(t);
        }
    }
    
    private boolean downloadedFiles=false;
    public synchronized void downloadResultFiles() throws GpUnitException {
        if (downloadedFiles) {
            return;
        } 
        File[] resultFiles=downloadFiles(downloadDir);
        if (resultFiles != null) {
            for(File file : resultFiles) {
                resultFilesMap.put(file.getName(), file);
            }
        }
        downloadedFiles=true;
    }


    private File[] downloadFiles(final File downloadDir) throws GpUnitException {
        //TODO: could be a lengthy operation, consider running in an interruptible thread
        final List<File> downloadedFiles=new ArrayList<File>();
        for(final String filename : outputFileNames) {
            final File toFile = downloadFile(filename, downloadDir);
            downloadedFiles.add(toFile);
        }
        return downloadedFiles.toArray(new File[downloadedFiles.size()]); 
    }
    
    /** Converts a string into something you can safely insert into a URL. */
    @SuppressWarnings("deprecation")
    public static String encodeURIcomponent(String str) {
        String encoded = str;
        try {
            encoded = URLEncoder.encode(str, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            encoded = URLEncoder.encode(str);
        }
        
        //replace all '+' with '%20'
        encoded = encoded.replace("+", "%20");        
        return encoded;
    }

    public void cleanDownloadedFiles() throws GpUnitException {
        //only going to clean files which were downloaded within the validation step
        List<File> not_deleted=new ArrayList<File>(); 
        
        for(Entry<String,File> entry : resultFilesMap.entrySet()) {
            File file=entry.getValue();
            if (file.isFile()) {
                boolean success=file.delete();
                if (!success) {
                    not_deleted.add(file);
                }
            }
            else {
                not_deleted.add(file);
            }
        }
        if (downloadDirCreated) {
            boolean success=downloadDir.delete();
            if (!success) {
                not_deleted.add(downloadDir);
            }
        }
        if (not_deleted.size()>0) {
            throw new GpUnitException("failed to clean up job result directory: "+downloadDir);
        }
    }

}
