package org.acme;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.list.ListCommands;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.redis.client.RedisAPI;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.domain.SimulationJob;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class JobManager {

    @Inject
    EventBus bus;

    private final RedisAPI redisAPI;
    @Inject
    Logger logger;
    private Multi<SimulationResult> shared;

    public JobManager(RedisAPI redisAPI) {
        this.redisAPI = redisAPI;
    }

    @PostConstruct
    void init() {
        logger.info("Init JobManager (blocking XREAD)");

        startXreadLoop("results", "$"); // use "0-0" if you want to read old entries too
    }

    private void startXreadLoop(String stream, String startId) {
        Uni.createFrom().item(startId)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .chain(lastId -> xreadOnce(stream, lastId)
                        .onItem().transformToUni(resp -> {
                            // resp can be null (depending on driver) but with BLOCK 0 it should normally wait.
                            if (resp != null) {
                                String newLastId = handleXreadResponse(stream, resp, lastId);
                                return Uni.createFrom().item(newLastId);
                            }
                            return Uni.createFrom().item(lastId);
                        })
                )
                // repeat forever
                .repeat().indefinitely()
                .onFailure().invoke(t -> logger.error("Redis XREAD loop failed", t))
                .subscribe().with(ignored -> {
                });
    }


    private Uni<io.vertx.mutiny.redis.client.Response> xreadOnce(String stream, String lastId) {
        // XREAD BLOCK 0 COUNT 10 STREAMS results <lastId>
        return redisAPI.xread(List.of(
                "BLOCK", "0",
                "COUNT", "10",
                "STREAMS", stream, lastId
        ));
    }

    /**
     * Parses: [[stream, [[id, [k, v, k, v...]], [id2, [..]] ... ]]]
     * Returns the new lastId (the last entry id processed), or the previous lastId if nothing processed.
     */
    private String handleXreadResponse(String stream, io.vertx.mutiny.redis.client.Response resp, String previousLastId) {
        String lastSeenId = previousLastId;

        for (io.vertx.mutiny.redis.client.Response streamResp : resp) {
            // streamResp = [streamName, entries]
            io.vertx.mutiny.redis.client.Response entries = streamResp.get(1);

            for (io.vertx.mutiny.redis.client.Response entry : entries) {
                String id = entry.get(0).toString();
                io.vertx.mutiny.redis.client.Response kv = entry.get(1);

                // kv = [k1, v1, k2, v2...]
                String dataValue = null;
                for (int i = 0; i < kv.size(); i += 2) {
                    String key = kv.get(i).toString();
                    if ("data".equals(key)) {
                        dataValue = kv.get(i + 1).toString();
                        break;
                    }
                }

                if (dataValue == null) {
                    logger.warnf("Stream entry %s has no 'data' field", id);
                    lastSeenId = id;
                    continue;
                }

                try {
                    bus.publish("job-result", dataValue);
                } catch (Exception e) {
                    logger.errorf(e, "Failed to decode SimulationResult from stream entry %s: %s", id, dataValue);
                    // Decide: skip, or stop. Right now we skip but advance lastSeenId to avoid infinite poison-pill loops.
                }

                lastSeenId = id;
            }
        }

        return lastSeenId;
    }


    public void submitJob(SimulationJob request) {
        bus.send("job-request", request);
    }

    public Multi<SimulationResult> stream() {
        return bus.<String>consumer("job-result")
                .bodyStream().toMulti().map((r) -> {
                            try {
                                return new ObjectMapper().readValue(r, SimulationResult.class);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
    }
}
