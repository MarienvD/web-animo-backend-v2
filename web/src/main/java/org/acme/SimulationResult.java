package org.acme;

public class SimulationResult {
    private String jobId;

    public SimulationResult() {

    }

    public SimulationResult(String jobId) {
        this.jobId = jobId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
}
