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

import org.genepattern.gpunit.exec.rest.JobResultValidatorRest;

/**
 * Run the server diff module to verify job results.
 * 
 * The test synthesized by this code like the following:
 *
 *      name: Diff Module
 *      description: Dynamically created test to execute a server diff module
 *      module: Diff
 *      params:
 *          arg0: http://genepatternbeta/gp/jobResults/748/....
 *          arg1: ftp://gpftp.broadinstitute.org/gpunit/....
 *      assertions:
 *          jobStatus: success
 *
 * NOTE: In order to add support for other (custom) diff modules, or for arguments to diff modules,
 * the commented out code below (marked "@TODO Custom Diffs" - in 2 places can be uncommented
 * and enabled. An example of a test that uses a custom diff commmand or arguments looks
 * like this:
 *
 *     assertions:
 *          diffCmd: SomeCustomDiffModuleName -f ...
 *          files:
 *              TextFile.cvt.txt:
 *                  diff: <%gpunit.testData%>/gpunit/FileDifference/input/TextFileCopy.txt
 *
 * The diff test synthesized for that test would then look like this:
 *
 *      name: Diff Module
 *      description: Dynamically created test to execute a server diff module
 *      module: SomeCustomDiffModuleName
 *      params:
 *          args: -f ...
 *          arg0: http://genepatternnbeta/gp/jobResults/748/....
 *          arg1: ftp://gpftp.broadinstitute.org/gpunit/....
 *      assertions:
 *          jobStatus: success
 *
 * @author cnorman
 */
public class ServerDiff extends AbstractDiffTest {
    private static String serverDiffModuleName = "Diff";

    // job validator of the parent of this diff job (the job that generated the
    // results that we're diff'ing, AKA the job that needs to fail if the diff fails)
    private final JobResultValidatorRest parentJobValidator;

    public ServerDiff(JobResultValidatorRest parentJobValidator) {
        this.parentJobValidator = parentJobValidator;
    }

    /**
     * Given the actual and expected remote file URLs, synthesize a string representing a "test" that
     * will execute the server diff module on the actual and expected files.
     */
    private String createDiffModuleTestObject(String actualURL, String expectedURL) {
        StringBuilder sb = new StringBuilder();
        sb.append("name: Diff Module\ndescription: Dynamically created test to execute a server diff module\nmodule: ");
        if (null == args) {
            sb.append (serverDiffModuleName); // default to "Diff Module"
        }
        else {
            String cmd = args.get(0);
            if (!cmd.equalsIgnoreCase(serverDiffModuleName)) {
                // TODO Custom Diffs: uncomment to allow the user to name the module, which must conform to the diff module
                // template, i.e.: "modulename arg1 arg2"
                //sb.append(cmd);
                Assert.fail("No support for custom remote diff module: " + cmd);
            }
            else {
                // make sure the case sense is correct (many of the test files have lower case "diff", but the module is named "Diff")
                sb.append(serverDiffModuleName);
            }
        }
        sb.append("\nparams:");
        if (args.size() > 1) {
            // TODO Custom Diffs: uncomment to allow the yaml file to contain argumments to the diff command
            //sb.append("\n    args:");
            //for (int i = 1; i < args.size(); i++) {
            //   sb.append(" " + args.get(i));
            //}
            Assert.fail("Warning: no support for diff command arguments: " + args.get(0));
        }
        sb.append("\n    arg0: ");
        sb.append(actualURL);
        sb.append("\n    arg1: ");
        sb.append(expectedURL);
        sb.append("\nassertions:\n    jobStatus: success\n"); 
        return sb.toString();
    }

    /**
     * Execute a server-side diff to validate a job result.
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
                    RestClientUtil.runTest(bp, batchTestObj, parentJobValidator);
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
