package org.acme;

import animo.core.analyser.uppaal.SimpleLevelResult;

public class SimulationResult {
    private String jobId;
    private String result;
    private String clientId;

    public SimulationResult(String jobId, String result, String clientId) {
        this.clientId = clientId;
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

    public String getClientId() {
        return clientId;
    }
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
