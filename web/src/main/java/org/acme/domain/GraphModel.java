package org.acme.domain;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphModel {

    private Elements elements;

    public static class Elements {
        public List<Node> nodes;

        public List<Edge> getEdges() {
            return edges;
        }

        public void setEdges(List<Edge> edges) {
            this.edges = edges;
        }

        public List<Node> getNodes() {
            return nodes;
        }

        public void setNodes(List<Node> nodes) {
            this.nodes = nodes;
        }

        public List<Edge> edges;
    }

    public static class Node {
        private NodeData data;
        private Position position;
        private String group;
        private boolean removed;
        private boolean selected;
        private boolean selectable;
        private boolean locked;
        private boolean grabbable;
        private boolean pannable;
        private String classes;

        public NodeData getData() {
            return data;
        }

        public void setData(NodeData data) {
            this.data = data;
        }

        public Position getPosition() {
            return position;
        }

        public void setPosition(Position position) {
            this.position = position;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public boolean isRemoved() {
            return removed;
        }

        public void setRemoved(boolean removed) {
            this.removed = removed;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public boolean isSelectable() {
            return selectable;
        }

        public void setSelectable(boolean selectable) {
            this.selectable = selectable;
        }

        public boolean isLocked() {
            return locked;
        }

        public void setLocked(boolean locked) {
            this.locked = locked;
        }

        public boolean isGrabbable() {
            return grabbable;
        }

        public void setGrabbable(boolean grabbable) {
            this.grabbable = grabbable;
        }

        public boolean isPannable() {
            return pannable;
        }

        public void setPannable(boolean pannable) {
            this.pannable = pannable;
        }

        public String getClasses() {
            return classes;
        }

        public void setClasses(String classes) {
            this.classes = classes;
        }
    }

    public static class Edge {
        private EdgeData data;
        private Position position;
        private String group;
        private boolean removed;
        private boolean selected;
        private boolean selectable;
        private boolean locked;
        private boolean grabbable;
        private boolean pannable;
        private String classes;

        public EdgeData getData() {
            return data;
        }

        public void setData(EdgeData data) {
            this.data = data;
        }

        public Position getPosition() {
            return position;
        }

        public void setPosition(Position position) {
            this.position = position;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public boolean isRemoved() {
            return removed;
        }

        public void setRemoved(boolean removed) {
            this.removed = removed;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public boolean isSelectable() {
            return selectable;
        }

        public void setSelectable(boolean selectable) {
            this.selectable = selectable;
        }

        public boolean isLocked() {
            return locked;
        }

        public void setLocked(boolean locked) {
            this.locked = locked;
        }

        public boolean isGrabbable() {
            return grabbable;
        }

        public void setGrabbable(boolean grabbable) {
            this.grabbable = grabbable;
        }

        public boolean isPannable() {
            return pannable;
        }

        public void setPannable(boolean pannable) {
            this.pannable = pannable;
        }

        public String getClasses() {
            return classes;
        }

        public void setClasses(String classes) {
            this.classes = classes;
        }
    }

    public static class Position {
        private double x;
        private double y;

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }
    }

    public static class NodeData {
        private String id;
        private double activityRatio;
        private boolean plotted;

        @JsonProperty("NODE_TYPE")
        private String nodeType;

        private String description;
        private boolean enabled;

        @JsonProperty("shared_name")
        private String sharedName;

        @JsonProperty("Position_X")
        private double positionX;

        @JsonProperty("Position_Y")
        private double positionY;

        private String moleculeType;
        private String name;
        private boolean randomInitialConcentration;

        @JsonProperty("SUID")
        private long suid;

        private double initialConcentration;
        private boolean selected;
        private int levels;
        private String canonicalName;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public double getActivityRatio() {
            return activityRatio;
        }

        public void setActivityRatio(double activityRatio) {
            this.activityRatio = activityRatio;
        }

        public boolean isPlotted() {
            return plotted;
        }

        public void setPlotted(boolean plotted) {
            this.plotted = plotted;
        }

        public String getNodeType() {
            return nodeType;
        }

        public void setNodeType(String nodeType) {
            this.nodeType = nodeType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSharedName() {
            return sharedName;
        }

        public void setSharedName(String sharedName) {
            this.sharedName = sharedName;
        }

        public double getPositionX() {
            return positionX;
        }

        public void setPositionX(double positionX) {
            this.positionX = positionX;
        }

        public double getPositionY() {
            return positionY;
        }

        public void setPositionY(double positionY) {
            this.positionY = positionY;
        }

        public String getMoleculeType() {
            return moleculeType;
        }

        public void setMoleculeType(String moleculeType) {
            this.moleculeType = moleculeType;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isRandomInitialConcentration() {
            return randomInitialConcentration;
        }

        public void setRandomInitialConcentration(boolean randomInitialConcentration) {
            this.randomInitialConcentration = randomInitialConcentration;
        }

        public long getSuid() {
            return suid;
        }

        public void setSuid(long suid) {
            this.suid = suid;
        }

        public double getInitialConcentration() {
            return initialConcentration;
        }

        public void setInitialConcentration(double initialConcentration) {
            this.initialConcentration = initialConcentration;
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

        public String getCanonicalName() {
            return canonicalName;
        }

        public void setCanonicalName(String canonicalName) {
            this.canonicalName = canonicalName;
        }
    }

    public static class EdgeData {
        private String id;
        private String source;
        private String target;
        private double activityRatio;

        @JsonProperty("shared_interaction")
        private String sharedInteraction;

        @JsonProperty("Bend_handles")
        private List<Double> bendHandles;

        private String description;
        private int increment;
        private double k;
        private boolean enabled;

        @JsonProperty("shared_name")
        private String sharedName;

        private int scenario;
        private String name;
        private String interaction;

        @JsonProperty("SUID")
        private long suid;

        @JsonProperty("output_reactant")
        private String outputReactant;

        private boolean selected;
        private String canonicalName;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public double getActivityRatio() {
            return activityRatio;
        }

        public void setActivityRatio(double activityRatio) {
            this.activityRatio = activityRatio;
        }

        public String getSharedInteraction() {
            return sharedInteraction;
        }

        public void setSharedInteraction(String sharedInteraction) {
            this.sharedInteraction = sharedInteraction;
        }

        public List<Double> getBendHandles() {
            return bendHandles;
        }

        public void setBendHandles(List<Double> bendHandles) {
            this.bendHandles = bendHandles;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public int getIncrement() {
            return increment;
        }

        public void setIncrement(int increment) {
            this.increment = increment;
        }

        public double getK() {
            return k;
        }

        public void setK(double k) {
            this.k = k;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSharedName() {
            return sharedName;
        }

        public void setSharedName(String sharedName) {
            this.sharedName = sharedName;
        }

        public int getScenario() {
            return scenario;
        }

        public void setScenario(int scenario) {
            this.scenario = scenario;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getInteraction() {
            return interaction;
        }

        public void setInteraction(String interaction) {
            this.interaction = interaction;
        }

        public long getSuid() {
            return suid;
        }

        public void setSuid(long suid) {
            this.suid = suid;
        }

        public String getOutputReactant() {
            return outputReactant;
        }

        public void setOutputReactant(String outputReactant) {
            this.outputReactant = outputReactant;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public String getCanonicalName() {
            return canonicalName;
        }

        public void setCanonicalName(String canonicalName) {
            this.canonicalName = canonicalName;
        }
    }

    public Elements getElements() {
        return elements;
    }

    public void setElements(Elements elements) {
        this.elements = elements;
    }
}
