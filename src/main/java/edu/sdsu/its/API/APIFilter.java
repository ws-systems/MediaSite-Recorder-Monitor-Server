package edu.sdsu.its.API;

import edu.sdsu.its.API.Models.SimpleMessage;
import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Tom Paulus
 * Created on 7/14/17.
 */
public class APIFilter implements Filter {
    private static final Logger LOGGER = Logger.getLogger(APIFilter.class);

    @Override public void init(FilterConfig filterConfig) throws ServletException {
        // Intentionally Blank
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        boolean isLoginRequest = ((HttpServletRequest) request).getRequestURI().contains("login");
        boolean isSSEStreamRequests = ((HttpServletRequest) request).getRequestURI().contains("stream");

        final HttpSession session = ((HttpServletRequest) request).getSession();
        if (!isLoginRequest && !isSSEStreamRequests && session == null) {
            // Session not included in request

            LOGGER.warn("Unauthorized Request to " + ((HttpServletRequest) request).getRequestURI() + "- session not included in request");
            ((HttpServletResponse) response).setStatus(401);
            ((HttpServletResponse) response).setHeader("Content-Type", MediaType.APPLICATION_JSON);

            final PrintWriter writer = response.getWriter();
            writer.write(new SimpleMessage("Error",
                    "No session token was included in your request. Make sure that the" +
                            "JSESSIONID in the cookies that are sent with your request.").asJson());
            writer.close();
        } else if (!isLoginRequest && !isSSEStreamRequests && !Login.authCheck(session)) {
            // Session is not valid

            LOGGER.warn("Unauthorized Request to " + ((HttpServletRequest) request).getRequestURI() + "- invalid session");
            ((HttpServletResponse) response).setStatus(401);
            ((HttpServletResponse) response).setHeader("Content-Type", MediaType.APPLICATION_JSON);

            final PrintWriter writer = response.getWriter();
            writer.write(new SimpleMessage("Error",
                    "Session token is not valid. You may need to " +
                            "login again.").asJson());
            writer.close();
        } else {
            // Valid Request
            chain.doFilter(request, response);
        }
    }

    @Override public void destroy() {
        // Intentionally Blank
    }
}
