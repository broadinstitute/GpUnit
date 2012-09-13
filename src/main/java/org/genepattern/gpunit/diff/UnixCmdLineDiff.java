package org.genepattern.gpunit.diff;

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
    public void init(List<String> args) {
        cmd_args = new String[]{"diff", "-q"};
    }
}
