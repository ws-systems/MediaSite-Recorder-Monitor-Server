package systems.whitestar.mediasite_monitor.Routes;

import lombok.extern.log4j.Log4j;
import org.jtwig.web.servlet.JtwigRenderer;
import org.pac4j.core.config.ConfigSingleton;
import org.pac4j.http.client.indirect.FormClient;
import systems.whitestar.mediasite_monitor.Auth.LoginConfigFactory;
import systems.whitestar.mediasite_monitor.Models.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Global System Admin Route
 *
 * @author Tom Paulus
 * Created on 12/27/17.
 */
@Log4j
public class SuperUser extends HttpServlet {
    private static final String LOGIN_TEMPLATE_PATH = "/WEB-INF/templates/superuser-login.twig";

    private final JtwigRenderer renderer = JtwigRenderer.defaultRenderer();


    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        switch (req.getServletPath()) {
            case "/superuser-login":
                // Render Login Page for GSA
                FormClient formClient = LoginConfigFactory.getConfig().getClients().findClient(FormClient.class);
                req.setAttribute("post_url", formClient.getCallbackUrl());
                break;
            case "/superuser":
                // Used to force redirection to the Super User Login Page if necessary
                resp.sendRedirect("/app");
                break;
            default:
                resp.sendError(404, "Page does not exist");
                return;
        }

        renderer.dispatcherFor(LOGIN_TEMPLATE_PATH)
                .render(req, resp);
    }
}
