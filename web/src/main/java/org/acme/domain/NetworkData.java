package org.acme.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public class NetworkData {
    @JsonProperty("shared_name")
    public String sharedName;

    @JsonProperty("__Annotations")
    public String[] annotations;

    public String networkMetadata;
    public String name;

    @JsonProperty("SUID")
    public long suid;

    @JsonProperty("seconds_per_point")
    public double secondsPerPoint;

    public boolean selected;
    public int levels;

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

    public double getSecondsPerPoint() {
        return secondsPerPoint;
    }

    public void setSecondsPerPoint(double secondsPerPoint) {
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
}
