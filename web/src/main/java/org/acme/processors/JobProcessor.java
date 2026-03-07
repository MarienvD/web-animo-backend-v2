package org.acme.processors;

import animo.core.model.Model;
import animo.exceptions.AnimoException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.mutiny.redis.client.RedisAPI;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.HeadlessMain;
import org.acme.ModelAnalyzer;
import org.acme.ModelMapper;
import org.acme.SimulationResult;
import org.acme.domain.SimulationJob;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import static java.lang.Thread.sleep;

@ApplicationScoped
public class JobProcessor {
    private final ReactiveRedisDataSource reactiveRedisDataSource;

    Logger logger;

    @ConfigProperty(name = "animo.config.file.path")
    String configFilePath;

    public JobProcessor(Logger logger, ReactiveRedisDataSource reactiveRedisDataSource) {
        this.logger = logger;
        this.reactiveRedisDataSource = reactiveRedisDataSource;
    }

    @ConsumeEvent("job-request")
    Uni<Void> consumeJob(SimulationJob item) {
        if (item != null) {
            logger.infof("simulate!");
            return Uni.createFrom().item(() -> simulate(item))
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                    .chain(result -> reactiveRedisDataSource.stream(SimulationResult.class)
                            .xadd("results", Map.of("data", result)))
                    // 3. Handle errors within the pipeline
                    .ifNoItem().after(Duration.ofMillis(10000)).fail()
                    .onFailure().invoke(e -> logger.errorf("Simulator failed for %s: %s", item, e.getMessage()))
                    .replaceWithVoid();
        }
        return Uni.createFrom().voidItem();
    }

    @Timed(value = "simulation")
    public SimulationResult simulate(SimulationJob request) {
        logger.infof("Simulator %s is going to simulate", request);
        Instant start = Instant.now();
        Model model = null;
        JSONObject result;
        try {
            model = ModelMapper.getModelFromJson(request.getModel(), request.getMinutesToSimulate());
            result = HeadlessMain.executeFromRequest(new ModelAnalyzer(configFilePath), new JSONObject(new ObjectMapper().writeValueAsString(request)), model);
        } catch (JSONException | AnimoException | IOException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        Instant end = Instant.now();
        logger.infof("Simulation took %s", Duration.between(start, end));

        // filter disabled reactants
        try {
            SimulationResultDto simulationResultDto = new ObjectMapper().readValue(result.toString(), SimulationResultDto.class);
            simulationResultDto.filterDisabledReactants(request);
            return new SimulationResult(request.getId(), new ObjectMapper().writeValueAsString(simulationResultDto), request.getClientId());

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}
