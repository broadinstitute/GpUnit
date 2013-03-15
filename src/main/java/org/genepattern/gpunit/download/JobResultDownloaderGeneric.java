package org.genepattern.gpunit.download;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.test.BatchProperties;

/**
 * Updated job result downloader, with bug fix for special characters in job result filenames.
 * This uses the SOAP API for downloading result files.
 * 
 * @author pcarr
 *
 */
abstract public class JobResultDownloaderGeneric implements JobResultDownloader {
    private static class OutputFileCache {
        public String path;
        public URL serverUrl;
        public File localFile;
        
        public File initLocalFileFromPath(final File toDir) {
            File toFile = new File(toDir, path);
            return toFile;
        }
        
        public OutputFileCache(final String path, final URL serverUrl) {
            this(path, serverUrl, null);
        }
        public OutputFileCache(final String path, final URL serverUrl, final File localFile) {
            this.path=path;
            this.serverUrl=serverUrl;
            this.localFile=localFile;
        }
    }
    
    private String jobId="";
    final private BatchProperties props;
    /**
     * The job result files, each item is a pathname, relative to the 
     * job result directory on the GP server. Usually just a file name.
     * For example,
     *     all_aml_test.cvt.cls
     * Can include relative paths for jobs which create sub directories, e.g.
     *     dir1/filea.txt
     *     dir1/fileb.txt
     *     dir2/file_1.txt
     */
    final private Map<String,OutputFileCache> outputFileMap = new LinkedHashMap<String, OutputFileCache>();
    final private File downloadDir;

    public JobResultDownloaderGeneric(final File downloadDir, final BatchProperties props) {
        this.downloadDir=downloadDir;
        this.props=props;
    }
    
    public void setJobId(final String jobId) {
        this.jobId=jobId;
    }

    public void addOutputFilename(final String pathname, final URL serverUrl) {
        outputFileMap.put(pathname, new OutputFileCache(pathname, serverUrl));
    }
    
    public int getNumResultFiles() {
        return outputFileMap.size();
    }
    
    public boolean hasResultFile(final String filename) {
        return outputFileMap.containsKey(filename);
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
                throw new GpUnitException("Unable to create local download directory for jobId="+jobId+", downloadDir="+downloadDir.getAbsolutePath());
            }
        }
        downloadDirInited=true;
    }

    public File getResultFile(String filename) throws GpUnitException {
        OutputFileCache record=outputFileMap.get(filename);
        if (record == null) {
            throw new IllegalArgumentException("No entry in result files for filename="+filename);
        }
        if (record.localFile != null) {
            //means we already downloaded the file
            return record.localFile;
        }
        if (record.serverUrl == null) {
            throw new IllegalArgumentException("url not set for filename="+filename);
        }
        
        //if we are here, we have the URL for the file, download a copy and update the record
        return downloadFile(record);
    }
    
    /**
     * If necessary, download the file from the URL into the local direcory.
     * 
     * @param record
     * @return
     * @throws GpUnitException
     */
    private File downloadFile(OutputFileCache record) throws GpUnitException {
        if (record==null) {
            throw new IllegalArgumentException("record==null");
        }
        if (record.localFile != null) {
            //assume we have already succesfully downloaded the local file
            if (record.localFile.exists()) {
                return record.localFile;
            }
            throw new GpUnitException("downloaded file no longer exists: "+record.localFile.getPath());
        }
        if (record.serverUrl==null) {
            throw new IllegalArgumentException("record.url==null");
        }
        _initDownloadDir();
        final File toFile=record.initLocalFileFromPath(downloadDir);
        record.localFile=downloadFile(record.serverUrl, toFile);
        return record.localFile;
    }

    abstract public File downloadFile(final URL url, final File downloadDir) throws GpUnitException;
    

    private boolean downloadedFiles=false;
    public synchronized void downloadResultFiles() throws GpUnitException {
        if (downloadedFiles) {
            return;
        }
        for(final Entry<String,OutputFileCache> entry : outputFileMap.entrySet()) {
            downloadFile(entry.getValue());
        }
        downloadedFiles=true;
    }

    public void cleanDownloadedFiles() throws GpUnitException {
        //only going to clean files which were downloaded within the validation step
        List<File> not_deleted=new ArrayList<File>(); 
        
        for(final Entry<String,OutputFileCache> entry : outputFileMap.entrySet()) {
            final File downloadedFile=entry.getValue().localFile;
            if (downloadedFile != null) {
                if (downloadedFile.isFile()) {
                    boolean success=downloadedFile.delete();
                    if (!success) {
                        not_deleted.add(downloadedFile);
                    }
                }
                else {
                    not_deleted.add(downloadedFile);
                }
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
