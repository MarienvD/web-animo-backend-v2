package org.acme.processors;

import animo.core.model.Model;
import animo.exceptions.AnimoException;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.list.KeyValue;
import io.quarkus.redis.datasource.list.ListCommands;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.acme.HeadlessMain;
import org.acme.ModelAnalyzer;
import org.acme.SimulationResult;
import org.acme.domain.SimulationJob;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Random;

import static java.lang.Thread.sleep;

@ApplicationScoped
public class JobProcessor {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(JobProcessor.class);
    private final PubSubCommands<SimulationResult> publisher;
    private final Logger logger;

    public JobProcessor(Logger logger, RedisDataSource ds) {
        this.logger = logger;
        this.publisher = ds.pubsub(SimulationResult.class);
    }

    @ConsumeEvent("job-request")
    String consumeJob(SimulationJob item) {
        if (item != null) {
            logger.infof("Simulator %s is going to simulate", item);
            SimulationResult result = null;
            try {
                result = simulate(item);
                publisher.publish("job-results", result);
            } catch (Exception e) {
                logger.errorf("Simulator %s failed to simulate %s", item, e);
            }
        }
        return "OK";
    }

    public SimulationResult simulate(SimulationJob request) throws AnimoException, JSONException, IOException {
        Model model = ModelAnalyzer.getModelFromJson(request.getModel(), request.getMinutesToSimulate());
        HeadlessMain.executeFromRequest(new ModelAnalyzer(), new JSONObject(), model);
        return new SimulationResult(request.getId());
    }
}
