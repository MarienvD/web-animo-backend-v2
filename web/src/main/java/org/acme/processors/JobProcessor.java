package org.acme.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.JobProcessorBuilder;
import org.acme.JobResult;
import org.acme.domain.JobWrapper;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Map;

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
    Uni<Void> consumeJob(JobWrapper item) {
        if (item != null) {
            org.acme.domain.JobProcessor builtJobProcessor;
            try {
                builtJobProcessor = new JobProcessorBuilder().build(item);
            } catch (JsonProcessingException e) {
                logger.errorf("Could not parse job: %s", item, e);
                return Uni.createFrom().voidItem();
            }
            logger.infof("simulate!");
            return Uni.createFrom().item(builtJobProcessor.run())
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                    .chain(result -> reactiveRedisDataSource.stream(JobResult.class)
                            .xadd("results", Map.of("data", result)))
                    // 3. Handle errors within the pipeline
                    .ifNoItem().after(Duration.ofMillis(10000)).fail()
                    .onFailure().invoke(e -> logger.errorf("Simulator failed for %s: %s", item, e.getMessage()))
                    .replaceWithVoid();
        }
        return Uni.createFrom().voidItem();
    }


}
