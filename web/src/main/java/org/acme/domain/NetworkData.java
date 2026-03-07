package org.acme.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NetworkData {
    @JsonProperty("shared_name")
    private String sharedName;

    @JsonProperty("__Annotations")
    private String[] annotations;

    private String networkMetadata;
    private String name;

    @JsonProperty("SUID")
    private long suid;

    @JsonProperty("seconds_per_point")
    private Double secondsPerPoint;

    @JsonProperty("seconds per point")
    private Double secondsPerPointAlt;

    @JsonProperty("time scale factor")
    private Double timeScaleFactor;

    private boolean selected;
    private int levels;

    @Override
    public String toString() {
        return "NetworkData{" +
                "sharedName='" + sharedName + '\'' +
                ", annotations=" + Arrays.toString(annotations) +
                ", networkMetadata='" + networkMetadata + '\'' +
                ", name='" + name + '\'' +
                ", suid=" + suid +
                ", secondsPerPoint=" + secondsPerPoint +
                ", selected=" + selected +
                ", levels=" + levels +
                '}';
    }

    public String getSharedName() {
        return sharedName;
    }

    public void setSharedName(String sharedName) {
        this.sharedName = sharedName;
    }

    public String[] getAnnotations() {
        return annotations;
    }

    public void setAnnotations(String[] annotations) {
        this.annotations = annotations;
    }

    public String getNetworkMetadata() {
        return networkMetadata;
    }

    public void setNetworkMetadata(String networkMetadata) {
        this.networkMetadata = networkMetadata;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSuid() {
        return suid;
    }

    public void setSuid(long suid) {
        this.suid = suid;
    }

    public Double getSecondsPerPoint() {
        return secondsPerPoint;
    }

    public void setSecondsPerPoint(Double secondsPerPoint) {
        this.secondsPerPoint = secondsPerPoint;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getLevels() {
        return levels;
    }

    public void setLevels(int levels) {
        this.levels = levels;
    }

    public Double getSecondsPerPointAlt() {
        return secondsPerPointAlt;
    }

    public void setSecondsPerPointAlt(Double secondsPerPointAlt) {
        this.secondsPerPointAlt = secondsPerPointAlt;
    }

    public Double getTimeScaleFactor() {
        return timeScaleFactor;
    }

    public void setTimeScaleFactor(Double timeScaleFactor) {
        this.timeScaleFactor = timeScaleFactor;
    }
}
