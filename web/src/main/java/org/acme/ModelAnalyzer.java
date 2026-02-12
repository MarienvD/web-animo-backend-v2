package org.acme;

import animo.core.AnimoBackend;
import animo.core.analyser.AnalysisException;
import animo.core.analyser.uppaal.SimpleLevelResult;
import animo.core.analyser.uppaal.UppaalModelAnalyserSMC;
import animo.core.model.*;
import animo.cytoscape.Animo;
import animo.cytoscape.AnimoActionTask;
import animo.exceptions.AnimoException;
import animo.fitting.ParameterFitter;
import animo.fitting.ScenarioCfg;
import animo.fitting.levenbergmarquardt.LevenbergMarquardtFitter;
import animo.util.Utilities;
import com.google.common.io.Files;
import org.acme.domain.CytoscapeModel;
import org.acme.domain.NetworkData;
import org.cytoscape.work.TaskMonitor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Supplier;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import static org.acme.domain.GraphModel.*;

public class ModelAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(ModelAnalyzer.class);

    private Random random;

    public ModelAnalyzer(String configPath) throws AnimoException {
        this.random = new Random();
        File configFile = new File(configPath);
        if (!configFile.exists()) {
            throw new AnimoException("Animo config file not does not exist at " + configPath);
        } else {
            log.info("Loading config file: " + configFile.getAbsolutePath());
            AnimoBackend.initialise(configFile);
        }
    }

    public SimpleLevelResult simulateModel(Model model, int nMinutesToSimulate) throws AnalysisException {
        if (model.getReactionCollection().isEmpty()) {
            throw new AnalysisException("The model to be simulated requires at least one reaction");
        } else {
            Double tsf = (Double)model.getProperties().get("time scale factor").as(Double.class);
            Double spp = (Double)model.getProperties().get("seconds per point").as(Double.class);
            int timeTo = (int)(nMinutesToSimulate * (double)60.0F * tsf / spp);
            SimpleLevelResult result = (new UppaalModelAnalyserSMC((TaskMonitor)null, (AnimoActionTask)null)).analyze(model, timeTo);
            return result;
        }
    }

    // TODO add some listener to notify the best cost found
    public JSONObject performParameterFitting(Model model, JSONObject request
//                                              ,Consumer<ParameterFitter.CostResult> bestCostListener
    ) throws JSONException, IOException {
        List<Reaction> reactionsToBeOptimized = new ArrayList();
        JSONArray reactions = request.getJSONArray("reactions");

        for(Reaction reaction : model.getReactionCollection()) {
            for(int i = 0; i < reactions.length(); ++i) {
                String reactionID = reactions.getString(i);
                if (reaction.getId().equals(reactionID)) {
                    reactionsToBeOptimized.add(reaction);
                }
            }
        }

        String name = Integer.toString(this.random.nextInt());
        File tempCsvFile = File.createTempFile(name, ".csv");
        Files.write(request.getString("referenceData"), tempCsvFile, Charset.defaultCharset());
        String referenceDataFile = tempCsvFile.getAbsolutePath();
        SortedMap<Reactant, String> reactantToDataCorrespondence = new TreeMap();
        JSONArray reactantToReferenceJson = request.getJSONArray("reactantToReferenceData");

        for(int i = 0; i < reactantToReferenceJson.length(); ++i) {
            JSONObject correspondence = reactantToReferenceJson.getJSONObject(i);
            Reactant reactant = model.getReactant(correspondence.getString("reactantID"));
            reactantToDataCorrespondence.put(reactant, correspondence.getString("columnName"));
        }

        Properties parameters = new Properties();
        if (request.has("params")) {
            JSONObject params = request.getJSONObject("params");

            for(String param : JSONObject.getNames(params)) {
                parameters.setProperty(param, params.getString(param));
            }
        }

        int timeTo = request.getInt("minutesToSimulate");
        HashMap<String, Supplier<ParameterFitter>> fitters = new HashMap();
        fitters.put("lma", (Supplier)() -> new LevenbergMarquardtFitter(model, reactionsToBeOptimized, referenceDataFile, reactantToDataCorrespondence, timeTo, parameters));
        ParameterFitter fitter = (ParameterFitter)((Supplier)fitters.get(request.optString("algorithm", "genetic"))).get();

        fitter.performHeadlessParameterFitting();
        // TODO below
//        JSONObject result = this.parametersToJson(parameters);
//        tempCsvFile.delete();
//        return result;

        // when done, do this:
//        graph.reset();
//        function.compute(lm.getParameters(), X, Y, true);
        return request;
    }

    public static Model getModelFromJson(JSONObject object, Integer nMinutesToSimulate) throws JSONException, AnimoException {
        Model model = new Model();
        Map<Long, String> nodeSUIDToModelId = new HashMap();
        Map<Long, String> edgeSUIDToModelId = new HashMap();
        Map<String, Long> nodeJSONIDtoSUID = new HashMap();
        Map<String, Long> edgeJSONIDtoSUID = new HashMap();
        JSONObject modelData = object.getJSONObject("data");

        for(String field : JSONObject.getNames(modelData)) {
            Object value = JSONObject.stringToValue(modelData.get(field).toString());
            model.getProperties().let(field).setValue(value);
        }

        JSONObject elements = object.getJSONObject("elements");
        HashMap<String, String> nameToID = new HashMap();
        Double secondsPerPoint = modelData.optDouble("seconds per point", modelData.optDouble("seconds_per_point", (double)1.0F));
        double timeScaleFactor = modelData.optDouble("time scale factor", (double)1.0F / secondsPerPoint);

        model.getProperties().let("time scale factor").be(timeScaleFactor);
        model.getProperties().let("seconds per point").be(secondsPerPoint);
        boolean noReactantsPlotted = true;
        int maxNumberOfLevels = 0;
        int defaultNumberOfLevels = 100;
        JSONArray nodes = elements.getJSONArray("nodes");
        List<JSONObject> nodesList = new ArrayList();

        for(int i = 0; i < nodes.length(); ++i) {
            JSONObject n = nodes.getJSONObject(i);
            nodesList.add(n);
        }

        Collections.sort(nodesList, new Comparator<JSONObject>() {
            public int compare(JSONObject n1, JSONObject n2) {
                try {
                    n1 = n1.getJSONObject("data");
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }

                try {
                    n2 = n2.getJSONObject("data");
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }

                String name1 = n1.optString("canonicalName", n1.optString("name", ""));
                String name2 = n2.optString("canonicalName", n2.optString("name", ""));
                return name1.compareTo(name2);
            }
        });
        nodes = new JSONArray(nodesList);

        for(int i = 0; i < nodes.length(); ++i) {
            JSONObject data = nodes.getJSONObject(i).getJSONObject("data");
            String stringId = data.getString("id");
            long longId = Long.parseLong(stringId.replaceAll("\\D+", ""));
            String reactantId = "n" + stringId;
            Reactant r = new Reactant(reactantId);
            nodeSUIDToModelId.put(longId, reactantId);
            nodeJSONIDtoSUID.put(stringId, longId);
            boolean enabled = data.optBoolean("enabled", true);
            boolean plotted = data.optBoolean("plotted", true);
            if (plotted && enabled) {
                noReactantsPlotted = false;
            }

            r.let("enabled").be(enabled);
            r.let("plotted").be(plotted);
            r.let("canonicalName").be(data.optString("canonicalName", data.optString("name", "")));
            r.let("alias").be(r.get("canonicalName").as(String.class));
            int nLevels = data.optInt("levels", 100);
            if (nLevels > maxNumberOfLevels) {
                maxNumberOfLevels = nLevels;
            }

            r.let("levels").be(nLevels);
            r.let("initialConcentration").be(data.optInt("initialConcentration", 0));
            r.let("randomInitialConcentration").be(data.optBoolean("randomInitialConcentration", false));
            r.let("randomInitialConcentrationMinimum").be(data.optInt("randomInitialConcentrationMinimum", 0));
            r.let("randomInitialConcentrationMaximum").be(data.optInt("randomInitialConcentrationMaximum", nLevels));
            r.let("randomInitialConcentrationStep").be(data.optInt("randomInitialConcentrationStep", 1));
            nameToID.put(data.getString("name"), r.getId());
            if (enabled) {
                model.add(r);
            }
        }

        if (noReactantsPlotted) {
            throw new AnimoException("No nodes were marked as plotted. No graph would be shown.\nPlease mark at least one (enabled) node as plotted.");
        } else {
            model.getProperties().let("levels").be(maxNumberOfLevels);
            int minTimeModel = Integer.MAX_VALUE;
            int maxTimeModel = Integer.MIN_VALUE;
            double uncertainty = (double)0.0F;

            // TODO marien fix config (from properties or input)
//            try {
//                uncertainty = (double)Integer.valueOf(AnimoBackend.get().configuration().get("/ANIMO/Uncertainty"));
//            } catch (NumberFormatException var75) {
//                uncertainty = (double)0.0F;
//            }

            JSONArray edges = elements.getJSONArray("edges");

            for(int i = 0; i < edges.length(); ++i) {
                JSONObject data = edges.getJSONObject(i).getJSONObject("data");
                String reactionId = "reaction" + i;
                Reaction r = new Reaction(reactionId);
                boolean enabled = data.optBoolean("enabled", true);
                r.let("enabled").be(enabled);
                if (enabled && !data.optString("source", "-1").equals("-1") && !data.optString("target", "-1").equals("-1")) {
                    String sourceID = data.getString("source");
                    String targetID = data.getString("target");
                    if (nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(sourceID)) != null && nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(targetID)) != null) {
                        Reactant sourceNode = model.getReactant((String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(sourceID)));
                        Reactant targetNode = model.getReactant((String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(targetID)));
                        if (sourceNode != null && targetNode != null) {
                            int increment = data.optInt("increment", 0);
                            if (increment == 0) {
                                if (data.getLong("source") == data.getLong("target")) {
                                    increment = -1;
                                } else {
                                    increment = 1;
                                }
                            }

                            r.let("increment").be(increment);
                            int scenarioIdx = data.optInt("scenario", 0);
                            r.let("scenario").be(scenarioIdx);
                            if (scenarioIdx == 2) {
                                r.let("_REACTANT_E1").be(data.optString("_REACTANT_E1", data.getString("source")));
                                r.let("_REACTANT_ACT_E1").be(data.optBoolean("_REACTANT_ACT_E1", true));
                                r.let("_REACTANT_E2").be(data.optString("_REACTANT_E2", data.getString("target")));
                                r.let("_REACTANT_ACT_E2").be(data.optBoolean("_REACTANT_ACT_E2", true));
                            }

                            String sourceName = (String)sourceNode.get("canonicalName").as(String.class);
                            String targetName = (String)targetNode.get("canonicalName").as(String.class);
                            String edgeName = data.optString("canonicalName", "");
                            if (edgeName.equals("")) {
                                if (!sourceName.equals("") && !targetName.equals("")) {
                                    StringBuilder nameBuilder = new StringBuilder();
                                    String min1 = "-1";
                                    String idE1 = data.optString("_REACTANT_E1", min1);
                                    String idE2 = data.optString("_REACTANT_E2", min1);
                                    if (scenarioIdx == 2 && !idE1.equals(min1) && !idE2.equals(min1)) {
                                        nameBuilder.append((String)model.getReactant((String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(idE1))).get("canonicalName").as(String.class));
                                        nameBuilder.append(" AND ");
                                        nameBuilder.append((String)model.getReactant((String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(idE2))).get("canonicalName").as(String.class));
                                    } else {
                                        nameBuilder.append(sourceName);
                                        nameBuilder.append(increment >= 0 ? " --> " : " --| ");
                                        nameBuilder.append(targetName);
                                    }

                                    edgeName = nameBuilder.toString();
                                } else {
                                    edgeName = "New Interaction";
                                }
                            }

                            r.let("canonicalName").be(edgeName);
                            r.let("alias").be(edgeName);
                            if (scenarioIdx < 0 || scenarioIdx >= Scenario.THREE_SCENARIOS.length) {
                                scenarioIdx = 0;
                                throw new AnimoException("The reaction " + edgeName + " has an invalid scenario setting (" + scenarioIdx + "). Now I set it to the first available: please set the correct parameters.");
                            }

                            Scenario scenario = Scenario.THREE_SCENARIOS[scenarioIdx];
                            String[] paramNames = scenario.listVariableParameters();

                            for(String param : paramNames) {
                                Double d = data.optDouble(param, (Double) scenario.getDefaultParameterValue(param));
                                if (d < (double)0.0F) {
                                    throw new AnimoException("Reaction " + edgeName + " with parameter " + param + " = " + Utilities.roundToSignificantFigures(d, 4) + " < 0.\n" + "ANIMO" + " accepts only STRICTLY POSITIVE parameter values: please change it accordingly.");
                                }

                                r.let(param).be(d);
                            }

                            HashMap<String, Object> scenarioParameterValues = new HashMap();

                            for(int j = 0; j < paramNames.length; ++j) {
                                Double parVal = (Double)r.get(paramNames[j]).as(Double.class);
                                if (parVal != null) {
                                    scenario.setParameter(paramNames[j], parVal);
                                    scenarioParameterValues.put(paramNames[j], parVal);
                                }
                            }

                            r.let("SCENARIO_CFG").be(new ScenarioCfg(scenarioIdx, scenarioParameterValues));
                            Boolean sourceEnabled = (Boolean)sourceNode.get("enabled").as(Boolean.class);
                            Boolean targetEnabled = (Boolean)targetNode.get("enabled").as(Boolean.class);
                            switch (scenarioIdx) {
                                case 0:
                                case 1:
                                    if (!sourceEnabled && targetEnabled) {
                                        throw new AnimoException("Please check that reactant \"" + sourceName + "\" is enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
                                    }

                                    if (!sourceEnabled && !targetEnabled) {
                                        throw new AnimoException("Please check that both reactants \"" + sourceName + "\" and \"" + targetName + "\" are enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
                                    }

                                    if (sourceEnabled && !targetEnabled) {
                                        throw new AnimoException("Please check that reactant \"" + targetName + "\" is enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
                                    }
                                    break;
                                case 2:
                                    String e1Id = (String)r.get("_REACTANT_E1").as(String.class);
                                    String e2Id = (String)r.get("_REACTANT_E2").as(String.class);
                                    Boolean e1Enabled = (Boolean)model.getReactant((String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(e1Id))).get("enabled").as(Boolean.class);
                                    Boolean e2Enabled = (Boolean)model.getReactant((String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(e2Id))).get("enabled").as(Boolean.class);
                                    String e1Name = (String)model.getReactant((String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(e1Id))).get("canonicalName").as(String.class);
                                    String e2Name = (String)model.getReactant((String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(e2Id))).get("canonicalName").as(String.class);
                                    if (!e1Enabled && e2Enabled && targetEnabled) {
                                        throw new AnimoException("Please check that reactant \"" + e1Name + "\" is enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
                                    }

                                    if (!e1Enabled && !e2Enabled && targetEnabled) {
                                        throw new AnimoException("Please check that both reactants \"" + e1Name + "\" and \"" + e2Name + "\" are enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
                                    }

                                    if (e1Enabled && !e2Enabled && targetEnabled) {
                                        throw new AnimoException("Please check that reactant \"" + e2Name + "\" is enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
                                    }

                                    if (!e1Enabled && e2Enabled && !targetEnabled) {
                                        throw new AnimoException("Please check that both reactants \"" + e1Name + "\" and \"" + targetName + "\" are enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
                                    }

                                    if (!e1Enabled && !e2Enabled && !targetEnabled) {
                                        throw new AnimoException("Please check that reactants \"" + e1Name + "\", \"" + e2Name + "\" and \"" + targetName + "\" are enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
                                    }

                                    if (e1Enabled && !e2Enabled && !targetEnabled) {
                                        throw new AnimoException("Please check that both reactants \"" + e2Name + "\" and \"" + targetName + "\" are enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
                                    }

                                    if (e1Enabled && e2Enabled && !targetEnabled) {
                                        throw new AnimoException("Please check that reactant \"" + targetName + "\" is enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
                                    }
                            }

                            String reactant = (String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(data.getString("target")));
                            r.let("reactant").be(reactant);
                            String catalyst = (String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(data.getString("source")));
                            r.let("catalyst").be(catalyst);
                            int nLevelsR1;
                            if (!targetNode.get("levels").isNull()) {
                                nLevelsR1 = (Integer)targetNode.get("levels").as(Integer.class);
                            } else {
                                nLevelsR1 = (Integer)model.getProperties().get("levels").as(Integer.class);
                            }

                            int nLevelsR2;
                            if (!sourceNode.get("levels").isNull()) {
                                nLevelsR2 = (Integer)sourceNode.get("levels").as(Integer.class);
                            } else {
                                nLevelsR2 = (Integer)model.getProperties().get("levels").as(Integer.class);
                            }

                            if (scenarioIdx == 2) {
                                String cata = (String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(data.getString("_REACTANT_E1")));
                                String reac = (String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(data.getString("_REACTANT_E2")));
                                r.let("catalyst").be(cata);
                                r.let("reactant").be(reac);
                                if (!model.getReactant(cata).get("levels").isNull()) {
                                    nLevelsR1 = (Integer)model.getReactant(cata).get("levels").as(Integer.class);
                                } else {
                                    nLevelsR1 = (Integer)model.getProperties().get("levels").as(Integer.class);
                                }

                                if (!model.getReactant(reac).get("levels").isNull()) {
                                    nLevelsR2 = (Integer)model.getReactant(reac).get("levels").as(Integer.class);
                                } else {
                                    nLevelsR2 = (Integer)model.getProperties().get("levels").as(Integer.class);
                                }

                                String out = (String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(data.getString("target")));
                                r.let("output reactant").be(out);
                            } else {
                                r.let("output reactant").be(reactant);
                            }

                            String r1Id = (String)r.get("catalyst").as(String.class);
                            String r2Id = (String)r.get("reactant").as(String.class);
                            String rOutput = (String)r.get("output reactant").as(String.class);
                            r.setId(r1Id + "_" + r2Id + (rOutput.equals(r2Id) ? "" : "_" + rOutput));
                            boolean activeR1 = true;
                            boolean activeR2 = false;
                            boolean reactant1IsDownstream = false;
                            boolean reactant2IsDownstream = true;
                            if (scenarioIdx != 0 && scenarioIdx != 1) {
                                if (scenarioIdx == 2) {
                                    reactant1IsDownstream = ((String)r.get("catalyst").as(String.class)).equals(r.get("output reactant").as(String.class));
                                    reactant2IsDownstream = ((String)r.get("reactant").as(String.class)).equals(r.get("output reactant").as(String.class));
                                } else {
                                    activeR2 = true;
                                    activeR1 = true;
                                }
                            } else {
                                activeR1 = true;
                                if ((Integer)r.get("increment").as(Integer.class) >= 0) {
                                    activeR2 = false;
                                } else {
                                    activeR2 = true;
                                }
                            }

                            r.let("r1IsDownstream").be(reactant1IsDownstream);
                            r.let("r2IsDownstream").be(reactant2IsDownstream);
                            double nLevelsCatalyst = ((Integer)model.getReactant(catalyst).get("levels").as(Integer.class)).doubleValue();
                            double nLevelsReactant = ((Integer)model.getReactant(reactant).get("levels").as(Integer.class)).doubleValue();
                            double levelsScaleFactor;
                            switch (scenarioIdx) {
                                case 0:
                                    levelsScaleFactor = (double)1.0F / nLevelsReactant * nLevelsCatalyst;
                                    break;
                                case 1:
                                    levelsScaleFactor = (double)1.0F * nLevelsCatalyst;
                                    break;
                                case 2:
                                    String e1Id = (String)r.get("_REACTANT_E1").as(String.class);
                                    String e2Id = (String)r.get("_REACTANT_E2").as(String.class);
                                    double nLevelsE1 = ((Integer)model.getReactant((String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(e1Id))).get("levels").as(Integer.class)).doubleValue();
                                    double nLevelsE2 = ((Integer)model.getReactant((String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(e2Id))).get("levels").as(Integer.class)).doubleValue();
                                    levelsScaleFactor = (double)1.0F / nLevelsReactant * nLevelsE1 * nLevelsE2;
                                    break;
                                default:
                                    levelsScaleFactor = (double)1.0F;
                            }

                            r.let("levels scale factor").be(levelsScaleFactor);
                            Double maxValueFormula = Double.POSITIVE_INFINITY;
                            int rowMax;
                            int incrementColMax;
                            int incrementRowMax;
                            int colMin;
                            int rowMin;
                            int colMax;
                            if (activeR1 && !activeR2) {
                                colMax = 0;
                                rowMax = nLevelsR2;
                                incrementColMax = 1;
                                incrementRowMax = -1;
                                colMin = nLevelsR1;
                                rowMin = 0;
                            } else if (activeR1 && activeR2) {
                                colMax = 0;
                                rowMax = 0;
                                incrementColMax = 1;
                                incrementRowMax = 1;
                                colMin = nLevelsR1;
                                rowMin = nLevelsR2;
                            } else if (!activeR1 && !activeR2) {
                                colMax = nLevelsR1;
                                rowMax = nLevelsR2;
                                incrementColMax = -1;
                                incrementRowMax = -1;
                                colMin = 0;
                                rowMin = 0;
                            } else if (!activeR1 && activeR2) {
                                colMax = nLevelsR1;
                                rowMax = 0;
                                incrementColMax = -1;
                                incrementRowMax = 1;
                                colMin = 0;
                                rowMin = nLevelsR2;
                            } else {
                                incrementRowMax = 1;
                                incrementColMax = 1;
                                rowMin = 1;
                                colMin = 1;
                                rowMax = 1;
                                colMax = 1;
                            }

                            Double minValueFormula;
                            for(minValueFormula = scenario.computeFormula(colMin, nLevelsR1, activeR1, rowMin, nLevelsR2, activeR2); Double.isInfinite(maxValueFormula) && colMax >= 0 && colMax <= nLevelsR1 && rowMax >= 0 && rowMax <= nLevelsR2; maxValueFormula = scenario.computeFormula(colMax, nLevelsR1, activeR1, rowMax, nLevelsR2, activeR2)) {
                                colMax += incrementColMax;
                                rowMax += incrementRowMax;
                            }

                            int minValueInTables;
                            if (Double.isInfinite(minValueFormula)) {
                                minValueInTables = -1;
                            } else if (uncertainty == (double)0.0F) {
                                minValueInTables = Math.max(0, (int)Math.round(timeScaleFactor * levelsScaleFactor * minValueFormula));
                            } else {
                                minValueInTables = Math.max(0, (int)Math.round(timeScaleFactor * levelsScaleFactor * minValueFormula * ((double)1.0F - uncertainty / (double)100.0F)));
                            }

                            int maxValueInTables;
                            if (Double.isInfinite(maxValueFormula)) {
                                maxValueInTables = -1;
                            } else if (uncertainty == (double)0.0F) {
                                maxValueInTables = Math.max(0, (int)Math.round(timeScaleFactor * levelsScaleFactor * maxValueFormula));
                            } else {
                                maxValueInTables = Math.max(0, (int)Math.round(timeScaleFactor * levelsScaleFactor * maxValueFormula * ((double)1.0F + uncertainty / (double)100.0F)));
                            }

                            r.let("minTime").be(minValueInTables);
                            r.let("maxTime").be(maxValueInTables);
                            if (minValueInTables != -1 && (minTimeModel == Integer.MAX_VALUE || minValueInTables < minTimeModel)) {
                                minTimeModel = minValueInTables;
                            }

                            if (maxValueInTables != -1 && (maxTimeModel == Integer.MIN_VALUE || maxValueInTables > maxTimeModel)) {
                                maxTimeModel = maxValueInTables;
                            }

                            String stringId = data.getString("id");
                            long longId = Long.parseLong(stringId.replaceAll("\\D+", ""));
                            edgeJSONIDtoSUID.put(stringId, longId);
                            edgeSUIDToModelId.put(longId, r.getId());
                            model.add(r);
                        }
                    }
                }
            }

            double minTime = Double.POSITIVE_INFINITY;
            double maxTime = Double.NEGATIVE_INFINITY;
            double minTimeKValue = (double)0.0F;
            double maxTimeKValue = (double)0.0F;
            String minTimeReactionName = "";
            String maxTimeReactionName = "";

            for(Reaction r : model.getReactionCollection()) {
                Boolean enabled = (Boolean)r.get("enabled").as(Boolean.class);
                if (enabled) {
                    String reactionName = (String)r.get("canonicalName").as(String.class);
                    Integer scenarioIdx = (Integer)r.get("scenario").as(Integer.class);
                    Scenario scenario = Scenario.THREE_SCENARIOS[scenarioIdx];
                    String[] paramNames = scenario.listVariableParameters();
                    double levelsScaleFactor = (Double)r.get("levels scale factor").as(Double.class);
                    double scaleFactor = levelsScaleFactor * timeScaleFactor;

                    for(String param : paramNames) {
                        Double k = (Double)r.get(param).as(Double.class) / scaleFactor;
                        scenario.setParameter(param, k);
                    }

                    int increment = (Integer)r.get("increment").as(Integer.class);
                    boolean r1Active = true;
                    boolean r2Active = false;
                    Reactant r1 = model.getReactant((String)r.get("catalyst").as(String.class));
                    Reactant r2 = model.getReactant((String)r.get("reactant").as(String.class));
                    int r1Levels;
                    int r2Levels;
                    switch (scenarioIdx) {
                        case 0:
                        case 1:
                            if (increment < 0) {
                                r2Active = true;
                            }

                            r1Levels = (Integer)r1.get("levels").as(Integer.class);
                            r2Levels = (Integer)r2.get("levels").as(Integer.class);
                            break;
                        case 2:
                            r1Active = (Boolean)r.get("_REACTANT_ACT_E1").as(Boolean.class);
                            r2Active = (Boolean)r.get("_REACTANT_ACT_E2").as(Boolean.class);
                            r1Levels = (Integer)r1.get("levels").as(Integer.class);
                            r2Levels = (Integer)r2.get("levels").as(Integer.class);
                            break;
                        default:
                            r2Levels = 100;
                            r1Levels = 100;
                    }

                    int cMin;
                    int cMax;
                    int incCMin;
                    int incCMax;
                    if (r1Active) {
                        cMin = r1Levels;
                        incCMin = -1;
                        cMax = 0;
                        incCMax = 1;
                    } else {
                        cMin = 0;
                        incCMin = 1;
                        cMax = r1Levels;
                        incCMax = -1;
                    }

                    int rMax;
                    int incRMin;
                    int incRMax;
                    int rMin;
                    if (r2Active) {
                        rMin = r2Levels;
                        incRMin = -1;
                        rMax = 0;
                        incRMax = 1;
                    } else {
                        rMin = 0;
                        incRMin = 1;
                        rMax = r2Levels;
                        incRMax = -1;
                    }

                    double fMin = Double.POSITIVE_INFINITY;

                    double fMax;
                    for(fMax = Double.POSITIVE_INFINITY; Double.isInfinite(fMin) && cMin >= 0 && cMin <= r1Levels && rMin >= 0 && rMin <= r2Levels; rMin += incRMin) {
                        fMin = scenario.computeFormula(cMin, r1Levels, r1Active, rMin, r2Levels, r2Active);
                        cMin += incCMin;
                    }

                    while(Double.isInfinite(fMax) && cMax >= 0 && cMax <= r1Levels && rMax >= 0 && rMax <= r2Levels) {
                        fMax = scenario.computeFormula(cMax, r1Levels, r1Active, rMax, r2Levels, r2Active);
                        cMax += incCMax;
                        rMax += incRMax;
                    }

                    if (!Double.isInfinite(fMin)) {
                        double tMin = fMin * ((double)1.0F - uncertainty / (double)100.0F);
                        if (tMin < minTime) {
                            minTime = tMin;
                            minTimeReactionName = reactionName;
                            String[] params = scenario.listVariableParameters();
                            if (params.length > 0) {
                                minTimeKValue = (double) scenario.getParameter(params[0]);
                            }
                        }
                    }

                    if (!Double.isInfinite(fMax)) {
                        double tMax = fMax * ((double)1.0F + uncertainty / (double)100.0F);
                        if (tMax > maxTime) {
                            maxTime = tMax;
                            maxTimeReactionName = reactionName;
                            String[] params = scenario.listVariableParameters();
                            if (params.length > 0) {
                                maxTimeKValue = (double) scenario.getParameter(params[0]);
                            }
                        }
                    }
                }
            }

            double timeTo;
            if (nMinutesToSimulate != null) {
                timeTo = nMinutesToSimulate * (double)60.0F / secondsPerPoint;
            } else {
                timeTo = (double)14400.0F / secondsPerPoint;
            }

            if (minTime < (double)10.0F) {
                secondsPerPoint = minTime * secondsPerPoint / Model.DIVISORE_MIN;
                minTime = minTime / timeScaleFactor / secondsPerPoint;
                maxTime = maxTime / timeScaleFactor / secondsPerPoint;
                timeTo = timeTo / timeScaleFactor / secondsPerPoint;
                timeScaleFactor = (double)1.0F / secondsPerPoint;
            }

            if (maxTime > 1.073741822E9) {
                secondsPerPoint = maxTime * secondsPerPoint / (1.073741822E9 / Model.DIVISORE_MAX);
                minTime = minTime / timeScaleFactor / secondsPerPoint;
                maxTime = maxTime / timeScaleFactor / secondsPerPoint;
                timeTo = timeTo / timeScaleFactor / secondsPerPoint;
                timeScaleFactor = (double)1.0F / secondsPerPoint;
            }

            if (timeTo > 1.073741822E9) {
                secondsPerPoint = timeTo * secondsPerPoint / 1.073741822E9;
                minTime = minTime / timeScaleFactor / secondsPerPoint;
                maxTime = maxTime / timeScaleFactor / secondsPerPoint;
                timeTo = timeTo / timeScaleFactor / secondsPerPoint;
                timeScaleFactor = (double)1.0F / secondsPerPoint;
            }

            if (minTime < (double)1.0F) {
                String finalMinTimeReactionName = minTimeReactionName;
                double finalMinTimeKValue = minTimeKValue;
                String finalMaxTimeReactionName = maxTimeReactionName;
                double finalMaxTimeKValue = maxTimeKValue;
                String finalMaxTimeReactionName1 = maxTimeReactionName;
                String finalMinTimeReactionName1 = minTimeReactionName;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "The difference between the fastest reaction (" + finalMinTimeReactionName + ", parameter K = " + finalMinTimeKValue + ")\nand the slowest reaction (" + finalMaxTimeReactionName + ", parameter K = " + finalMaxTimeKValue + ")\nmay be too large to be properly represented. We advise to reduce such difference\nby either increasing K for " + finalMaxTimeReactionName1 + " or decreasing K for " + finalMinTimeReactionName1 + ".", "Parameter space too broad", 2);
                    }
                });
            }

            timeScaleFactor = (double)1.0F / secondsPerPoint;
            model.getProperties().let("time scale factor").be(timeScaleFactor);
            if (minTimeModel == Integer.MAX_VALUE) {
                minTimeModel = -1;
            }

            model.getProperties().let("minTime").be(minTimeModel);
            if (maxTimeModel == Integer.MIN_VALUE) {
                maxTimeModel = -1;
            }

            model.getProperties().let("maxTime").be(maxTimeModel);
            model.setMapCytoscapeIDtoReactantID(nodeSUIDToModelId);
            // TODO check if not still required
//            model.setMapCytoscapeIDtoReactionID(edgeSUIDToModelId);
            return model;
        }
    }

    public static Model getModelFromJson(CytoscapeModel object, Integer nMinutesToSimulate) throws JSONException, AnimoException {
        Model model = new Model();
        Map<Long, String> nodeSUIDToModelId = new HashMap();
        Map<Long, String> edgeSUIDToModelId = new HashMap();
        Map<String, Long> nodeJSONIDtoSUID = new HashMap();
        Map<String, Long> edgeJSONIDtoSUID = new HashMap();
        JSONObject modelData = object.getJSONObject("data");

        for(String field : JSONObject.getNames(modelData)) {
            Object value = JSONObject.stringToValue(modelData.get(field).toString());
            model.getProperties().let(field).setValue(value);
        }

        JSONObject elements = object.getJSONObject("elements");
        HashMap<String, String> nameToID = new HashMap();
        Double secondsPerPoint = modelData.optDouble("seconds per point", modelData.optDouble("seconds_per_point", (double)1.0F));
        double timeScaleFactor = modelData.optDouble("time scale factor", (double)1.0F / secondsPerPoint);

        model.getProperties().let("time scale factor").be(timeScaleFactor);
        model.getProperties().let("seconds per point").be(secondsPerPoint);
        boolean noReactantsPlotted = true;
        int maxNumberOfLevels = 0;
        int defaultNumberOfLevels = 100;
        JSONArray nodes = elements.getJSONArray("nodes");
        List<JSONObject> nodesList = new ArrayList();

        for(int i = 0; i < nodes.length(); ++i) {
            JSONObject n = nodes.getJSONObject(i);
            nodesList.add(n);
        }

        Collections.sort(nodesList, new Comparator<JSONObject>() {
            public int compare(JSONObject n1, JSONObject n2) {
                try {
                    n1 = n1.getJSONObject("data");
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }

                try {
                    n2 = n2.getJSONObject("data");
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }

                String name1 = n1.optString("canonicalName", n1.optString("name", ""));
                String name2 = n2.optString("canonicalName", n2.optString("name", ""));
                return name1.compareTo(name2);
            }
        });
        nodes = new JSONArray(nodesList);

        for(int i = 0; i < nodes.length(); ++i) {
            JSONObject data = nodes.getJSONObject(i).getJSONObject("data");
            String stringId = data.getString("id");
            long longId = Long.parseLong(stringId.replaceAll("\\D+", ""));
            String reactantId = "n" + stringId;
            Reactant r = new Reactant(reactantId);
            nodeSUIDToModelId.put(longId, reactantId);
            nodeJSONIDtoSUID.put(stringId, longId);
            boolean enabled = data.optBoolean("enabled", true);
            boolean plotted = data.optBoolean("plotted", true);
            if (plotted && enabled) {
                noReactantsPlotted = false;
            }

            r.let("enabled").be(enabled);
            r.let("plotted").be(plotted);
            r.let("canonicalName").be(data.optString("canonicalName", data.optString("name", "")));
            r.let("alias").be(r.get("canonicalName").as(String.class));
            int nLevels = data.optInt("levels", 100);
            if (nLevels > maxNumberOfLevels) {
                maxNumberOfLevels = nLevels;
            }

            r.let("levels").be(nLevels);
            r.let("initialConcentration").be(data.optInt("initialConcentration", 0));
            r.let("randomInitialConcentration").be(data.optBoolean("randomInitialConcentration", false));
            r.let("randomInitialConcentrationMinimum").be(data.optInt("randomInitialConcentrationMinimum", 0));
            r.let("randomInitialConcentrationMaximum").be(data.optInt("randomInitialConcentrationMaximum", nLevels));
            r.let("randomInitialConcentrationStep").be(data.optInt("randomInitialConcentrationStep", 1));
            nameToID.put(data.getString("name"), r.getId());
            if (enabled) {
                model.add(r);
            }
        }

        if (noReactantsPlotted) {
            throw new AnimoException("No nodes were marked as plotted. No graph would be shown.\nPlease mark at least one (enabled) node as plotted.");
        } else {
            model.getProperties().let("levels").be(maxNumberOfLevels);
            int minTimeModel = Integer.MAX_VALUE;
            int maxTimeModel = Integer.MIN_VALUE;
            double uncertainty = (double)0.0F;

            // TODO marien fix config (from properties or input)
//            try {
//                uncertainty = (double)Integer.valueOf(AnimoBackend.get().configuration().get("/ANIMO/Uncertainty"));
//            } catch (NumberFormatException var75) {
//                uncertainty = (double)0.0F;
//            }

            JSONArray edges = elements.getJSONArray("edges");

            for(int i = 0; i < edges.length(); ++i) {
                JSONObject data = edges.getJSONObject(i).getJSONObject("data");
                String reactionId = "reaction" + i;
                Reaction r = new Reaction(reactionId);
                boolean enabled = data.optBoolean("enabled", true);
                r.let("enabled").be(enabled);
                if (enabled && !data.optString("source", "-1").equals("-1") && !data.optString("target", "-1").equals("-1")) {
                    String sourceID = data.getString("source");
                    String targetID = data.getString("target");
                    if (nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(sourceID)) != null && nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(targetID)) != null) {
                        Reactant sourceNode = model.getReactant((String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(sourceID)));
                        Reactant targetNode = model.getReactant((String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(targetID)));
                        if (sourceNode != null && targetNode != null) {
                            int increment = data.optInt("increment", 0);
                            if (increment == 0) {
                                if (data.getLong("source") == data.getLong("target")) {
                                    increment = -1;
                                } else {
                                    increment = 1;
                                }
                            }

                            r.let("increment").be(increment);
                            int scenarioIdx = data.optInt("scenario", 0);
                            r.let("scenario").be(scenarioIdx);
                            if (scenarioIdx == 2) {
                                r.let("_REACTANT_E1").be(data.optString("_REACTANT_E1", data.getString("source")));
                                r.let("_REACTANT_ACT_E1").be(data.optBoolean("_REACTANT_ACT_E1", true));
                                r.let("_REACTANT_E2").be(data.optString("_REACTANT_E2", data.getString("target")));
                                r.let("_REACTANT_ACT_E2").be(data.optBoolean("_REACTANT_ACT_E2", true));
                            }

                            String sourceName = (String)sourceNode.get("canonicalName").as(String.class);
                            String targetName = (String)targetNode.get("canonicalName").as(String.class);
                            String edgeName = data.optString("canonicalName", "");
                            if (edgeName.equals("")) {
                                if (!sourceName.equals("") && !targetName.equals("")) {
                                    StringBuilder nameBuilder = new StringBuilder();
                                    String min1 = "-1";
                                    String idE1 = data.optString("_REACTANT_E1", min1);
                                    String idE2 = data.optString("_REACTANT_E2", min1);
                                    if (scenarioIdx == 2 && !idE1.equals(min1) && !idE2.equals(min1)) {
                                        nameBuilder.append((String)model.getReactant((String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(idE1))).get("canonicalName").as(String.class));
                                        nameBuilder.append(" AND ");
                                        nameBuilder.append((String)model.getReactant((String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(idE2))).get("canonicalName").as(String.class));
                                    } else {
                                        nameBuilder.append(sourceName);
                                        nameBuilder.append(increment >= 0 ? " --> " : " --| ");
                                        nameBuilder.append(targetName);
                                    }

                                    edgeName = nameBuilder.toString();
                                } else {
                                    edgeName = "New Interaction";
                                }
                            }

                            r.let("canonicalName").be(edgeName);
                            r.let("alias").be(edgeName);
                            if (scenarioIdx < 0 || scenarioIdx >= Scenario.THREE_SCENARIOS.length) {
                                scenarioIdx = 0;
                                throw new AnimoException("The reaction " + edgeName + " has an invalid scenario setting (" + scenarioIdx + "). Now I set it to the first available: please set the correct parameters.");
                            }

                            Scenario scenario = Scenario.THREE_SCENARIOS[scenarioIdx];
                            String[] paramNames = scenario.listVariableParameters();

                            for(String param : paramNames) {
                                Double d = data.optDouble(param, (Double) scenario.getDefaultParameterValue(param));
                                if (d < (double)0.0F) {
                                    throw new AnimoException("Reaction " + edgeName + " with parameter " + param + " = " + Utilities.roundToSignificantFigures(d, 4) + " < 0.\n" + "ANIMO" + " accepts only STRICTLY POSITIVE parameter values: please change it accordingly.");
                                }

                                r.let(param).be(d);
                            }

                            HashMap<String, Object> scenarioParameterValues = new HashMap();

                            for(int j = 0; j < paramNames.length; ++j) {
                                Double parVal = (Double)r.get(paramNames[j]).as(Double.class);
                                if (parVal != null) {
                                    scenario.setParameter(paramNames[j], parVal);
                                    scenarioParameterValues.put(paramNames[j], parVal);
                                }
                            }

                            r.let("SCENARIO_CFG").be(new ScenarioCfg(scenarioIdx, scenarioParameterValues));
                            Boolean sourceEnabled = (Boolean)sourceNode.get("enabled").as(Boolean.class);
                            Boolean targetEnabled = (Boolean)targetNode.get("enabled").as(Boolean.class);
                            switch (scenarioIdx) {
                                case 0:
                                case 1:
                                    if (!sourceEnabled && targetEnabled) {
                                        throw new AnimoException("Please check that reactant \"" + sourceName + "\" is enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
                                    }

                                    if (!sourceEnabled && !targetEnabled) {
                                        throw new AnimoException("Please check that both reactants \"" + sourceName + "\" and \"" + targetName + "\" are enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
                                    }

                                    if (sourceEnabled && !targetEnabled) {
                                        throw new AnimoException("Please check that reactant \"" + targetName + "\" is enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
                                    }
                                    break;
                                case 2:
                                    String e1Id = (String)r.get("_REACTANT_E1").as(String.class);
                                    String e2Id = (String)r.get("_REACTANT_E2").as(String.class);
                                    Boolean e1Enabled = (Boolean)model.getReactant((String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(e1Id))).get("enabled").as(Boolean.class);
                                    Boolean e2Enabled = (Boolean)model.getReactant((String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(e2Id))).get("enabled").as(Boolean.class);
                                    String e1Name = (String)model.getReactant((String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(e1Id))).get("canonicalName").as(String.class);
                                    String e2Name = (String)model.getReactant((String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(e2Id))).get("canonicalName").as(String.class);
                                    if (!e1Enabled && e2Enabled && targetEnabled) {
                                        throw new AnimoException("Please check that reactant \"" + e1Name + "\" is enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
                                    }

                                    if (!e1Enabled && !e2Enabled && targetEnabled) {
                                        throw new AnimoException("Please check that both reactants \"" + e1Name + "\" and \"" + e2Name + "\" are enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
                                    }

                                    if (e1Enabled && !e2Enabled && targetEnabled) {
                                        throw new AnimoException("Please check that reactant \"" + e2Name + "\" is enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
                                    }

                                    if (!e1Enabled && e2Enabled && !targetEnabled) {
                                        throw new AnimoException("Please check that both reactants \"" + e1Name + "\" and \"" + targetName + "\" are enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
                                    }

                                    if (!e1Enabled && !e2Enabled && !targetEnabled) {
                                        throw new AnimoException("Please check that reactants \"" + e1Name + "\", \"" + e2Name + "\" and \"" + targetName + "\" are enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
                                    }

                                    if (e1Enabled && !e2Enabled && !targetEnabled) {
                                        throw new AnimoException("Please check that both reactants \"" + e2Name + "\" and \"" + targetName + "\" are enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
                                    }

                                    if (e1Enabled && e2Enabled && !targetEnabled) {
                                        throw new AnimoException("Please check that reactant \"" + targetName + "\" is enabled, or reaction \"" + edgeName + "\" cannot stay enabled.");
                                    }
                            }

                            String reactant = (String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(data.getString("target")));
                            r.let("reactant").be(reactant);
                            String catalyst = (String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(data.getString("source")));
                            r.let("catalyst").be(catalyst);
                            int nLevelsR1;
                            if (!targetNode.get("levels").isNull()) {
                                nLevelsR1 = (Integer)targetNode.get("levels").as(Integer.class);
                            } else {
                                nLevelsR1 = (Integer)model.getProperties().get("levels").as(Integer.class);
                            }

                            int nLevelsR2;
                            if (!sourceNode.get("levels").isNull()) {
                                nLevelsR2 = (Integer)sourceNode.get("levels").as(Integer.class);
                            } else {
                                nLevelsR2 = (Integer)model.getProperties().get("levels").as(Integer.class);
                            }

                            if (scenarioIdx == 2) {
                                String cata = (String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(data.getString("_REACTANT_E1")));
                                String reac = (String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(data.getString("_REACTANT_E2")));
                                r.let("catalyst").be(cata);
                                r.let("reactant").be(reac);
                                if (!model.getReactant(cata).get("levels").isNull()) {
                                    nLevelsR1 = (Integer)model.getReactant(cata).get("levels").as(Integer.class);
                                } else {
                                    nLevelsR1 = (Integer)model.getProperties().get("levels").as(Integer.class);
                                }

                                if (!model.getReactant(reac).get("levels").isNull()) {
                                    nLevelsR2 = (Integer)model.getReactant(reac).get("levels").as(Integer.class);
                                } else {
                                    nLevelsR2 = (Integer)model.getProperties().get("levels").as(Integer.class);
                                }

                                String out = (String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(data.getString("target")));
                                r.let("output reactant").be(out);
                            } else {
                                r.let("output reactant").be(reactant);
                            }

                            String r1Id = (String)r.get("catalyst").as(String.class);
                            String r2Id = (String)r.get("reactant").as(String.class);
                            String rOutput = (String)r.get("output reactant").as(String.class);
                            r.setId(r1Id + "_" + r2Id + (rOutput.equals(r2Id) ? "" : "_" + rOutput));
                            boolean activeR1 = true;
                            boolean activeR2 = false;
                            boolean reactant1IsDownstream = false;
                            boolean reactant2IsDownstream = true;
                            if (scenarioIdx != 0 && scenarioIdx != 1) {
                                if (scenarioIdx == 2) {
                                    reactant1IsDownstream = ((String)r.get("catalyst").as(String.class)).equals(r.get("output reactant").as(String.class));
                                    reactant2IsDownstream = ((String)r.get("reactant").as(String.class)).equals(r.get("output reactant").as(String.class));
                                } else {
                                    activeR2 = true;
                                    activeR1 = true;
                                }
                            } else {
                                activeR1 = true;
                                if ((Integer)r.get("increment").as(Integer.class) >= 0) {
                                    activeR2 = false;
                                } else {
                                    activeR2 = true;
                                }
                            }

                            r.let("r1IsDownstream").be(reactant1IsDownstream);
                            r.let("r2IsDownstream").be(reactant2IsDownstream);
                            double nLevelsCatalyst = ((Integer)model.getReactant(catalyst).get("levels").as(Integer.class)).doubleValue();
                            double nLevelsReactant = ((Integer)model.getReactant(reactant).get("levels").as(Integer.class)).doubleValue();
                            double levelsScaleFactor;
                            switch (scenarioIdx) {
                                case 0:
                                    levelsScaleFactor = (double)1.0F / nLevelsReactant * nLevelsCatalyst;
                                    break;
                                case 1:
                                    levelsScaleFactor = (double)1.0F * nLevelsCatalyst;
                                    break;
                                case 2:
                                    String e1Id = (String)r.get("_REACTANT_E1").as(String.class);
                                    String e2Id = (String)r.get("_REACTANT_E2").as(String.class);
                                    double nLevelsE1 = ((Integer)model.getReactant((String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(e1Id))).get("levels").as(Integer.class)).doubleValue();
                                    double nLevelsE2 = ((Integer)model.getReactant((String)nodeSUIDToModelId.get(nodeJSONIDtoSUID.get(e2Id))).get("levels").as(Integer.class)).doubleValue();
                                    levelsScaleFactor = (double)1.0F / nLevelsReactant * nLevelsE1 * nLevelsE2;
                                    break;
                                default:
                                    levelsScaleFactor = (double)1.0F;
                            }

                            r.let("levels scale factor").be(levelsScaleFactor);
                            Double maxValueFormula = Double.POSITIVE_INFINITY;
                            int rowMax;
                            int incrementColMax;
                            int incrementRowMax;
                            int colMin;
                            int rowMin;
                            int colMax;
                            if (activeR1 && !activeR2) {
                                colMax = 0;
                                rowMax = nLevelsR2;
                                incrementColMax = 1;
                                incrementRowMax = -1;
                                colMin = nLevelsR1;
                                rowMin = 0;
                            } else if (activeR1 && activeR2) {
                                colMax = 0;
                                rowMax = 0;
                                incrementColMax = 1;
                                incrementRowMax = 1;
                                colMin = nLevelsR1;
                                rowMin = nLevelsR2;
                            } else if (!activeR1 && !activeR2) {
                                colMax = nLevelsR1;
                                rowMax = nLevelsR2;
                                incrementColMax = -1;
                                incrementRowMax = -1;
                                colMin = 0;
                                rowMin = 0;
                            } else if (!activeR1 && activeR2) {
                                colMax = nLevelsR1;
                                rowMax = 0;
                                incrementColMax = -1;
                                incrementRowMax = 1;
                                colMin = 0;
                                rowMin = nLevelsR2;
                            } else {
                                incrementRowMax = 1;
                                incrementColMax = 1;
                                rowMin = 1;
                                colMin = 1;
                                rowMax = 1;
                                colMax = 1;
                            }

                            Double minValueFormula;
                            for(minValueFormula = scenario.computeFormula(colMin, nLevelsR1, activeR1, rowMin, nLevelsR2, activeR2); Double.isInfinite(maxValueFormula) && colMax >= 0 && colMax <= nLevelsR1 && rowMax >= 0 && rowMax <= nLevelsR2; maxValueFormula = scenario.computeFormula(colMax, nLevelsR1, activeR1, rowMax, nLevelsR2, activeR2)) {
                                colMax += incrementColMax;
                                rowMax += incrementRowMax;
                            }

                            int minValueInTables;
                            if (Double.isInfinite(minValueFormula)) {
                                minValueInTables = -1;
                            } else if (uncertainty == (double)0.0F) {
                                minValueInTables = Math.max(0, (int)Math.round(timeScaleFactor * levelsScaleFactor * minValueFormula));
                            } else {
                                minValueInTables = Math.max(0, (int)Math.round(timeScaleFactor * levelsScaleFactor * minValueFormula * ((double)1.0F - uncertainty / (double)100.0F)));
                            }

                            int maxValueInTables;
                            if (Double.isInfinite(maxValueFormula)) {
                                maxValueInTables = -1;
                            } else if (uncertainty == (double)0.0F) {
                                maxValueInTables = Math.max(0, (int)Math.round(timeScaleFactor * levelsScaleFactor * maxValueFormula));
                            } else {
                                maxValueInTables = Math.max(0, (int)Math.round(timeScaleFactor * levelsScaleFactor * maxValueFormula * ((double)1.0F + uncertainty / (double)100.0F)));
                            }

                            r.let("minTime").be(minValueInTables);
                            r.let("maxTime").be(maxValueInTables);
                            if (minValueInTables != -1 && (minTimeModel == Integer.MAX_VALUE || minValueInTables < minTimeModel)) {
                                minTimeModel = minValueInTables;
                            }

                            if (maxValueInTables != -1 && (maxTimeModel == Integer.MIN_VALUE || maxValueInTables > maxTimeModel)) {
                                maxTimeModel = maxValueInTables;
                            }

                            String stringId = data.getString("id");
                            long longId = Long.parseLong(stringId.replaceAll("\\D+", ""));
                            edgeJSONIDtoSUID.put(stringId, longId);
                            edgeSUIDToModelId.put(longId, r.getId());
                            model.add(r);
                        }
                    }
                }
            }

            double minTime = Double.POSITIVE_INFINITY;
            double maxTime = Double.NEGATIVE_INFINITY;
            double minTimeKValue = (double)0.0F;
            double maxTimeKValue = (double)0.0F;
            String minTimeReactionName = "";
            String maxTimeReactionName = "";

            for(Reaction r : model.getReactionCollection()) {
                Boolean enabled = (Boolean)r.get("enabled").as(Boolean.class);
                if (enabled) {
                    String reactionName = (String)r.get("canonicalName").as(String.class);
                    Integer scenarioIdx = (Integer)r.get("scenario").as(Integer.class);
                    Scenario scenario = Scenario.THREE_SCENARIOS[scenarioIdx];
                    String[] paramNames = scenario.listVariableParameters();
                    double levelsScaleFactor = (Double)r.get("levels scale factor").as(Double.class);
                    double scaleFactor = levelsScaleFactor * timeScaleFactor;

                    for(String param : paramNames) {
                        Double k = (Double)r.get(param).as(Double.class) / scaleFactor;
                        scenario.setParameter(param, k);
                    }

                    int increment = (Integer)r.get("increment").as(Integer.class);
                    boolean r1Active = true;
                    boolean r2Active = false;
                    Reactant r1 = model.getReactant((String)r.get("catalyst").as(String.class));
                    Reactant r2 = model.getReactant((String)r.get("reactant").as(String.class));
                    int r1Levels;
                    int r2Levels;
                    switch (scenarioIdx) {
                        case 0:
                        case 1:
                            if (increment < 0) {
                                r2Active = true;
                            }

                            r1Levels = (Integer)r1.get("levels").as(Integer.class);
                            r2Levels = (Integer)r2.get("levels").as(Integer.class);
                            break;
                        case 2:
                            r1Active = (Boolean)r.get("_REACTANT_ACT_E1").as(Boolean.class);
                            r2Active = (Boolean)r.get("_REACTANT_ACT_E2").as(Boolean.class);
                            r1Levels = (Integer)r1.get("levels").as(Integer.class);
                            r2Levels = (Integer)r2.get("levels").as(Integer.class);
                            break;
                        default:
                            r2Levels = 100;
                            r1Levels = 100;
                    }

                    int cMin;
                    int cMax;
                    int incCMin;
                    int incCMax;
                    if (r1Active) {
                        cMin = r1Levels;
                        incCMin = -1;
                        cMax = 0;
                        incCMax = 1;
                    } else {
                        cMin = 0;
                        incCMin = 1;
                        cMax = r1Levels;
                        incCMax = -1;
                    }

                    int rMax;
                    int incRMin;
                    int incRMax;
                    int rMin;
                    if (r2Active) {
                        rMin = r2Levels;
                        incRMin = -1;
                        rMax = 0;
                        incRMax = 1;
                    } else {
                        rMin = 0;
                        incRMin = 1;
                        rMax = r2Levels;
                        incRMax = -1;
                    }

                    double fMin = Double.POSITIVE_INFINITY;

                    double fMax;
                    for(fMax = Double.POSITIVE_INFINITY; Double.isInfinite(fMin) && cMin >= 0 && cMin <= r1Levels && rMin >= 0 && rMin <= r2Levels; rMin += incRMin) {
                        fMin = scenario.computeFormula(cMin, r1Levels, r1Active, rMin, r2Levels, r2Active);
                        cMin += incCMin;
                    }

                    while(Double.isInfinite(fMax) && cMax >= 0 && cMax <= r1Levels && rMax >= 0 && rMax <= r2Levels) {
                        fMax = scenario.computeFormula(cMax, r1Levels, r1Active, rMax, r2Levels, r2Active);
                        cMax += incCMax;
                        rMax += incRMax;
                    }

                    if (!Double.isInfinite(fMin)) {
                        double tMin = fMin * ((double)1.0F - uncertainty / (double)100.0F);
                        if (tMin < minTime) {
                            minTime = tMin;
                            minTimeReactionName = reactionName;
                            String[] params = scenario.listVariableParameters();
                            if (params.length > 0) {
                                minTimeKValue = (double) scenario.getParameter(params[0]);
                            }
                        }
                    }

                    if (!Double.isInfinite(fMax)) {
                        double tMax = fMax * ((double)1.0F + uncertainty / (double)100.0F);
                        if (tMax > maxTime) {
                            maxTime = tMax;
                            maxTimeReactionName = reactionName;
                            String[] params = scenario.listVariableParameters();
                            if (params.length > 0) {
                                maxTimeKValue = (double) scenario.getParameter(params[0]);
                            }
                        }
                    }
                }
            }

            double timeTo;
            if (nMinutesToSimulate != null) {
                timeTo = nMinutesToSimulate * (double)60.0F / secondsPerPoint;
            } else {
                timeTo = (double)14400.0F / secondsPerPoint;
            }

            if (minTime < (double)10.0F) {
                secondsPerPoint = minTime * secondsPerPoint / Model.DIVISORE_MIN;
                minTime = minTime / timeScaleFactor / secondsPerPoint;
                maxTime = maxTime / timeScaleFactor / secondsPerPoint;
                timeTo = timeTo / timeScaleFactor / secondsPerPoint;
                timeScaleFactor = (double)1.0F / secondsPerPoint;
            }

            if (maxTime > 1.073741822E9) {
                secondsPerPoint = maxTime * secondsPerPoint / (1.073741822E9 / Model.DIVISORE_MAX);
                minTime = minTime / timeScaleFactor / secondsPerPoint;
                maxTime = maxTime / timeScaleFactor / secondsPerPoint;
                timeTo = timeTo / timeScaleFactor / secondsPerPoint;
                timeScaleFactor = (double)1.0F / secondsPerPoint;
            }

            if (timeTo > 1.073741822E9) {
                secondsPerPoint = timeTo * secondsPerPoint / 1.073741822E9;
                minTime = minTime / timeScaleFactor / secondsPerPoint;
                maxTime = maxTime / timeScaleFactor / secondsPerPoint;
                timeTo = timeTo / timeScaleFactor / secondsPerPoint;
                timeScaleFactor = (double)1.0F / secondsPerPoint;
            }

            if (minTime < (double)1.0F) {
                String finalMinTimeReactionName = minTimeReactionName;
                double finalMinTimeKValue = minTimeKValue;
                String finalMaxTimeReactionName = maxTimeReactionName;
                double finalMaxTimeKValue = maxTimeKValue;
                String finalMaxTimeReactionName1 = maxTimeReactionName;
                String finalMinTimeReactionName1 = minTimeReactionName;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "The difference between the fastest reaction (" + finalMinTimeReactionName + ", parameter K = " + finalMinTimeKValue + ")\nand the slowest reaction (" + finalMaxTimeReactionName + ", parameter K = " + finalMaxTimeKValue + ")\nmay be too large to be properly represented. We advise to reduce such difference\nby either increasing K for " + finalMaxTimeReactionName1 + " or decreasing K for " + finalMinTimeReactionName1 + ".", "Parameter space too broad", 2);
                    }
                });
            }

            timeScaleFactor = (double)1.0F / secondsPerPoint;
            model.getProperties().let("time scale factor").be(timeScaleFactor);
            if (minTimeModel == Integer.MAX_VALUE) {
                minTimeModel = -1;
            }

            model.getProperties().let("minTime").be(minTimeModel);
            if (maxTimeModel == Integer.MIN_VALUE) {
                maxTimeModel = -1;
            }

            model.getProperties().let("maxTime").be(maxTimeModel);
            model.setMapCytoscapeIDtoReactantID(nodeSUIDToModelId);
            // TODO check if not still required
//            model.setMapCytoscapeIDtoReactionID(edgeSUIDToModelId);
            return model;
        }
    }

    public JSONObject resultToJSON(SimpleLevelResult result, Model model) throws JSONException {
        JSONObject json = new JSONObject();
        double tsf = (Double)model.getProperties().get("time scale factor").as(Double.class);

        for(Map.Entry<String, SortedMap<Double, Double>> r : result.getLevels().entrySet()) {
            if (model.getReactant((String)r.getKey()) != null) {
                JSONArray timePoints = new JSONArray();

                for(Map.Entry<Double, Double> entry : r.getValue().entrySet()) {
                    JSONObject timePoint = new JSONObject();
                    timePoint.put("x", (Double)entry.getKey() / (tsf * (double)60.0F));
                    timePoint.put("y", entry.getValue());
                    timePoints.put(timePoint);
                }

                json.put((String)r.getKey(), timePoints);
            }
        }

        return json;
    }

    public JSONObject parametersToJson(Map<Reaction, Map<String, Double>> reactionParameters) throws JSONException {
        JSONObject result = new JSONObject();

        for(Reaction r : reactionParameters.keySet()) {
            JSONArray paramPairs = new JSONArray();
            Map<String, Double> paramMap = (Map)reactionParameters.get(r);

            for(String param : paramMap.keySet()) {
                JSONObject parameter = new JSONObject();
                parameter.put(param, paramMap.get(param));
                paramPairs.put(parameter);
            }

            result.put(r.getId(), paramPairs);
        }

        return result;
    }
}
