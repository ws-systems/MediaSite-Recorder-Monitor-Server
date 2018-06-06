package systems.whitestar.mediasite_monitor.Agent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.log4j.Log4j;
import systems.whitestar.mediasite_monitor.DB;
import systems.whitestar.mediasite_monitor.Models.Agent;
import systems.whitestar.mediasite_monitor.Models.AgentJob;
import systems.whitestar.mediasite_monitor.Models.ClassTypeAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Tom Paulus
 * Created on 12/1/17.
 */
@Log4j
@Path("queue")
public class AgentJobQueue {
    private static Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, new ClassTypeAdapter()).create();


    @Context
    private HttpServletRequest request;

    /**
     * Periodic Recorder Check in Endpoint.
     * Updates last seen record for agents, and provides the agent with a job if one is available and the specified
     * agent is allowed to execute the job.
     *
     * @return {@link Response} Job to execute if one is available, else Empty Response (Code 204)
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response pop() {
        Agent agent = DB.getAgent(String.format("id = '%s'", request.getSession().getAttribute("agent-id")))[0];
        log.debug(String.format("Agent %s is checking in - standard heartbeat", agent.getId()));
        agent.setLastSeen(new Timestamp(System.currentTimeMillis()));
        DB.updateAgent(agent);

        AgentJob job = DB.getNewAgentJob(agent);

        if (job != null) {
            log.info("A job is available for this agent");
            log.debug(job.toString());

            // Update Job
            job.setAgent(agent);
            job.setStatus(AgentJob.AgentJobStatus.RECEIVED);
            DB.updateAgentJob(job);

            log.debug("Job has been updated to reflect that it has been distributed to an agent");

            return Response
                    .status(Response.Status.OK)
                    .entity(gson.toJson(job))
                    .build();
        }

        return Response
                .status(Response.Status.NO_CONTENT)
                .build();

    }

    @Path("/job/{id}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response push(@PathParam("id") final String jobID,
                         final String payload) {
        AgentJob job = DB.getAgentJob(jobID);

        //noinspection unchecked
        job.setPayload(gson.fromJson(payload, Map.class));

        // Mark job as received in DB
        job.setStatus(AgentJob.AgentJobStatus.EXECUTED);
        DB.updateAgentJob(job);

        try {
            Class jobClass = job.getJob();
            log.debug(String.format("Class %s has methods - %s", jobClass.getName(), Arrays.toString(jobClass.getDeclaredMethods())));

            //noinspection unchecked
            Method method = jobClass.getMethod("process", Map.class);
            method.invoke(jobClass.newInstance(), job.getPayload());
        } catch (NoSuchMethodException e) {
            log.error("Class does not correctly implement the interface as it is missing the 'process' method", e);
        } catch (IllegalAccessException | InstantiationException e) {
            log.error("Cannot instantiate new instance of job class", e);
        } catch (InvocationTargetException e) {
            log.error("Could not process job - method threw exception", e);
        }

        // Mark job as completed in DB
        job.setStatus(AgentJob.AgentJobStatus.DONE);
        DB.updateAgentJob(job);

        return Response.status(Response.Status.ACCEPTED).build();
    }
}
