package org.genepattern.gpunit.yaml;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;

import org.genepattern.gpunit.ModuleTestObject;

/**
 * Helper class which converts a gp_execution_log.txt file into a gp-unit file in yaml format.
 * 
 * Example execution log,
<pre>
# Created: Mon Apr 23 16:49:21 EDT 2012 by pcarr@broadinstitute.org
# Job: 16558    server:  http://gpdev.broadinstitute.org/gp/
# Module: ConvertLineEndings urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2
# Parameters: 
#    input.filename = /xchip/gpdev/shared_data/sqa/TestFiles/all_aml_test.cls   /gp/data//xchip/gpdev/shared_data/sqa/TestFiles/all_aml_test.cls
#    output.file = <input.filename_basename>.cvt.<input.filename_extension>
</pre>

Example input file parameters,
 * 1) external url
#    input.filename = ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.cls
 * 2) job upload
#    input.filename = /xchip/gpdev/shared_data/sqa/TestFiles/all_aml_test.cls   /gp/data//xchip/gpdev/shared_data/sqa/TestFiles/all_aml_test.cls
 * 2) a job with a previous job result file
#    input.filename = all_aml_test.cvt.res   http://gpdev.broadinstitute.org/gp/jobResults/16556/all_aml_test.cvt.res
 * 3) a job with a server file path file
#    input.filename = /xchip/gpdev/shared_data/sqa/TestFiles/all_aml_test.cls   /gp/data//xchip/gpdev/shared_data/sqa/TestFiles/all_aml_test.cls

 * 
 * 
 * @author pcarr
 */
public class ExecutionLogParser {

    static public ModuleTestObject parse(File executionLog) throws Exception {
        File testDir = null;
        if (executionLog.isAbsolute()) {
            testDir = executionLog.getParentFile();
        }
        else {
            testDir = executionLog.getCanonicalFile().getParentFile();
        }
        V v = new V(testDir);
        
        LineNumberReader reader = null;
        reader = new LineNumberReader(new FileReader(executionLog));
        try {
            String line = null;
            while((line = reader.readLine()) != null) {
                v.nextLine(line);
            }
            return v.getTestCase();
        }
        finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
    
    //visitor pattern-ish, create a new instance of this object, read each line from the execution log, and build up the model
    private static class V {
        private static class ParamEntry {
            public String name="";
            public Object value=null;
        }
        
        public V() {
            this(new File("."));
        }
        public V(File testDir) {
            testCase.setInputdir(testDir);
        }

        private int lineCount=0;
        private String server="";
        private String job="";
        private ModuleTestObject testCase = new ModuleTestObject();
        
        public ModuleTestObject getTestCase() {
            return testCase;
        }
        
        private void error(String errorMessage) throws Exception {
            throw new Exception(errorMessage);
        }
        
        public void nextLine(String line) throws Exception {
            ++lineCount;
            if (line == null) {
                //TODO: should throw exception?
                return;
            }
            if (line.trim().length() == 0) {
                //TODO: should throw exception?
                return;
            }
            //strip out leading '#' and trim whitespace
            if (line.startsWith("#")) {
                line = line.substring(1);
                if (line == null) {
                    return;
                }
                line = line.trim();
            }
            
            if (lineCount == 2) {
                //set the server name, helpful for handling input parameters
                //# Job: 16556    server:  http://gpdev.broadinstitute.org/gp/
                String[] tokens = line.split(" ");
                if (tokens.length == 4) {
                    job = tokens[1];
                    server = tokens[3];
                }
            }

            if (lineCount < 3) {
                return;
            }
            if (lineCount == 3) {
                //parse the module name and lsid
                //# Module: ConvertLineEndings urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2
                String[] tokens = line.split(" ");
                if (tokens.length < 2 || !tokens[0].startsWith("Module:")) {
                    error("Expecting '# Module: <name> <lsid>' on line 3");
                }
                testCase.setModule(tokens[1]);
                if (tokens.length > 2) {
                    testCase.setModuleLsid(tokens[2]);
                }
                return;
            }
            if (lineCount == 4) {
                //should be '# Parameters: ' line
                if (!line.startsWith("Parameters:")) {
                    error("Expecting '# Parameters: ' on line 4");
                }
                return;
            }
            //else, assume it's another input parameter
            ParamEntry paramEntry = parseParamLine(line);
            if (paramEntry != null) {
                testCase.getParams().put(paramEntry.name, paramEntry.value);
            }

            
        }
        
        private ParamEntry parseParamLine(String line) throws Exception {
            //Example input file parameters,
// * 1) external url
//#    input.filename = ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.cls
// * 2) job upload
//#    input.filename = all_aml_test.res   /gp/getFile.jsp?job=16556&file=pcarr%40broadinstitute.org_run3943667334821074755.tmp%2Fall_aml_test.res
// * 2) a job with a previous job result file
//#    input.filename = all_aml_test.cvt.res   http://gpdev.broadinstitute.org/gp/jobResults/16556/all_aml_test.cvt.res
// * 3) a job with a server file path file
//#    input.filename = /xchip/gpdev/shared_data/sqa/TestFiles/all_aml_test.cls   /gp/data//xchip/gpdev/shared_data/sqa/TestFiles/all_aml_test.cls
            int idx = line.indexOf(" = ");
            if (idx < 0) {
                error("Error parsing gp_execution_log, line "+lineCount+": Missing ' = '");
            }
            ParamEntry param = new ParamEntry();
            param.name = line.substring(0, idx).trim();
            param.value = line.substring(idx + 3);
            
            String[] tokens = param.value.toString().split(" ");
            if (tokens.length == 1) {
                //use literal value ... only one token
                return param;
            }
            
            //Note: for these special cases, user must manually download the correct input file into the test directory
            //special-case for job upload files, cast to a File object
            if (tokens[2].startsWith("/gp/getFile.jsp?")) {
                param.value = new File(param.name);
                return param;
            }
            //special-case for previous job result files, cast to a File object
            if (tokens[2].startsWith( server + "jobResults/" )) {
                param.value = new File(param.name);
                return param;
            }
            
            //all other parameters are passed by value
            return param;
        }
    }

}
