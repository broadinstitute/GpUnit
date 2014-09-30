package org.genepattern.gpunit.exec.rest.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class JobResultObj {
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
    
    private final String jobId;
    private final boolean hasError;
    private final boolean isFinished;
    private final List<OutputFile> outputFiles;
    
    private JobResultObj(Builder in) {
        this.jobId=in.jobId;
        this.hasError=in.hasError;
        this.isFinished=in.isFinished;
        if (in.outputFiles==null) {
            this.outputFiles=Collections.emptyList();
        }
        else {
            this.outputFiles=Collections.unmodifiableList(in.outputFiles);
        }
    }
    
    public String getJobId() {
        return jobId;
    }

    public boolean hasError() {
        return hasError;
    }
    
    public boolean isFinished() {
        return isFinished;
    }
    
    public List<OutputFile> getOutputFiles() {
        return outputFiles;
    }
    
    /**
     * Builder pattern for building JobResultObj instances
     * @author pcarr
     *
     */
    public static class Builder {
        private String jobId=null;
        private boolean hasError=false;
        private boolean isFinished=false;
        private List<OutputFile> outputFiles=null;
        
//        private List<OutputFile> getOutputFiles() {
//            if (outputFiles==null) {
//                return Collections.emptyList();
//            }
//            return Collections.unmodifiableList(outputFiles);
//        }
//
        public Builder jobId(final String jobId) {
            this.jobId=jobId;
            return this;
        }

        public Builder hasError(final boolean hasError) {
            this.hasError=hasError;
            return this;
        }
        
        public Builder isFinished(final boolean isFinished) {
            this.isFinished=isFinished;
            return this;
        }
        
        public Builder addOutputFile(final OutputFile outputFile) {
            if (outputFiles==null) {
                outputFiles=new ArrayList<OutputFile>();
            }
            outputFiles.add(outputFile);
            return this;
        }
        
        public JobResultObj build() {
            return new JobResultObj(this);
        }
        
        // org.json specific code
        
        public Builder jsonObject(JSONObject jsonObject) throws Exception { 
            jobId(jsonObject.getString("jobId"));
            hasError(initHasError(jsonObject));
            isFinished(initIsFinished(jsonObject));
            
            // initialize the list of output files
            JSONArray outputFilesJsonArray=jsonObject.getJSONArray("outputFiles");
            int numFiles=outputFilesJsonArray.length();
            for(int i=0; i<numFiles; ++i) {
                final JSONObject outputFileJsonObj=outputFilesJsonArray.getJSONObject(i);
                final JSONObject linkJsonObject=outputFileJsonObj.getJSONObject("link");
                final String name=linkJsonObject.getString("name"); 
                final String href=linkJsonObject.getString("href");
                OutputFile outputFile = new OutputFile(name, href);
                addOutputFile(outputFile);
            }
            return this;
        }
        
        private boolean initHasError(JSONObject obj) throws Exception {
            JSONObject status=obj.getJSONObject("status");
            boolean hasError=status.getBoolean("hasError");
            return hasError;
        }
        
        private boolean initIsFinished(JSONObject obj) throws Exception {
            return obj.getJSONObject("status").getBoolean("isFinished");
        } 
    }
}
