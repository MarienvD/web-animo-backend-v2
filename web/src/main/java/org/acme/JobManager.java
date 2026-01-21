package org.acme;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.list.ListCommands;
import io.smallrye.mutiny.Multi;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.acme.domain.SimulationJob;

import java.util.UUID;

@ApplicationScoped
public class JobManager {

    @Inject
    EventBus bus;

    private final ListCommands<String, SimulationJob> jobs;
    private final ReactiveRedisDataSource reactiveRedisDataSource;
    private Multi<SimulationResult> shared;

    public JobManager(RedisDataSource dataSource, ReactiveRedisDataSource reactiveRedisDataSource) {
        jobs = dataSource.list(SimulationJob.class);
        this.reactiveRedisDataSource = reactiveRedisDataSource;
    }

    @PostConstruct
    void init() {
        // ONE Redis subscription, broadcast to all SSE subscribers
        this.shared = reactiveRedisDataSource.pubsub(SimulationResult.class)
                .subscribe("job-results")
                // protect your server if SSE clients are slow
                .onOverflow().drop()
                .broadcast().toAllSubscribers();
    }

    public Response submitJob(SimulationJob request) {
        bus.send("job-request", request);
        return null;
    }

    public Multi<SimulationResult> stream() {
        return shared;
    }
}
