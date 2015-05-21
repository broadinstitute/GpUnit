package org.genepattern.gpunit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.parser.ParserException;


public class GpAssertions {
    private int exitCode = 0;
    private String jobStatus = "success";
    private int numFiles = -1;
    private Map<String,TestFileObj> files;
    private File outputDir = null;

    /*
        a Note regarding the diffCmd and diffCmdArgs fields.
        To make it possible to declare a command as either a single String or as a List of String ...
        For example,
            diffCmd: [org.genepattern.gpunit.diff.CmdLineDiff, diff, -q]
            diffCmd: org.genepattern.gpunit.diff.CmdLineDiff diff -q
        Both declarations are equivalent.
        The default snakeyaml parser will call setDiffCmd() with either a String or List<String> arg.
        However, when I added a getter, List<String> getDiffCmd(), snakeyaml parser throws an exception.
        This is why there is a separate getter, getCmdArgs which should be used from within the job validation step.
    */
    private Object diffCmd; //the value as provided in the yaml file (can be a String or a List<String>)
    private List<String> diffCmdArgs; //the value to use when validating the results (can be null)
    private BatchProperties bp = null;

    private BatchProperties getBatchProperties() {
        if (null == bp) {
            try {
                bp = BatchProperties.Factory.initFromProps();
            }
            catch (GpUnitException gpe) {
                throw new ParserException("Error properties during yaml parsing", null, "error intializing properties", null);
            }
        }
        return bp;
    }


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
    
    public Object getDiffCmd() {
        return diffCmd;
    }
    public void setDiffCmd(Object o) throws GpUnitException {
        this.diffCmd = o;
        PropertyExpansion pe = new PropertyExpansion();
        if (o instanceof List<?>) {
            //diffCmd = o;
            setDiffCmdFromList( (List<?>) o);
        }
        else if (o instanceof String) {
            //diffCmd = o;
            setDiffCmdFromString( pe.expandProperties(getBatchProperties(),(String) o));
        }
        else {
            throw new IllegalArgumentException("diffCmd must be a String or a List<?>");
        }
    }

    private void setDiffCmdFromString(String cmd) throws GpUnitException {
        //by default, split on space characters
        if (cmd == null || cmd.length() == 0) {
            return;
        }
        PropertyExpansion pe = new PropertyExpansion();
        String[] cmdArgs = cmd.split(" ");
        this.diffCmdArgs = new ArrayList<String>();
        for(String arg : cmdArgs) {
            diffCmdArgs.add( pe.expandProperties(getBatchProperties(),arg.trim()));
        }
    }

    private void setDiffCmdFromList(List<?> args) throws GpUnitException {
        this.diffCmdArgs = new ArrayList<String>();
        PropertyExpansion pe = new PropertyExpansion();
        for(Object arg : args) {
            if (arg instanceof String) {
                diffCmdArgs.add(pe.expandProperties(getBatchProperties(),(String)arg));
            }
            else {
                diffCmdArgs.add( arg.toString() );
            }
        }
    }
    
    /**
     * Call this from the gp-unit job validation step. If it is non-null, it means the test-case
     * declared a custom diff command.
     * @return can be null, a null or empty string means, 'use the default diff command'.
     */
    public List<String> getDiffCmdArgs() {
        return diffCmdArgs;
    }
}