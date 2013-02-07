package org.genepattern.gpunit.download;

import java.io.File;

import org.genepattern.gpunit.GpUnitException;

public interface JobResultDownloader {
    File downloadFile(String filename, File downloadDir) throws GpUnitException;
    File[] downloadFiles(File downloadDir) throws GpUnitException;
}