package org.genepattern.gpunit.diff;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Assert;

/**
 * Example entry in _test.yaml file, 
 * <pre>
assertions:
    diffCmd: org.genepattern.gpunit.diff diff -q
    files:
        example_diff_input.cvt.txt:
            diff: ./expected.files/all_aml_test.preprocessed.sub28.2.clu
 * </pre>
 * @author pcarr
 *
 */
public class CmdLineDiff extends AbstractDiffTest {
    protected String[] cmd_args;
    
    public void init(List<String> args) {
        //I don't feel like reading the doc for 'System.arraycopy(arg0, arg1, arg2, arg3, arg4)' --pcarr
        if (args != null) {
            cmd_args = new String[args.size()];
            int i=0;
            for(String arg : args) {
                cmd_args[i++] = arg;
            }
        }
    }
    
    protected String[] getCmdLine() {
        int K = 2;
        if (cmd_args != null) {
            K+= cmd_args.length;
        }
        String[] cmd = new String[K];
        int i=0;
        if (cmd_args != null) {
            for(String arg : cmd_args) {
                cmd[i++] = arg;
            }
        }
        cmd[i++] = expected.getAbsolutePath();
        cmd[i++] = actual.getAbsolutePath();
        return cmd;
    }

    @Override
    public void diff() {
        boolean hasDiff = true;
        boolean interrupted = false;
        try {
            hasDiff = hasDiff(expected,actual);
        }
        catch (InterruptedException e) {
            interrupted = true;
            Thread.currentThread().interrupt();
        }
        catch (IOException e) {
            e.printStackTrace();
            Assert.fail("Error in diff( '"+expected.getPath()+"', '"+actual.getPath()+"' ): " +e.getLocalizedMessage());
        }
        if (interrupted) {
            Assert.fail("diff command was interrupted.");
        }
        if (hasDiff) {
            Assert.fail("Files differ, '"+expected.getPath()+"', '"+actual.getPath()+"'");
        } 
    }
    
    private boolean hasDiff(File expected, File actual) throws InterruptedException, IOException {
        String[] cmd = getCmdLine();
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(cmd);
        int exitCode = process.waitFor();
        if (exitCode == 0) {
            return false;
        }
        return true;
    }

}
