package org.acme;

import animo.core.analyser.uppaal.SimpleLevelResult;

public class SimulationResult {
    private String jobId;
    private String result;

    public SimulationResult(String jobId, String result) {
        this.jobId = jobId;
        this.result = result;
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
}
