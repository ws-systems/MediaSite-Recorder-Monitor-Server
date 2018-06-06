package systems.whitestar.mediasite_monitor.Agent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.log4j.Log4j;
import systems.whitestar.mediasite_monitor.DB;
import systems.whitestar.mediasite_monitor.Models.Agent;
import systems.whitestar.mediasite_monitor.Models.SimpleMessage;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;

/**
 * @author Tom Paulus
 * Created on 11/22/17.
 */
@Log4j
@Path("register")
public class Register {

    @Context
    private HttpServletRequest request;

    static boolean getAgentAuthorization(final String agentId) {
        Agent[] agents = DB.getAgent(String.format("id = '%s'", agentId));
        return agents.length == 1 && agents[0].isAuthorized();
    }

    /**
     * Register an Agent with the Server
     *
     * @param payload {@link String} Agent JSON
     * @return {@link Response} Registration Status
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerAgent(final String payload) {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

        if (payload == null || payload.isEmpty())
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SimpleMessage("Error", "No Payload").asJson())
                    .build();

        Agent agent = gson.fromJson(payload, Agent.class);
        agent.setAuthorized(null);  // Clear Authorization value sent from client, if one is set
        if (agent.getId() == null || agent.getId().isEmpty())
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SimpleMessage("Error", "Agent ID Not specified in Payload").asJson())
                    .build();
        if (agent.getName() == null || agent.getName().isEmpty())
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SimpleMessage("Error", "Agent Name Not specified in Payload").asJson())
                    .build();

        Response.Status status;
        if (DB.getAgent(String.format("id = '%s'", agent.getId())).length == 0) {
            log.info(String.format("Agent %s does not exist - creating", agent.getId()));
            agent.setLastSeen(new Timestamp(System.currentTimeMillis()));
            DB.updateAgent(agent);
            status = Response.Status.CREATED;
        } else {
            status = Response.Status.OK;
        }
        request.getSession(true).setAttribute("agent-id", agent.getId());
        Agent[] agents = DB.getAgent(String.format("id = '%s'", agent.getId()));
        return Response
                .status(status)
                .entity(gson.toJson(agents[0]))
                .build();
    }

    /**
     * Get the Agent Registration Status
     *
     * @param agentId {@link String} Agent ID
     * @return {@link Response} Agent Object JSON
     */
    @Path("/status/{id}")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrationStatus(@PathParam("id") final String agentId) {
        Agent[] agents = DB.getAgent(String.format("id = '%s'", agentId));

        if (agents.length != 1) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new SimpleMessage("Error", "Invalid Agent ID").asJson())
                    .build();
        }

        return Response
                .status(Response.Status.OK)
                .entity(new Gson().toJson(agents[0]))
                .build();
    }
}
