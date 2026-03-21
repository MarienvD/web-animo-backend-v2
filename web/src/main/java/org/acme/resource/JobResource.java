package org.acme.resource;

import io.micrometer.core.annotation.Counted;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.JobManager;
import org.acme.JobResult;
import org.acme.domain.JobType;
import org.acme.domain.SimulationJob;
import org.jboss.resteasy.reactive.RestStreamElementType;

@Path("/job")
public class JobResource {

    @Inject
    JobManager jobManager;

    @Path("/simulate")
    @POST
    @Counted(value = "job.submit", description = "Number of jobs submitted")
    public Response submit(@HeaderParam("clientId") String clientId, @HeaderParam("token") String token, SimulationJob job) {
        job.setClientId(clientId);
        job.setToken(token);
        jobManager.submitSimulationJob(job, JobType.SIMULATION);
        return Response.ok().build();
    }

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @Counted(value = "job.socket.connections", description = "Connections to socket")
    public Multi<JobResult> streamJobResults(@HeaderParam("clientId") String clientId) {
        if (clientId == null || clientId.isEmpty()) {
            return Multi.createFrom().failure(() -> new IllegalArgumentException("clientId is null or empty"));
        }
        return jobManager.stream()
                .filter(result -> clientId.equals(result.getClientId()));
    }
}
