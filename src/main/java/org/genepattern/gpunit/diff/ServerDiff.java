package org.genepattern.gpunit.diff;

import java.io.InputStream;
import java.io.ByteArrayInputStream;

import org.junit.Assert;

import org.genepattern.gpunit.BatchProperties;
import org.genepattern.gpunit.BatchModuleTestObject;
import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.exec.rest.RestClientUtil;
import org.genepattern.gpunit.yaml.ModuleTestParserYaml;

/**
 * Run the server diff module to verify job results.
 * 
 * @author cnorman
 */
public class ServerDiff extends AbstractDiffTest {
    private static String serverDiffModuleName = "FileDiff";

    /**
     * Given the serverURL, synthesize a string representing a "test" that will execute
     * the server diff module on the actual and expected files.
     */
    private String createDiffModuleTestObject(String serverURL) {
        StringBuilder sb = new StringBuilder();
        sb.append("name: Diff Module\ndescription: Dynamically created test to execute the server diff Module\nmodule: ");
        sb.append (serverDiffModuleName);
        sb.append("\nparams:\n    args: ");
        if (args.size() > 0) {
            for(String arg : args) {
                sb.append(arg);
                sb.append(" ");
            }
        }
        sb.append("\n    input.filename1: ");
        sb.append(serverURL);
        sb.append("\n    input.filename2: ");
        sb.append(expected.getAbsolutePath());
        sb.append("\nassertions:\n    jobStatus: success\n"); 
        return sb.toString();
    }

    /**
     * Execute a server-side diffs to validate a job Result.
     */
    @Override
    public void diff(String serverURL) {
        String diffTask = createDiffModuleTestObject(serverURL);
        InputStream is = new ByteArrayInputStream(diffTask.getBytes());
        try {
            ModuleTestObject testCase = ModuleTestParserYaml.parse(is);
            BatchModuleTestObject batchTestObj = new BatchModuleTestObject();
            batchTestObj.setTestCase(testCase);
            BatchProperties bp = BatchProperties.Factory.initFromProps();
            try {
                if (bp.getClient().equals(BatchProperties.GpUnitClient.REST)) {
                    RestClientUtil.runTest(bp, batchTestObj, true);
                }
                else {
                    throw new GpUnitException("Invalid SOAP call context; server side diffs are REST only");
                }
            }
            catch (Throwable t) {
                Assert.fail(t.getLocalizedMessage());
            }
        }
        catch (GpUnitException e) {
            Assert.fail("Failed parsing server diff module test definition (yaml).");
        }
    }
}
