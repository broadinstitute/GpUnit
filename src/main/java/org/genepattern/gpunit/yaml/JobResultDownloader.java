package org.genepattern.gpunit.yaml;

import java.io.File;
import java.io.IOException;

public interface JobResultDownloader {
    File downloadFile(String filename, File downloadDir) throws IOException;
    File[] downloadFiles(File downloadDir) throws IOException;
}