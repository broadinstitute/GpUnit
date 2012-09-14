package org.genepattern.gpunit;

import java.util.ArrayList;
import java.util.List;

public class TestFileObj {
    private int numRows = -1;
    private int numCols = -1;
    private String diff = null;
    //private String diffCmd = null;
    private Object diffCmd; //the value as provided in the yaml file (can be a String or a List<String>)
    private List<String> diffCmdArgs; //the value to use when validating the results (can be null)

    public int getNumRows() {
        return numRows;
    }
    public void setNumRows(int numRows) {
        this.numRows = numRows;
    }
    public int getNumCols() {
        return numCols;
    }
    public void setNumCols(int numCols) {
        this.numCols = numCols;
    }
    public String getDiff() {
        return diff;
    }
    public void setDiff(String diff) {
        this.diff = diff;
    }
    public Object getDiffCmd() {
        return diffCmd;
    }
    public void setDiffCmd(Object o) {
        this.diffCmd = o;
        if (o instanceof List<?>) {
            //diffCmd = o;
            setDiffCmdFromList( (List<?>) o);
        }
        else if (o instanceof String) {
            //diffCmd = o;
            setDiffCmdFromString( (String) o);
        }
        else {
            throw new IllegalArgumentException("diffCmd must be a String or a List<?>");
        }
    }

    private void setDiffCmdFromString(String cmd) {
        //by default, split on space characters
        if (cmd == null || cmd.length() == 0) {
            return;
        }
        String[] cmdArgs = cmd.split(" ");
        this.diffCmdArgs = new ArrayList<String>();
        for(String arg : cmdArgs) {
            diffCmdArgs.add( arg.trim() );
        }
    }

    private void setDiffCmdFromList(List<?> args) {
        this.diffCmdArgs = new ArrayList<String>();
        for(Object arg : args) {
            if (arg instanceof String) {
                diffCmdArgs.add((String)arg);
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