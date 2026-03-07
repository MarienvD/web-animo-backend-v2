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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
