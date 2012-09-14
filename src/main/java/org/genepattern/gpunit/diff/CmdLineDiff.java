package org.genepattern.gpunit.diff;

import java.io.File;
import java.io.IOException;

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

    protected String[] getCmdLine() {
        int K = 2;
        if (args != null) {
            K+= args.size();
        }
        String[] cmd = new String[K];
        int i=0;
        if (args != null) {
            for(String arg : args) {
                cmd[i++] = arg;
            }
        }
        cmd[i++] = expected.getAbsolutePath();
        cmd[i++] = actual.getAbsolutePath();
        
        //special-case for first arg
        if (cmd.length > 2) {
            String exec = cmd[0];
            // if it's a file, with a relative path, force it to be an FQ path, relative to the inputdir
            File execFile = new File(exec);
            if (!execFile.isAbsolute()) {
                
            }
            if (!execFile.isAbsolute() && inputDir != null) {
                //it's relative to test.inputdir
                try {
                    execFile = new File( inputDir, execFile.getPath() ).getCanonicalFile();
                }
                catch (IOException e) {
                    Assert.fail("Error initializing execFile for '"+execFile.getPath()+"': "+e.getLocalizedMessage());
                }
                if (execFile.canExecute()) {
                    try {
                        cmd[0] = execFile.getCanonicalPath();
                    }
                    catch (IOException e) {
                        Assert.fail("Error initializing custom executable for diff command: "+e.getLocalizedMessage());
                    }
                }
            }
        }
        
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
