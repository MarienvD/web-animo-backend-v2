package org.acme.resource;

import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.JobManager;
import org.acme.SimulationResult;
import org.acme.domain.SimulationJob;
import org.jboss.resteasy.reactive.RestStreamElementType;

@Path("/job")
public class JobResource {

    @Inject
    JobManager jobManager;

    // TODO use token to verify client Id
    @POST
    public Response submit(@HeaderParam("clientId") String clientId, @HeaderParam("token") String token, SimulationJob job) {
        job.setClientId(clientId);
        job.setToken(token);
        return jobManager.submitJob(job);
    }

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<SimulationResult> fights(@HeaderParam("clientId") String clientId) {
        if (clientId == null || clientId.isEmpty()) {
            return Multi.createFrom().failure(() -> new IllegalArgumentException("clientId is null or empty"));
        }
        return jobManager.stream()
                .filter(result -> clientId.equals(result.getClientId()));
    }
}