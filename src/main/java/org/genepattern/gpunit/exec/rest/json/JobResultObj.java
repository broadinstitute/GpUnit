package org.genepattern.gpunit.exec.rest.json;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class JobResultObj {
    public static class Link {
    }
    
    public static class OutputFile {
        final String name;
        final String href;
        protected OutputFile(final String name, final String href) {
            this.name=name;
            this.href=href;
        }
        
        public String getName() {
            return name;
        }
        public String getHref() {
            return href;
        } 
    }
    
    private JSONObject obj;
    public JobResultObj(final JSONObject obj) {
        this.obj=obj;
    }

    public boolean hasError() throws Exception {
        JSONObject status=obj.getJSONObject("status");
        boolean hasError=status.getBoolean("hasError");
        return hasError;
    }
    
    public boolean isFinished() throws Exception {
        boolean isFinished;
        //try {
            isFinished=obj.getJSONObject("status").getBoolean("isFinished");
        //}
        //catch (Exception e) {
        //    throw new GpUnitException("Error parsing JSON object from: "+jobUri, e);
        //}
            return isFinished;
    }
    
    public List<OutputFile> getOutputFiles() throws Exception {
        final List<OutputFile> rval=new ArrayList<OutputFile>();        
        JSONArray outputFiles=obj.getJSONArray("outputFiles");
        int numFiles=outputFiles.length();
        for(int i=0; i<numFiles; ++i) {
            final JSONObject outputFile=outputFiles.getJSONObject(i);
            final JSONObject link=outputFile.getJSONObject("link");
            final String name=link.getString("name"); 
            final String href=link.getString("href");
            
            rval.add(new OutputFile(name, href));
        }
        return rval;
    }
    
    public String getString(final String key) throws Exception { 
        return obj.getString(key);
    }

    public JSONObject getJSONObject(final String key) throws Exception {
        return obj.getJSONObject(key);
    }

    public JSONArray getJSONArray(final String key) throws Exception {
        return obj.getJSONArray(key);
    }
}
