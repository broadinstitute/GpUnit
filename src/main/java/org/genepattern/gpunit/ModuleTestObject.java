package org.genepattern.gpunit;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.gpunit.test.BatchProperties;
import org.genepattern.gpunit.PropertyExpansion;
import org.yaml.snakeyaml.parser.ParserException;


public class ModuleTestObject {
    /** the name of the test */
    private String name = null;
    /** a description of the test */
    private String description = null;
    /** the name of the module */
    private String moduleName = null;
    /** the LSID of the module */
    private String moduleLsid = null;
    
    /** the input parameter values to use at module run time. */
    private Map<String,Object> params = new HashMap<String,Object>();
    /** the list of assertions */
    private GpAssertions assertions = new GpAssertions();

    private BatchProperties bp = null;

    private BatchProperties getBatchProperties() {
        if (null == bp) {
            try {
                bp = BatchProperties.Factory.initFromProps();
            }
            catch (GpUnitException gpe) {
                throw new ParserException("Error properties during yaml parsing", null, "error intializing properties", null);
            }
        }
        return bp;
    }

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
    public String getDescription() {
        return this.description;
    }
    public void setDescription(String str) {
        this.description = str;
    }
    public String getModule() {
        return moduleName;
    }
    public void setModule(String module) {
        this.moduleName = module;
    }
    public String getModuleLsid() {
        return moduleLsid;
    }
    public void setModuleLsid(String moduleLsid) {
        this.moduleLsid = moduleLsid;
    }
    public Map<String,Object> getParams() {
        return params;
    }
    /*
     * Process all test params and substitute any property references with property values.
     */
    public void setParams(Map<String,Object> inputParams) {
        Map<String, Object> expandedParams = new HashMap<String, Object>();
        PropertyExpansion pe = new PropertyExpansion();

        try {
            for (String k : inputParams.keySet()) {
                Object v = inputParams.get(k);
                if (v instanceof String) {
                    expandedParams.put(k, pe.expandProperties(getBatchProperties(), (String) v));
                }
                else if (v instanceof List<?>) {
                    List<Object> newList = new ArrayList<Object>();
                    List<?> oldList = (List<?>) v;
                    for(Object val : oldList) {
                        if (val instanceof String) {
                            newList.add(pe.expandProperties(getBatchProperties(), (String) val));
                        }
                        else {
                            newList.add(val);
                        }
                    }
                    expandedParams.put(k,  newList);
                }
                else {
                    expandedParams.put(k, v);
                }
            }
        }
        catch (GpUnitException gpe) {
            throw new IllegalArgumentException(gpe.getLocalizedMessage());
        }
        this.params = expandedParams;
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
