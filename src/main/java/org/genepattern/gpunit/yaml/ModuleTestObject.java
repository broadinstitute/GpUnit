package org.genepattern.gpunit.yaml;

import java.util.HashMap;
import java.util.Map;

public class ModuleTestObject {
    /** the name of the test */
    private String name = null;
    /** the name or lsid of the module */
    private String module = null;
    /** the input parameter values to use at module run time. */
    private Map<String,String> params = new HashMap<String,String>();
    /** the list of assertions */
    private GpAssertions assertions = new GpAssertions();
    
    public String getName() {
        return this.name;
    }
    public void setName(String str) {
        this.name = str;
    }
    public String getModule() {
        return module;
    }
    public void setModule(String module) {
        this.module = module;
    }
    public Map<String,String> getParams() {
        return params;
    }
    public void setParams(Map<String,String> inputParams) {
        this.params = inputParams;
    }
    public GpAssertions getAssertions() {
        return assertions;
    }
    public void setAssertions(GpAssertions assertions) {
        this.assertions = assertions;
    }
    
}
