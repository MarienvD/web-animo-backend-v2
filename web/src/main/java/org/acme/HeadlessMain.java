package org.acme;

import animo.core.AnimoBackend;
import animo.core.analyser.AnalysisException;
import animo.core.analyser.uppaal.SimpleLevelResult;
import animo.core.model.Model;
import animo.exceptions.AnimoException;
import animo.util.XmlConfiguration;
import com.google.common.io.Files;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class HeadlessMain {
    private static final String configPath = "ANIMO_configuration.xml";
    public static final int MAX_ANALYZE_MILLIS;
    private static Random random = new Random();

    public static JSONObject executeFromRequest(ModelAnalyzer analyzer, JSONObject request, Model model) throws JSONException, AnalysisException, IOException {
        String analyzeType = request.optString("type", HeadlessMain.RequestType.SIMULATE.name);
        if (HeadlessMain.RequestType.SIMULATE.name.equals(analyzeType)) {
            SimpleLevelResult result = analyzer.simulateModel(model, request.getInt("minutesToSimulate"));
            JSONObject json = new JSONObject(result);
            System.out.println(json);
            return json;
        }

        if (HeadlessMain.RequestType.PARAMFIT_STARTPROCESS.name.equals(analyzeType)) {
            String id = Integer.toString(random.nextInt());
            request.put("result_id", id);
            File tempFile = File.createTempFile(id, "_request.json");
            Files.write(request.toString(), tempFile, Charset.defaultCharset());
            ProcessBuilder builder = new ProcessBuilder(new String[]{"java", "-jar", tempFile.getAbsolutePath()});
            builder.start();
            JSONObject result = new JSONObject();
            result.put("result_id", id);
            return null;
        }

        if (HeadlessMain.RequestType.PARAMFIT_IMMEDIATE.name.equals(analyzeType)) {
            File file = new File("pf_inter_results/" + request.getString("result_id") + ".json");
            JSONObject result = analyzer.performParameterFitting(model, request
                    // TODO replace with something
//                        , (c) -> {
//                    try {
//                        if (c.isBestResult) {
//                            JSONObject interResult = new JSONObject();
//                            interResult.put("simulation", analyzer.resultToJSON(c.result, model));
//                            interResult.put("cost", c.cost);
//                            interResult.put("parameters", analyzer.parametersToJson(c.getFitter().getReactionParameters()));
//                            Files.write(interResult.toString(), file, Charset.defaultCharset());
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//
//                }
            );
            System.out.println(result);
            file.delete();
        }
        return null;
    }

    static {
        File configFile = new File("ANIMO_configuration.xml");
        boolean error = false;
        if (configFile.exists()) {
            try {
                AnimoBackend.initialise(configFile);
            } catch (AnimoException e) {
                e.printStackTrace();
                error = true;
            }
        }

        if (!error && configFile.exists()) {
            XmlConfiguration configuration = AnimoBackend.get().configuration();
            String maxAnalyzeTimeStr = configuration.get("/ANIMO/MaximumAnalysisTime", (String)null);
            int maxAnalyzeTime = 60;
            if (maxAnalyzeTimeStr != null) {
                try {
                    maxAnalyzeTime = Integer.parseInt(maxAnalyzeTimeStr);
                } catch (Exception var6) {
                }
            }

            MAX_ANALYZE_MILLIS = 1000 * maxAnalyzeTime;
        } else {
            MAX_ANALYZE_MILLIS = 60000;
        }

    }

    private static enum RequestType {
        SIMULATE("simulate"),
        PARAMFIT_STARTPROCESS("paramfit"),
        PARAMFIT_IMMEDIATE("paramfit_immediate");

        private final String name;

        private RequestType(String name) {
            this.name = name;
        }
    }
}
