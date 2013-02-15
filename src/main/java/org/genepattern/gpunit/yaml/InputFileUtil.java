package org.genepattern.gpunit.yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.genepattern.client.GPClient;
import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.gpunit.test.BatchProperties;
import org.genepattern.webservice.Parameter;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * Helper class for initializing input file parameters.
 * 
 * How does gp-unit decide what to do with input file parameters?
 * 

If the value is a fully qualified path, e.g. /MyData/input.txt

0) if it's a URL, literal value
1) if it's a fully qualified path:
    a) if the file exists locally, upload it
    b) else, pass the path literally to the server (assume it's a server file path)

2) otherwise ... it's a relative path
    a) if the file exists relative to the directory which contains the test-case file, upload it
    b) else if 'local.path.prefix' is set AND if <local.path.prefix>/<relativepath> exists, upload it
    c) else if 'server.path.prefix' is set, literal path '<server.path.prefix>/<relativepath>
    d) else [WARNING!] ... literal value

 * @author pcarr
 *
 */
public class InputFileUtil {
    final private BatchProperties props;
    final private TaskInfo taskInfo;
    final private Map<String, ParameterInfo> pinfoMap;

    public InputFileUtil(final GPClient gpClient, final BatchProperties props, final String moduleNameOrLsid) throws WebServiceException {
        this.props=props;
        taskInfo=gpClient.getModule(moduleNameOrLsid);
        //initialize parameter map
        pinfoMap=new HashMap<String,ParameterInfo>();
        final ParameterInfo[] formalParameters = taskInfo.getParameterInfoArray();
        if (formalParameters==null) {
            throw new IllegalArgumentException("Error initializing parameter map from task, formalParameters==null");
        }
        for(ParameterInfo param : formalParameters) {
            pinfoMap.put(param.getName(), param);
        }
    }

    public TaskInfo getTaskInfo() {
        return taskInfo;
    }

    public Parameter initParam(final ModuleTestObject test, final Entry<String,Object> paramEntry) throws IOException, FileNotFoundException {
        String pName = paramEntry.getKey();
        String pValue=getParamValue(test, paramEntry);
        return new Parameter(pName, pValue);
    }

    public String getParamValue(final ModuleTestObject test, final Entry<String,Object> paramEntry) throws IOException, FileNotFoundException {
        final String pName = paramEntry.getKey();
        final ParameterInfo pinfo=getParameterInfo(pName);
        return getParamValue(props, pinfo, test, paramEntry);
    }

    public static String getParamValue(final BatchProperties props, final ParameterInfo pinfo, final ModuleTestObject test, final Entry<String,Object> paramEntry) throws IOException, FileNotFoundException {
        boolean isInputFile;
        if (pinfo==null) {
            //TODO log error message
            isInputFile=false;
        }
        else {
            isInputFile=pinfo.isInputFile();
        }
        return getParamValue(props, isInputFile, test, paramEntry);
    }

    public static String getParamValue(final BatchProperties props, final boolean isInputFile, final ModuleTestObject test, final Entry<String,Object> paramEntry) throws IOException, FileNotFoundException {
        String pName = paramEntry.getKey();
        Object pValue = paramEntry.getValue();

        if (pValue == null) {
            //convert to empty String
            return "";
        }
        if (pValue.toString().length()==0) {
            return "";
        }

        //special handling for input files
        if (isInputFile) {
            final String inputFileValue=getParamValueForInputFile(props, test, pName, pValue);
            return inputFileValue;
//            //0) if it's a URL
//            if (pValue instanceof String) {
//                try {
//                    URL url = new URL( (String) pValue);
//                    //it's a url
//                    return (String) pValue;
//                }
//                catch (MalformedURLException e) {
//                    //it's not a URL, continue
//                }
//            }
//
//            final File file;
//            if (pValue instanceof File) {
//                file = (File) pValue;
//            }
//            else if (pValue instanceof String) {
//                file = new File((String)pValue);
//            }
//            else {
//                //TODO: error, invalid type
//                throw new IllegalArgumentException("invalid type for "+pName+"="+pValue+", type is "+pValue.getClass().getName());
//            }
//
//            //1) if it's a fully qualified path:
//            if (file.isAbsolute()) {
//                //a) if the file exists locally, upload it
//                if (file.exists()) {
//                    //it's a local file, upload it
//                    return file.getCanonicalPath();
//                }
//                //b) else, it's a server file path, pass by reference
//                return file.getPath();
//            }
//
//            //2) otherwise ... it's a relative path
//            //a) if the file exists relative to the directory which contains the test-case file, upload it
//            final File relativeToInputDir = new File( test.getInputdir(), file.getPath() ).getCanonicalFile();
//            if (relativeToInputDir.exists()) {
//                //it's a local file, upload it
//                return relativeToInputDir.getCanonicalPath();
//            }
//
//            //b) else if 'local.path.prefix' is set AND if <local.path.prefix>/<relativepath> exists, upload it
//            if (BatchProperties.isSet(props.getUploadDir())) {
//                final File localDir=new File(props.getUploadDir());
//                final File relativeToLocalPathPrefix = new File(localDir, file.getPath());
//                if (relativeToLocalPathPrefix.exists()) {
//                    return relativeToLocalPathPrefix.getCanonicalPath();
//                }
//            }
//
//            //c) else if 'server.path.prefix' is set, literal path '<server.path.prefix>/<relativepath>
//            if (BatchProperties.isSet(props.getServerDir())) {
//                final File serverDir=new File(props.getServerDir());
//                final File serverPath=new File(serverDir, file.getPath());
//                return serverPath.getPath();
//            }
//
//            //d) else [WARNING!] ... literal value
//            return file.getPath();
        }

        return pValue.toString();
    }

    public static String getParamValueForInputFile(final BatchProperties props, final ModuleTestObject test, final String pName, final Object pValue) throws IOException, FileNotFoundException {
        //special handling for input files
        //0) if it's a URL
        if (pValue instanceof String) {
            try {
                URL url = new URL( (String) pValue);
                //it's a url
                return (String) pValue;
            }
            catch (MalformedURLException e) {
                //it's not a URL, continue
            }
        }

        final File file;
        if (pValue instanceof File) {
            file = (File) pValue;
        }
        else if (pValue instanceof String) {
            file = new File((String)pValue);
        }
        else {
            //TODO: error, invalid type
            throw new IllegalArgumentException("invalid type for "+pName+"="+pValue+", type is "+pValue.getClass().getName());
        }

        //1) if it's a fully qualified path:
        if (file.isAbsolute()) {
            //a) if the file exists locally, upload it
            if (file.exists()) {
                //it's a local file, upload it
                return file.getCanonicalPath();
            }
            //b) else, it's a server file path, pass by reference
            return file.getPath();
        }

        //2) otherwise ... it's a relative path
        //a) if the file exists relative to the directory which contains the test-case file, upload it
        final File relativeToInputDir = new File( test.getInputdir(), file.getPath() ).getCanonicalFile();
        if (relativeToInputDir.exists()) {
            //it's a local file, upload it
            return relativeToInputDir.getCanonicalPath();
        }

        //b) else if 'local.path.prefix' is set AND if <local.path.prefix>/<relativepath> exists, upload it
        if (BatchProperties.isSet(props.getUploadDir())) {
            final File localDir=new File(props.getUploadDir());
            final File relativeToLocalPathPrefix = new File(localDir, file.getPath());
            if (relativeToLocalPathPrefix.exists()) {
                return relativeToLocalPathPrefix.getCanonicalPath();
            }
        }

        //c) else if 'server.path.prefix' is set, literal path '<server.path.prefix>/<relativepath>
        if (BatchProperties.isSet(props.getServerDir())) {
            final File serverDir=new File(props.getServerDir());
            final File serverPath=new File(serverDir, file.getPath());
            return serverPath.getPath();
        }

        //d) else [WARNING!] ... literal value
        return file.getPath();
    }

    public ParameterInfo getParameterInfo(final String pname) {
        ParameterInfo pinfo=pinfoMap.get(pname);
        return pinfo;
    }

}
