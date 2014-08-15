package org.genepattern.gpunit.exec.rest;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.genepattern.gpunit.ModuleTestObject;
import org.genepattern.gpunit.test.BatchProperties;
import org.genepattern.gpunit.yaml.GpUnitFileParser;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class TestJobRunnerRest {
    private String pname="inputFileGroup";
    private BatchProperties batchProps;
    private ModuleTestObject testCase;
    
    @Before
    public void setUp() {
        batchProps=mock(BatchProperties.class);
        when(batchProps.getGpUrl()).thenReturn("http://127.0.0.1:8080/gp");
    }
    
    @Test
    public void fileGroup_asList() throws Exception {
        testCase=GpUnitFileParser.initTestCase(
                new File("./src/test/data/inputFileGroup_asList.yaml"));
        Object yamlValue=testCase.getParams().get(pname);
        assertEquals("expecting a List", true, (yamlValue instanceof List));
        
        JobRunnerRest jobRunner=new JobRunnerRest(batchProps, testCase);
        List<ParamEntry> groupedEntries=jobRunner.prepareInputValues(pname, yamlValue);
        assertEquals("num groups", 1, groupedEntries.size());
        assertEquals("num values", 4, groupedEntries.get(0).getValues().size());
        assertEquals("values[0]", "ftp://gpftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.cls", groupedEntries.get(0).getValues().get(0));
        assertEquals("values[0]", "ftp://gpftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.gct", groupedEntries.get(0).getValues().get(1));
        assertEquals("values[0]", "ftp://gpftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.cls", groupedEntries.get(0).getValues().get(2));
        assertEquals("values[0]", "ftp://gpftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.gct", groupedEntries.get(0).getValues().get(3));
        
        //asJson
        JSONObject jsonObj=new JSONObject(groupedEntries.get(0));
        assertEquals("json.name", pname, jsonObj.getString("name"));
        assertEquals("Not expecting a 'groupId'", false, jsonObj.has("groupId"));
        JSONArray jsonArr=jsonObj.getJSONArray("values");
        assertEquals("values JSONArray.length", 4, jsonArr.length());
    }
    
    @Test
    public void fileGroup_asMap() throws Exception {
        testCase=GpUnitFileParser.initTestCase(
                new File("./src/test/data/inputFileGroup_asMap.yaml"));
        Object yamlValue=testCase.getParams().get(pname);
        assertEquals("expecting a Map", true, (yamlValue instanceof Map));
        JobRunnerRest jobRunner=new JobRunnerRest(batchProps, testCase);
        List<ParamEntry> groupedEntries=jobRunner.prepareInputValues(pname, yamlValue);
        assertEquals("num groups", 2, groupedEntries.size());
        
        // convert to JSON for validation
        JSONObject jsonObjA=new JSONObject(groupedEntries.get(0));
        JSONObject jsonObjB=new JSONObject(groupedEntries.get(1));
        
        assertEquals("group[0].groupId", "Group A", jsonObjA.getString("groupId"));
        assertEquals("group[0].num values", 2, jsonObjA.getJSONArray("values").length());
        assertEquals("group[1].groupId", "Group B", jsonObjB.getString("groupId"));
        assertEquals("group[1].num values", 2, jsonObjB.getJSONArray("values").length());
    }
    
}
