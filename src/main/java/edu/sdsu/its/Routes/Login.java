package edu.sdsu.its.Routes;

import edu.sdsu.its.DB;
import org.jtwig.web.servlet.JtwigRenderer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static edu.sdsu.its.Routes.Route.addMeta;
import static edu.sdsu.its.Routes.Route.setUserData;

/**
 * @author Tom Paulus
 *         Created on 5/28/17.
 */
public class Login extends HttpServlet {
    private final JtwigRenderer renderer = JtwigRenderer.defaultRenderer();
    private static final String TEMPLATE_PATH = "/WEB-INF/templates/login.twig";

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (request.getSession().getAttribute("request") != null) {
            // User is already logged in - send them to the Index!
            response.sendRedirect("index");
            return;
        }

        String loginStatus = (String) request.getSession().getAttribute("login-status");
        request.setAttribute("login_status", loginStatus);
        if ("logged out".equals(loginStatus)) {
            // End the User's Session, Since they are logging out now.
            request.getSession().invalidate();
        }


        renderer.dispatcherFor(TEMPLATE_PATH)
                .render(request, response);
    }
}
