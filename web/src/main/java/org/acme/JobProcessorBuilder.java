package org.acme;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.redis.client.RedisAPI;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.domain.*;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class JobProcessorBuilder {

    public JobProcessor build(JobWrapper item) throws JsonProcessingException {
        if (item.getType().equals(JobType.SIMULATION)) {
            return new SimulationJobProcessor(new ObjectMapper().readValue(item.getContents(), SimulationJob.class), item.getType().name());
        }
        throw new RuntimeException("Unknown job type: " + item.getType());
    }
}
