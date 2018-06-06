package systems.whitestar.mediasite_monitor.Agent;

import com.google.gson.Gson;
import lombok.extern.log4j.Log4j;
import systems.whitestar.mediasite_monitor.DB;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Tom Paulus
 * Created on 12/1/17.
 */
@Log4j
@Path("config")
public class Configuration {
    @Context
    private HttpServletRequest request;

    /**
     * Get the Configuration Settings for an Agent
     *
     * @return {@link Response} Agent Configuration JSON
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAgentConfiguration() {
        log.debug(String.format("Agent %s requested configuration", request.getSession().getAttribute("agent-id")));
        return Response
                .status(Response.Status.OK)
                .entity(new Gson().toJson(new AgentConfig()))
                .build();
    }

    @SuppressWarnings("unused")
    private class AgentConfig {
        private String url;
        private String apiKey;
        private String apiUser;
        private String apiPass;

        AgentConfig() {
            url = DB.getPreference("ms.url");
            apiKey = DB.getPreference("ms.api-key");
            apiUser = DB.getPreference("ms.api-user");
            apiPass = DB.getPreference("ms.api-pass");
        }
    }
}
