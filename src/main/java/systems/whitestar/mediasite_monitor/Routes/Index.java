package systems.whitestar.mediasite_monitor.Routes;

import systems.whitestar.mediasite_monitor.DB;
import org.jtwig.web.servlet.JtwigRenderer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static systems.whitestar.mediasite_monitor.Routes.Route.addMeta;
import static systems.whitestar.mediasite_monitor.Routes.Route.setUserData;

/**
 * @author Tom Paulus
 *         Created on 5/28/17.
 */
public class Index extends HttpServlet {
    private final JtwigRenderer renderer = JtwigRenderer.defaultRenderer();
    private static final String TEMPLATE_PATH = "/WEB-INF/templates/index.twig";

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        addMeta(request);
        setUserData(request);

        request.setAttribute("recorders", DB.getRecorder(""));

        renderer.dispatcherFor(TEMPLATE_PATH)
                .render(request, response);
    }
}
