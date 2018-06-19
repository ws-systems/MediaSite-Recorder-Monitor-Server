package systems.whitestar.mediasite_monitor.API;

import com.google.gson.Gson;
import lombok.extern.log4j.Log4j;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.jax.rs.annotations.Pac4JProfile;
import org.pac4j.jax.rs.annotations.Pac4JSecurity;
import systems.whitestar.mediasite_monitor.DB;
import systems.whitestar.mediasite_monitor.Models.Agent;
import systems.whitestar.mediasite_monitor.Models.SimpleMessage;

import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
/**
 * @author Tom Paulus
 * Created on 12/15/17.
 */
@Log4j
@Path("agents")
@Pac4JSecurity(authorizers = "admin")
public class Agents {
    @Context
    private HttpServletRequest request;

    /**
     * List all Agents
     *
     * @return {@link String} Agent Array JSON
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAgents() {
        log.debug("Getting All Agents");
        final Agent[] agents = DB.getAgent("");
        log.debug(String.format("Found %d Agents", agents.length));

        return Response.status(Response.Status.OK).entity(new Gson().toJson(agents)).build();
    }

    /**
     * Update an Agent. Mostly used to change the Authorization Status of a Recorder
     *
     * @param agentId {@link String } Agent ID
     * @param payload {@link String} Updated Agent JSON Object
     * @return {@link Response} Updated Agent JSON
     */
    @Path("{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAgent(@PathParam("id") final String agentId,
                                final String payload) {
        if (agentId == null || agentId.isEmpty())
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SimpleMessage("Error",
                            "No Agent ID supplied in request").asJson())
                    .build();

        final Agent existingAgent;
        try {
            existingAgent = DB.getAgent("a.id = '" + agentId + "'")[0];
        } catch (IndexOutOfBoundsException e) {
            log.info(String.format("Could not find a user with ID \"%s\"", agentId));
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new SimpleMessage("Error",
                            "No agent with that id could be found in the system").asJson())
                    .build();
        }

        Agent newAgent = new Gson().fromJson(payload, Agent.class);

        final Agent mergedAgent = Agent.merge(existingAgent, newAgent);
        try {
            DB.updateAgent(mergedAgent);
        } catch (PersistenceException e) {
            return Response.status(Response.Status.PRECONDITION_FAILED).entity(new SimpleMessage("error",
                    "Could not update Agent, Precondition Failed. This may be caused by a duplicate ID").asJson()).build();
        }

        return Response.status(Response.Status.ACCEPTED).entity(new SimpleMessage(
                "okay",
                "Agent updated successfully!"
        ).asJson()).build();
    }

    /**
     * Delete an Agent.
     *
     * @param agentId {@link String} Agent ID
     * @return {@link Response} Confirmation Message
     */
    @Path("{id}")
    @DELETE
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAgent(@Pac4JProfile CommonProfile profile,
                                @PathParam("id") final String agentId) {
        if (agentId == null || agentId.isEmpty())
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SimpleMessage("Error",
                            "No Agent ID supplied in request").asJson())
                    .build();

        log.warn(String.format("User \"%s\" is requesting to delete the Agent with ID \"%s\"",
                profile.getAttribute("name"),
                agentId));

        try {
            final Agent agent = DB.getAgent("a.id = '" + agentId + "'")[0];
            DB.deleteAgent(agent);

        } catch (IndexOutOfBoundsException e) {
            log.info(String.format("Could not find a agent with ID \"%s\"", agentId));
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new SimpleMessage("Error",
                            "No agent with that ID could be found in the system").asJson())
                    .build();
        }

        return Response.status(Response.Status.ACCEPTED).entity(new SimpleMessage(
                "okay",
                String.format("Agent with ID \"%s\" has been deleted", agentId))
                .asJson()).build();
    }
}
