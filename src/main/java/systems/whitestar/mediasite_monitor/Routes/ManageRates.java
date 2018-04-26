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
public class ManageRates extends HttpServlet {
    private final JtwigRenderer renderer = JtwigRenderer.defaultRenderer();
    private static final String TEMPLATE_PATH = "/WEB-INF/templates/manage-rates.twig";

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        addMeta(request);
        setUserData(request);
        setNavBar(request);

        request.setAttribute("list_sync", DB.getPreference("sync_db.enable"));
        request.setAttribute("list_frequency", DB.getPreference("sync_db.frequency"));
        request.setAttribute("status_sync", DB.getPreference("sync_recorder.enable"));
        request.setAttribute("status_frequency", DB.getPreference("sync_recorder.frequency"));

        response.setHeader("Content-Type", MediaType.TEXT_HTML);
        renderer.dispatcherFor(TEMPLATE_PATH)
                .render(request, response);
    }
}
