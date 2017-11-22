package systems.whitestar.mediasite_monitor.Routes;

import systems.whitestar.mediasite_monitor.API.Models.User;
import systems.whitestar.mediasite_monitor.Meta;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Tom Paulus
 *         Created on 7/5/17.
 */
public class Route {
    /**
     * Check if a User has a Valid Session.
     * If the user does not have a valid session, the "post-login-redirect" will be saved to the session,
     * and the user will be redirected to the login page
     *
     * @param request  {@link HttpServletRequest} Request
     * @param response {@link HttpServletResponse} Response
     * @return True if session is valid, false otherwise.
     * @throws IOException If an input or output exception occurs
     */
    static boolean checkAuth(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getSession().getAttribute("user") == null) {
            // User is not logged in - redirect to login page
            request.getSession(true).setAttribute("post-login-redirect", request.getRequestURI());
            response.sendRedirect("/login");
            return false;
        }
        return true;
    }

    /**
     * Add Monitor Dashboard Build Information
     *
     * @param request {@link HttpServletRequest} Request
     */
    static void addMeta(HttpServletRequest request) {
        if (Meta.buildInfo != null) {
            request.setAttribute("app_version", Meta.buildInfo.getVersion());
            request.setAttribute("app_build", Meta.buildInfo.getBuild());
            request.setAttribute("app_build_time", Meta.buildInfo.getTime());
        }

        // TODO
//        request.setAttribute("hide_issue_link", Settings.getSetting("issues.hideLink"));
//        request.setAttribute("issue_link", Settings.getSetting("issues.link"));
    }

    /**
     * Set User Attributes in Request
     *
     * @param request {@link HttpServletRequest} Request
     */
    static void setUserData(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        request.setAttribute("user", user);
    }
}
