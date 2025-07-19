package org.acme.processors;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.list.KeyValue;
import io.quarkus.redis.datasource.list.ListCommands;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.acme.SimulationResult;
import org.acme.domain.SimulationJob;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Random;

import static java.lang.Thread.sleep;

@ApplicationScoped
public class JobProcessor implements Runnable {

    private final PubSubCommands<SimulationResult> publisher;
    private final String name;
    private final Logger logger;
    private final ListCommands<String, SimulationJob> queue;

    private volatile boolean stopped = false;

    public JobProcessor(@ConfigProperty(name = "simulator-name") String name, Logger logger, RedisDataSource ds) {
        this.name = name;
        this.logger = logger;
        this.publisher = ds.pubsub(SimulationResult.class);
        this.queue = ds.list(SimulationJob.class);
    }

    public void start(@Observes StartupEvent ev) {
        new Thread(this).start();
    }

    public void stop(@Observes ShutdownEvent ev) {
        stopped = true;
    }

    @Override
    public void run() {
        logger.infof("Simulator %s starting", name);
        while ((!stopped)) {
            KeyValue<String, SimulationJob> item = queue.brpop(Duration.ofSeconds(1), "job-requests");
            if (item != null) {
                var request = item.value();
                logger.infof("Simulator %s is going to simulate", name);
                var result = simulate(request);
                publisher.publish("job-results", result);

            }
        }
    }

    public SimulationResult simulate(SimulationJob request) {
        int random = new Random().nextInt(0, 2000);
        try {
            Thread.sleep(random);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        logger.infof("Simulator %s finished simulation for request %s, waited %s seconds", name, request.getId(), random);
        return new SimulationResult(request.getId());
    }
}
