package org.genepattern.gpunit;

import java.io.File;

import org.genepattern.gpunit.yaml.Util;

/**
 * Run a single gpunit test from the command line.
 * 
 * @author pcarr
 */
public class RunTest {
    String gpServer="http://gpdev.broadinstitute.org";
    String gpUserid="jntest";
    String gpPassword="jntest";
    String testFile;
    String downloadJobResultsDir;
    
    public RunTest() {
    }
    
    public void runTest() {
        
    }
    
    static public void main(String[] args) {
        String testFilepath = "./tests/protocols/step1/test.yaml";
        
        if (args.length > 0) {
            //first arg is an optional test file
            testFilepath = args[0];
        }
        File testFile = new File(testFilepath);
        try {
            System.out.println("starting test "+testFile+" ... ");
            Util.runTest(testFile);
            System.out.println("Success!");
        }
        catch (Throwable t) {
            System.err.println("Failure!");
            t.printStackTrace();
        }
    }
}
