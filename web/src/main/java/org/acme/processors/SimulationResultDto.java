package org.acme.processors;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.acme.domain.SimulationJob;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimulationResultDto {
    @JsonProperty("reactantIds")
    private List<String> reactantIds;

    @JsonProperty("timeIndices")
    private List<Integer> timeIndices;

    @JsonProperty("modelRegions")
    private List<?> modelRegions;

    @JsonProperty("levels")
    private Map<String, Map<String, Integer>> levels;

    @JsonProperty("empty")
    private boolean empty;

    @JsonProperty("numberOfLevels")
    private int numberOfLevels;

    // Constructors
    public SimulationResultDto() {}

    public SimulationResultDto(List<String> reactantIds, List<Integer> timeIndices,
                            Map<String, Map<String, Integer>> levels, boolean empty, int numberOfLevels) {
        this.reactantIds = reactantIds;
        this.timeIndices = timeIndices;
        this.modelRegions = List.of();
        this.levels = levels;
        this.empty = empty;
        this.numberOfLevels = numberOfLevels;
    }

    // Getters and Setters
    public List<String> getReactantIds() {
        return reactantIds;
    }

    public void setReactantIds(List<String> reactantIds) {
        this.reactantIds = reactantIds;
    }

    public List<Integer> getTimeIndices() {
        return timeIndices;
    }

    public void setTimeIndices(List<Integer> timeIndices) {
        this.timeIndices = timeIndices;
    }

    public List<?> getModelRegions() {
        return modelRegions;
    }

    public void setModelRegions(List<?> modelRegions) {
        this.modelRegions = modelRegions;
    }

    public Map<String, Map<String, Integer>> getLevels() {
        return levels;
    }

    public void setLevels(Map<String, Map<String, Integer>> levels) {
        this.levels = levels;
    }

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }

    public int getNumberOfLevels() {
        return numberOfLevels;
    }

    public void setNumberOfLevels(int numberOfLevels) {
        this.numberOfLevels = numberOfLevels;
    }

    public void filterDisabledReactants(SimulationJob request) {
        List<String> nodesPlotted = request.getModel().getElements().getNodes().stream()
                .filter(n -> n.getData().isPlotted())
                .filter(n -> n.getData().isEnabled())
                .map(n -> n.getData().getId())
                .toList();

        List<String> enabledReactantIds = reactantIds.stream()
                .map(r -> r.substring(1)) // remove "n" prefix
                .filter(reactantId -> nodesPlotted.contains(reactantId))
                .toList();

        List<String> listToRemoveReactants = new ArrayList<>();

        for (String reactantId : levels.keySet()) {
            String normalizedReactantID = reactantId.substring(1);
            if (!enabledReactantIds.contains(normalizedReactantID)) {
                listToRemoveReactants.add(reactantId);
            }
        }

        listToRemoveReactants.forEach(levels::remove);

        reactantIds.removeAll(listToRemoveReactants);
    }
}
