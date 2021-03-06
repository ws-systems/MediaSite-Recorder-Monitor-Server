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
@Path("manage/integrations")
public class ManageIntegration {
    private static final String TEMPLATE_PATH = "/WEB-INF/templates/manage-integrations.twig";

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

        Map<String, String> preferences = DB.getPreferences();

        // Mediasite Settings
        attributes.put("ms_api_url", preferences.get("ms.url"));
        attributes.put("ms_api_key", preferences.get("ms.api-key"));
        attributes.put("ms_api_user", preferences.get("ms.api-user"));

        // Email Settings
        attributes.put("email_host", preferences.get("email.host"));
        attributes.put("email_port", preferences.get("email.port"));
        attributes.put("email_ssl", Boolean.parseBoolean(preferences.get("email.ssl")));
        attributes.put("email_username", preferences.get("email.username"));
        attributes.put("email_from_name", preferences.get("email.from_name"));
        attributes.put("email_from_email", preferences.get("email.from_email"));

        // Slack Settings
        attributes.put("slack_enable", preferences.get("slack.enable"));
        attributes.put("slack_webhook_url", preferences.get("slack.webhook_url"));

        return Response.ok(renderTemplate(TEMPLATE_PATH, attributes, context)).build();
    }
}
