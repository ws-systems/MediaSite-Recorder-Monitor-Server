package systems.whitestar.mediasite_monitor.Routes;

import org.jtwig.web.servlet.JtwigRenderer;
import systems.whitestar.mediasite_monitor.DB;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.sql.Timestamp;

import static systems.whitestar.mediasite_monitor.Jobs.CleanupAgents.MAX_AGE;
import static systems.whitestar.mediasite_monitor.Routes.Route.*;

/**
 * @author Tom Paulus
 * Created on 5/28/17.
 */
public class ManageAgents extends HttpServlet {
    private static final String TEMPLATE_PATH = "/WEB-INF/templates/manage-agents.twig";
    public static final int ONLINE_DELTA_MINS = -5; // How long since a recorder checked in and have it still be considered online
    private final JtwigRenderer renderer = JtwigRenderer.defaultRenderer();

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        addMeta(request);
        setUserData(request);
        setNavBar(request);

        request.setAttribute("agents", DB.getAgent(""));
        request.setAttribute("last_seen_delta",
                new Timestamp(System.currentTimeMillis() + ONLINE_DELTA_MINS * 60 * 1000));
        request.setAttribute("agent_cleanup_rate", MAX_AGE);
        request.setAttribute("agent_job_count", DB.getAgentJobCount());

        response.setHeader("Content-Type", MediaType.TEXT_HTML);

        renderer.dispatcherFor(TEMPLATE_PATH)
                .render(request, response);
    }
}
