package systems.whitestar.mediasite_monitor.Routes;

import lombok.extern.log4j.Log4j;
import org.apache.http.HttpRequest;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.pac4j.core.profile.CommonProfile;
import systems.whitestar.mediasite_monitor.Auth.Callback;
import systems.whitestar.mediasite_monitor.DB;
import systems.whitestar.mediasite_monitor.Meta;
import systems.whitestar.mediasite_monitor.Secret;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Optional;

import static systems.whitestar.mediasite_monitor.Routes.ManageAgents.ONLINE_DELTA_MINS;

/**
 * @author Tom Paulus
 * Created on 7/5/17.
 */
@Log4j class Route {
    private static final String SSO_UPDATE_INFO_URL = Secret.getInstance().getSecret("sso.update-info-URL");

    /**
     * Add Monitor Dashboard Build Information
     *
     * @param attributes {@link Map} Request Attributes
     */
    static void addMeta(Map<String, Object> attributes) {
        if (Meta.buildInfo != null) {
            attributes.put("app_version", Meta.buildInfo.getVersion());
            attributes.put("app_build", Meta.buildInfo.getBuild());
            attributes.put("app_build_time", Meta.buildInfo.getTime());
        }

        attributes.put("hide_issue_link", Secret.getInstance().getSecret("issues.hideLink"));
        attributes.put("issue_link", Secret.getInstance().getSecret("issues.link"));
    }

    /**
     * Set User Attributes in Request
     *
     * @param request    {@link HttpRequest} Request
     * @param attributes {@link Map} Request Attributes
     */
    static void setUserData(HttpServletRequest request, CommonProfile profile, Map<String, Object> attributes) {
        if (request.getSession().getAttribute("user") == null) {
            new Callback().loginCB(request, Optional.of(profile));
        }

        attributes.put("user", request.getSession().getAttribute("user"));
        attributes.put("superuser", request.getSession().getAttribute("superuser"));
        attributes.put("first_login", request.getSession().getAttribute("first_login"));
        attributes.put("sso_update_info_url", SSO_UPDATE_INFO_URL);
    }


    /**
     * Set attributes defined in the NavBar
     *
     * @param attributes {@link Map} Request Attributes
     */
    static void setNavBar(Map<String, Object> attributes) {
        // Agent Count is number of authorized and online agents
        attributes.put("agent_count", DB.getAgent("a.lastSeen > (current_date() + " + ONLINE_DELTA_MINS + "/(24*60)) and a.authorized = true").length);
    }

    /**
     * Render a Twig Template for a given set of attributes for a Servlet
     *
     * @param templatePath {@link String} Template Path (Servlet Relative)
     * @param attributes   {@link Map} Model Attributes
     * @param context      {@link ServletContext} Servlet
     * @return {@link StreamingOutput} Render Result
     */
    static StreamingOutput renderTemplate(String templatePath, Map<String, Object> attributes, ServletContext context) {
        try {
            final JtwigTemplate template = JtwigTemplate.fileTemplate(context.getResource(templatePath).getPath());
            final JtwigModel model = JtwigModel.newModel(attributes);

            return os -> {
                Writer writer = new BufferedWriter(new OutputStreamWriter(os));
                template.render(model, os);
                writer.flush();
            };

        } catch (MalformedURLException e) {
            log.error("Incorrectly formatted Template URL", e);
            throw new RuntimeException(e);
        }
    }
}
