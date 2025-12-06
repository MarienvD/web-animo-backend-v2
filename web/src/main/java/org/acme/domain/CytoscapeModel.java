package org.acme.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CytoscapeModel extends GraphModel {
    @JsonProperty("format_version")
    private String formatVersion;

    @JsonProperty("generated_by")
    private String generatedBy;

    @JsonProperty("target_cytoscapejs_version")
    private String targetCytoscapejsVersion;

    private NetworkData data;

    @Override
    public String toString() {
        return "CytoscapeModel{" +
                "data=" + data +
                '}';
    }

    public String getFormatVersion() {
        return formatVersion;
    }

    public void setFormatVersion(String formatVersion) {
        this.formatVersion = formatVersion;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }

    public String getTargetCytoscapejsVersion() {
        return targetCytoscapejsVersion;
    }

    public void setTargetCytoscapejsVersion(String targetCytoscapejsVersion) {
        this.targetCytoscapejsVersion = targetCytoscapejsVersion;
    }

    public NetworkData getData() {
        return data;
    }

    public void setData(NetworkData data) {
        this.data = data;
    }
}
