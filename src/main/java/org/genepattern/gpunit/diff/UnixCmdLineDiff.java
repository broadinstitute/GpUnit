package org.genepattern.gpunit.diff;

import java.util.ArrayList;
import java.util.List;

/**
 * Implement diff as a unix command line,
 * <pre>
 *     diff -q <arg0> <arg1> 
 * </pre>
 * 
 * @author pcarr
 */
public class UnixCmdLineDiff extends CmdLineDiff {
    public UnixCmdLineDiff() {
        this.args = new ArrayList<String>();
        this.args.add("diff");
        this.args.add("-q");
    }
    
    //ignore setArgs in this class, they're hard-coded in the constructor
    public void setArgs(final List<String> in) {
    }
}
