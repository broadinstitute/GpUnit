package org.genepattern.gpunit;

import java.util.ArrayList;
import java.util.List;

import org.genepattern.gpunit.BatchProperties;
import org.yaml.snakeyaml.parser.ParserException;

public class TestFileObj {
    protected int numRows = -1;
    protected int numCols = -1;
    protected String diff = null;
    protected Object diffCmd; //the value as provided in the yaml file (can be a String or a List<String>)
    protected List<String> diffCmdArgs; //the value to use when validating the results (can be null)
    protected BatchProperties bp = null;

    protected BatchProperties getBatchProperties() {
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

    public int getNumRows() {
        return numRows;
    }
    public void setNumRows(int numRows) throws GpUnitException {
        this.numRows = numRows;
    }
    public int getNumCols() {
        return numCols;
    }
    public void setNumCols(int numCols) throws GpUnitException {
        this.numCols = numCols;
    }
    public String getDiff() {
        return diff;
    }
    public void setDiff(String diff) throws GpUnitException {
        PropertyExpansion pe = new PropertyExpansion();
        this.diff = pe.expandProperties(getBatchProperties(), diff);
    }
    public Object getDiffCmd() {
        return diffCmd;
    }
    public void setDiffCmd(Object o) throws GpUnitException {
        this.diffCmd = o;
        if (o instanceof List<?>) {
            //diffCmd = o;
            setDiffCmdFromList( (List<?>) o);
        }
        else if (o instanceof String) {
            //diffCmd = o;
            PropertyExpansion pe = new PropertyExpansion();
            setDiffCmdFromString(pe.expandProperties(getBatchProperties(), (String) o));
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

    private void setDiffCmdFromList(List<?> args) throws GpUnitException {
        this.diffCmdArgs = new ArrayList<String>();
        PropertyExpansion pe = new PropertyExpansion();
        for(Object arg : args) {
            if (arg instanceof String) {
                diffCmdArgs.add(pe.expandProperties(getBatchProperties(),(String)arg));
            }
            else {
                diffCmdArgs.add( pe.expandProperties(getBatchProperties(), arg.toString()) );
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