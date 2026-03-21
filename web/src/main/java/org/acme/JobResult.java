package org.acme;

import org.acme.domain.JobType;

public class JobResult {
    private String jobId;
    private String result;
    private String clientId;
    private JobType jobType;

    public JobResult() {}

    public JobResult(String jobId, String result, String clientId, JobType jobType) {
        this.clientId = clientId;
        this.jobId = jobId;
        this.result = result;
        this.jobType = jobType;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getClientId() {
        return clientId;
    }
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }
}
