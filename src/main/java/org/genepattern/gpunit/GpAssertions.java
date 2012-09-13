package org.genepattern.gpunit;

import java.io.File;
import java.util.List;
import java.util.Map;


public class GpAssertions {
    private int exitCode = 0;
    private String jobStatus = "success";
    private int numFiles = -1;
    private Map<String,TestFileObj> files;
    private File outputDir = null;
    private List<String> diffCmd;

    public int getExitCode() {
        return exitCode;
    }
    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }
    public String getJobStatus() {
        return jobStatus;
    }
    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }
    public int getNumFiles() {
        return numFiles;
    }
    public void setNumFiles(int numFiles) {
        this.numFiles = numFiles;
    }
    public Map<String,TestFileObj> getFiles() {
        return files;
    }
    public void setFiles(Map<String,TestFileObj> files) {
        this.files = files;
    }
    public File getOutputDir() {
        return outputDir;
    }
    public void setOutputDir(File out) {
        this.outputDir = out;
    }
    
    //optionally set a default diff command
    public List<String> getDiffCmd() {
        return diffCmd;
    }
    public void setDiffCmd(List<String> args) {
        this.diffCmd = args;
    }
}