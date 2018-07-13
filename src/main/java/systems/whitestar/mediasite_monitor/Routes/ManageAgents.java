package systems.whitestar.mediasite_monitor.Routes;

import org.pac4j.core.profile.CommonProfile;
import org.pac4j.jax.rs.annotations.Pac4JProfile;
import org.pac4j.jax.rs.annotations.Pac4JSecurity;
import systems.whitestar.mediasite_monitor.DB;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import static systems.whitestar.mediasite_monitor.Jobs.CleanupAgents.MAX_AGE;
import static systems.whitestar.mediasite_monitor.Routes.Route.*;

/**
 * @author Tom Paulus
 * Created on 5/28/17.
 */
@Path("manage/agents")
public class ManageAgents {
    public static final int ONLINE_DELTA_MINS = -5; // How long since a recorder checked in and have it still be considered online
    private static final String TEMPLATE_PATH = "/WEB-INF/templates/manage-agents.twig";

    @Context
    private ServletContext context;


    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    @Pac4JSecurity(clients = "OidcClient, FormClient", authorizers = "admin")
    public Response getIndex(@Context final HttpServletRequest httpRequest,
                             @Pac4JProfile CommonProfile profile) {
        final Map<String, Object> attributes = new HashMap<>();

        addMeta(attributes);
        setUserData(httpRequest, profile, attributes);
        setNavBar(attributes);

        attributes.put("agents", DB.getAgent(""));
        attributes.put("last_seen_delta", new Timestamp(System.currentTimeMillis() + ONLINE_DELTA_MINS * 60 * 1000));
        attributes.put("agent_cleanup_rate", MAX_AGE);
        attributes.put("agent_job_count", DB.getAgentJobCount());

        return Response.ok(renderTemplate(TEMPLATE_PATH, attributes, context)).build();
    }
}
