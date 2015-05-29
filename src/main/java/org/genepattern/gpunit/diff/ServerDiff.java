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
     * Given the actual and expected remote file URLs, synthesize a string representing a "test" that
     * will execute the server diff module on the actual and expected files.
     */
    private String createDiffModuleTestObject(String actualURL, String expectedURL) {
        StringBuilder sb = new StringBuilder();
        sb.append("name: Diff Module\ndescription: Dynamically created test to execute the server diff Module\nmodule: ");
        sb.append (serverDiffModuleName);
        sb.append("\nparams:\n");
        sb.append("    cmd: " + args.get(0));
        sb.append("\n    args: ");
        for (int i = 1; i < args.size(); i++) {
            sb.append(args.get(i));
            sb.append(" ");
        }
        sb.append("\n    input.filename.1: ");
        sb.append(actualURL);
        sb.append("\n    input.filename.2: ");
        sb.append(expectedURL);
        sb.append("\nassertions:\n    jobStatus: success\n"); 
        return sb.toString();
    }

    /**
     * Execute a server-side diffs to validate a job Result.
     */
    public void diff(String actualURL, String expectedURL) {
        String diffTask = createDiffModuleTestObject(actualURL, expectedURL);
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
