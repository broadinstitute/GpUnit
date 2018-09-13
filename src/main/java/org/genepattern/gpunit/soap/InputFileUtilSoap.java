package org.genepattern.gpunit.soap;

import org.genepattern.gpunit.BatchProperties;
import org.genepattern.gpunit.GpUnitException;
import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.gpunit.yaml.InputFileUtil;
import org.genepattern.webservice.Parameter;
import org.genepattern.webservice.ParameterInfo;

/**
 * Helper class for initializing input file parameters for the SOAP client.
 * 
 * @author pcarr
 */
public class InputFileUtilSoap {
    protected static final boolean isInputFile(final ParameterInfo pinfo) {
        if (pinfo==null) {
            //[WARNING!] ... log error message
            return false;
        }
        else {
            return pinfo.isInputFile();
        }
    }

    protected static final String getParamValue(
        final BatchProperties props, 
        final ModuleTestObject test, 
        final String pName, 
        final Object pValue, 
        final ParameterInfo pinfo
    ) 
    throws GpUnitException 
    {
        boolean isInputFile=isInputFile(pinfo);
        return InputFileUtil.getParamValue(props, isInputFile, test, pName, pValue);
    }

    public static final Parameter initParam(
        final BatchProperties props, 
        final ModuleTestObject test, 
        final String pName, 
        final Object pValue, 
        final ParameterInfo pinfo
    ) 
    throws GpUnitException 
    {
        final String paramValue=getParamValue(props, test, pName, pValue, pinfo);
        return new Parameter(pName, paramValue);
    }

}
