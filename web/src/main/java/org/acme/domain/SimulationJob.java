package org.acme.domain;

import org.json.JSONObject;

public class SimulationJob {
    private final String id;
    private Integer minutesToSimulate;
    private JSONObject model;

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

    public JSONObject getModel() {
        return model;
    }

    public void setModel(JSONObject model) {
        this.model = model;
    }
}


