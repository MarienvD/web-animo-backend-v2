package org.acme.domain;

public class SimulationJob {
    private final String id;
    private Integer minutesToSimulate;
    private CytoscapeModel model;
    private String clientId;
    private String token;

    public SimulationJob(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Integer getMinutesToSimulate() {
        return minutesToSimulate;
    }

    public void setMinutesToSimulate(Integer minutesToSimulate) {
        this.minutesToSimulate = minutesToSimulate;
    }

    public CytoscapeModel getModel() {
        return model;
    }

    public void setModel(CytoscapeModel model) {
        this.model = model;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}


