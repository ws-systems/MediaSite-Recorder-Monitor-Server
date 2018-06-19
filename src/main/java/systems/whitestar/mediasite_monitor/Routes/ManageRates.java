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
import java.util.HashMap;
import java.util.Map;

import static systems.whitestar.mediasite_monitor.Routes.Route.*;

/**
 * @author Tom Paulus
 * Created on 5/28/17.
 */
@Path("manage/rates")
public class ManageRates {
    private static final String TEMPLATE_PATH = "/WEB-INF/templates/manage-rates.twig";

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

        attributes.put("list_sync", DB.getPreference("sync_db.enable"));
        attributes.put("list_frequency", DB.getPreference("sync_db.frequency"));
        attributes.put("status_sync", DB.getPreference("sync_recorder.enable"));
        attributes.put("status_frequency", DB.getPreference("sync_recorder.frequency"));
        attributes.put("status_retry_count", DB.getPreference("sync_recorder.retry_count"));

        return Response.ok(renderTemplate(TEMPLATE_PATH, attributes, context)).build();
    }
}
