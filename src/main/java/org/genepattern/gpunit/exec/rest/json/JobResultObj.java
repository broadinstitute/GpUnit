package org.genepattern.gpunit.exec.rest.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Wrapper class for a job result, as returned by the REST API call to the 
 * 'rest/v1/jobs/{jobId}' endpoint.
 * @author pcarr
 *
 */
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

        // GSON specific code
        public Builder gsonObject(JsonObject jsonObject) {
            // init jobId
            jobId(jsonObject.get("jobId").getAsString());

            //init hasError and isFinished
            final JsonObject status=jsonObject.get("status").getAsJsonObject();
            final boolean hasError=status.get("hasError").getAsBoolean();
            final boolean isFinished=status.get("isFinished").getAsBoolean();
            hasError(hasError);
            isFinished(isFinished);

            // initialize the list of output files
            JsonArray outputFilesJsonArray=jsonObject.get("outputFiles").getAsJsonArray();
            int numFiles=outputFilesJsonArray.size();
            for(int i=0; i<numFiles; ++i) {
                final JsonObject outputFileJsonObj=outputFilesJsonArray.get(i).getAsJsonObject();
                final JsonObject linkJsonObject=outputFileJsonObj.get("link").getAsJsonObject();
                final String name=linkJsonObject.get("name").getAsString();
                final String href=linkJsonObject.get("href").getAsString();
                OutputFile outputFile = new OutputFile(name, href);
                addOutputFile(outputFile);
            }

            return this;
        }
        
    }
}
