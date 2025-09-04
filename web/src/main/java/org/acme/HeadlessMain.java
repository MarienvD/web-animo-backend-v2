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

    public static void main(String[] args) throws JSONException {
        AtomicBoolean interrupted = new AtomicBoolean(false);

        try {
            Thread workThread = Thread.currentThread();
            Thread interruptThread = new Thread(() -> {
                try {
                    Thread.sleep((long)MAX_ANALYZE_MILLIS);
                    interrupted.set(true);
                    workThread.interrupt();
                } catch (Exception var3) {
                }

            });
            interruptThread.setDaemon(true);
            interruptThread.start();
            ModelAnalyzer analyzer = new ModelAnalyzer();
            if (args.length != 1) {
                throw new IllegalArgumentException("invalid number of args: " + args.length);
            }

            JSONObject request = new JSONObject(new JSONTokener(new FileInputStream(args[0])));
            Model model = ModelAnalyzer.getModelFromJson(request.getJSONObject("model"), request.getInt("minutesToSimulate"));
           executeFromRequest(analyzer, request, model);

            interruptThread.interrupt();
        } catch (Exception var12) {
            JSONObject exceptionJSON = new JSONObject();
            exceptionJSON.put("error", true);
            if (interrupted.get()) {
                exceptionJSON.put("message", "Analysis took too long\n(more than " + MAX_ANALYZE_MILLIS / 1000 + " seconds)");
            } else {
                exceptionJSON.put("message", var12.getMessage());
            }

            exceptionJSON.put("cause", var12.getCause());
            String stackTrace = (String)Arrays.asList(var12.getStackTrace()).stream().map((se) -> se.toString()).reduce((s1, s2) -> s1 + "\n" + s2).get();
            exceptionJSON.put("stacktrace", stackTrace);
            System.out.println(exceptionJSON);
        }

    }

    public static void executeFromRequest(ModelAnalyzer analyzer, JSONObject request, Model model) throws JSONException, AnalysisException, IOException {
        String analyzeType = request.optString("type", HeadlessMain.RequestType.SIMULATE.name);
        if (HeadlessMain.RequestType.SIMULATE.name.equals(analyzeType)) {
            SimpleLevelResult result = analyzer.simulateModel(model, request.getInt("minutesToSimulate"));
            JSONObject json = analyzer.resultToJSON(result, model);
            System.out.println(json);
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
            System.out.println(result);
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
