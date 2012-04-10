package org.genepattern.gpunit;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


public class ModuleTestObject {
    /** the name of the test */
    private String name = null;
    /** the name of the module */
    private String moduleName = null;
    /** the LSID of the module */
    private String moduleLsid = null;
    
    /** the input parameter values to use at module run time. */
    private Map<String,Object> params = new HashMap<String,Object>();
    /** the list of assertions */
    private GpAssertions assertions = new GpAssertions(); 
    /**
     * The root input directory for uploaded input files with a relative path.
     * @return
     */
    private File inputdir = null;
    
    public String getName() {
        return this.name;
    }
    public void setName(String str) {
        this.name = str;
    }
    public String getModule() {
        return moduleName;
    }
    public void setModule(String module) {
        this.moduleName = module;
    }
    public Map<String,Object> getParams() {
        return params;
    }
    public void setParams(Map<String,Object> inputParams) {
        this.params = inputParams;
    }
    public GpAssertions getAssertions() {
        return assertions;
    }
    public void setAssertions(GpAssertions assertions) {
        this.assertions = assertions;
    }
    public void setInputdir(File dir) {
        this.inputdir = dir;
    }
    public File getInputdir() {
        return inputdir;
    }
    
    public File initFileFromPath(String filepath) {
        File file = new File(filepath);
        if (file.isAbsolute()) {
            return file;
        }
        if (inputdir != null && inputdir.isDirectory()) {
            file = new File(inputdir, filepath);
            return file;
        }
        return file;
    }
    
}
