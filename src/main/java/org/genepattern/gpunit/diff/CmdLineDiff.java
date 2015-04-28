package org.genepattern.gpunit.diff;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

/**
 * Default way to customize the diff command for validating job result files for a given module test case.
 * To declare a custom diff algorithm for a test case, add an enty to the assertions section.
 * 
 * Option 1: Applies to all defined result files:
 * <pre>
assertions:
    diffCmd: diff -q
    files:
        example_diff_input.cvt.txt:
            diff: ./expected.files/all_aml_test.preprocessed.sub28.2.clu
 * </pre>
 * 
 * Option 2: Applies on a per-file basis
 * <pre>
assertions:
    files:
        example_diff_input.cvt.txt:
            diffCmd: diff -q
            diff: ./expected.files/all_aml_test.preprocessed.sub28.2.clu
 * </pre>
 * 
 * There are several ways to define the custom diff command.
<pre>
assertions:
    # it's an executable (script) saved in the same directory as this test-case file
    diffCmd: ./myDiff.sh
    # can be a relative path to this test-case file
    diffCmd: ../copyOfmyDiff.sh
    # can be an executable, must be on the exec PATH
    diffCmd: diff -q
    # can be a fully qualified path to an executable
    diffCmd: /usr/bin/diff
    # can be a different java class which extends the AbstractDiffTest class
    diffCmd: org.genepattern.gpunit.diff.UnixCmdLineDiff
    # the 'org.genepattern.gpunit.diff.CmdLineDiff' is optional, but it works
    diffCmd: org.genepattern.gpunit.diff.CmdLineDiff diff -q
</pre>
 * @author pcarr
 *
 */
public class CmdLineDiff extends AbstractDiffTest {

    protected String[] getCmdLine() {
        //must have at least one arg
        if (args==null) {            
            Assert.fail("job #"+jobId+", invalid custom diff command: args==null");
        }
        if (args.size()==0) {
            Assert.fail("job #"+jobId+", invalid custom diff command: args.size==0");
        }
        
        //must have a valid executable
        String exec = args.get(0);
        File execFile = new File(exec);
        if (execFile.getParent() != null) {
            //special-case for relative paths, it's relative to test.inputdir 
            if (!execFile.isAbsolute() && inputDir != null) {
                try {
                    execFile = new File( inputDir, execFile.getPath() ).getCanonicalFile();
                }
                catch (IOException e) {
                    Assert.fail("job #"+jobId+", Error initializing execFile for '"+execFile.getPath()+"': "+e.getLocalizedMessage());
                }
            }
            if (!execFile.canExecute()) {
                Assert.fail("job #"+jobId+", invalid custom diff command: can't execute command, execFile="+execFile.toString());
            }
            try {
                exec = execFile.getCanonicalPath();
            }
            catch (IOException e) {
                Assert.fail("job #"+jobId+", Error initializing custom executable for diff command: "+e.getLocalizedMessage());
            }
        }
        
        List<String> cmdLine = new ArrayList<String>();
        cmdLine.add(exec);
        for(int i=1; i<args.size(); ++i) {
            cmdLine.add( args.get(i) );
        }
        cmdLine.add(expected.getAbsolutePath());
        cmdLine.add(actual.getAbsolutePath());
        
        //List to Array (should be more concise)
        String[] cmd = new String[ cmdLine.size() ];
        int i=0;
        for(String a : cmdLine) {
            cmd[i++] = a;
        }
        return cmd;
    }

    @Override
    public void diff() {
        int exitCode = 2;
        boolean interrupted = false;
        try {
            exitCode = hasDiff(expected,actual);
        }
        catch (InterruptedException e) {
            interrupted = true;
            Thread.currentThread().interrupt();
        }
        catch (IOException e) {
            e.printStackTrace();
            Assert.fail("job #"+jobId+", Error executing diff( '"+expected.getPath()+"', '"+actual.getPath()+"' ): " +e.getLocalizedMessage());
        }
        if (interrupted) {
            Assert.fail("job #"+jobId+", diff command was interrupted.");
        }
        else if (exitCode != 0) {
            String[] cmd = getCmdLine();
            Assert.fail("job #"+jobId+", Error executing diff ((cmd.toString()) return code: " + Integer.toString(exitCode) + "), '"+expected.getPath()+"', '"+actual.getPath()+"'");
        }
    }
    
    private int hasDiff(File expected, File actual) throws InterruptedException, IOException {
        String[] cmd = getCmdLine();
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(cmd);
        return process.waitFor();
    }
}
