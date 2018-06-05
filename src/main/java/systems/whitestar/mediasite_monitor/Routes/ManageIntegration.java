package systems.whitestar.mediasite_monitor.Routes;

import systems.whitestar.mediasite_monitor.DB;
import org.jtwig.web.servlet.JtwigRenderer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

import static systems.whitestar.mediasite_monitor.Routes.Route.*;

/**
 * @author Tom Paulus
 *         Created on 5/28/17.
 */
public class ManageIntegration extends HttpServlet {
    private final JtwigRenderer renderer = JtwigRenderer.defaultRenderer();
    private static final String TEMPLATE_PATH = "/WEB-INF/templates/manage-integrations.twig";

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        addMeta(request);
        setUserData(request);
        setNavBar(request);


        // Mediasite Settings
        request.setAttribute("ms_api_url", DB.getPreference("ms.url"));
        request.setAttribute("ms_api_key", DB.getPreference("ms.api-key"));
        request.setAttribute("ms_api_user", DB.getPreference("ms.api-user"));

        // Email Settings
        request.setAttribute("email_host", DB.getPreference("email.host"));
        request.setAttribute("email_port", DB.getPreference("email.port"));
        request.setAttribute("email_ssl", Boolean.parseBoolean(DB.getPreference("email.ssl")));
        request.setAttribute("email_username", DB.getPreference("email.username"));
        request.setAttribute("email_from_name", DB.getPreference("email.from_name"));
        request.setAttribute("email_from_email", DB.getPreference("email.from_email"));

        // Slack Settings
        request.setAttribute("slack_enable", DB.getPreference("slack.enable"));
        request.setAttribute("slack_webhook_url", DB.getPreference("slack.webhook_url"));

        response.setHeader("Content-Type", MediaType.TEXT_HTML);
        renderer.dispatcherFor(TEMPLATE_PATH)
                .render(request, response);
    }
}
