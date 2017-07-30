package edu.sdsu.its.Routes;

import edu.sdsu.its.DB;
import org.jtwig.web.servlet.JtwigRenderer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static edu.sdsu.its.Routes.Route.addMeta;
import static edu.sdsu.its.Routes.Route.checkAuth;
import static edu.sdsu.its.Routes.Route.setUserData;

/**
 * @author Tom Paulus
 *         Created on 5/28/17.
 */
public class ManageUsers extends HttpServlet {
    private final JtwigRenderer renderer = JtwigRenderer.defaultRenderer();
    private static final String TEMPLATE_PATH = "/WEB-INF/templates/manage-users.twig";

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!checkAuth(request, response)) return;
        addMeta(request);
        setUserData(request);

        request.setAttribute("users", DB.getUser(""));

        renderer.dispatcherFor(TEMPLATE_PATH)
                .render(request, response);
    }
}
