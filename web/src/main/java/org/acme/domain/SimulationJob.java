package org.acme.domain;

public class SimulationJob {
    private String id;
    private Integer minutesToSimulate;
    private CytoscapeModel model;
    private String clientId;
    private String token;

    @Override
    public String toString() {
        return "SimulationJobParams{" +
                "id='" + id + '\'' +
                ", minutesToSimulate=" + minutesToSimulate +
                ", model=" + model +
                ", clientId='" + clientId + '\'' +
                ", token='" + token + '\'' +
                '}';
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
