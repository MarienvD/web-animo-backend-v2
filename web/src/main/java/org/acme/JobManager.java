package org.acme;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.list.ListCommands;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.domain.SimulationJob;

import java.util.UUID;

@ApplicationScoped
public class JobManager {
    private final ListCommands<String, SimulationJob> jobs;
    private final Multi<SimulationResult> stream;

    public JobManager(RedisDataSource dataSource, ReactiveRedisDataSource reactiveRedisDataSource) {
        jobs = dataSource.list(SimulationJob.class);
        stream = reactiveRedisDataSource.pubsub(SimulationResult.class).subscribe("job-results")
                .broadcast().toAllSubscribers();
    }

    public SimulationJob submitJob() {
        var id = UUID.randomUUID().toString();
        var request = new SimulationJob(id);
        jobs.lpush("job-requests", request);
        return request;
    }

    public Multi<SimulationResult> stream() {
        return stream;
    }
}