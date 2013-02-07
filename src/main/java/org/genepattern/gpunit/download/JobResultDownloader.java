package org.genepattern.gpunit.download;

import java.io.File;

import org.genepattern.gpunit.GpUnitException;

public interface JobResultDownloader {
    int getNumResultFiles();
    boolean hasResultFile(String filename);
    File getResultFile(String filename) throws GpUnitException;
    void downloadResultFiles() throws GpUnitException;
    void cleanDownloadedFiles() throws GpUnitException;
}