package systems.whitestar.mediasite_monitor.Auth;

import org.pac4j.core.client.Client;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.exception.HttpAction;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Force the Super User Login page to be shown to the user, even if they are currently logged in.
 * This makes things nice and straight forward for the user if they are trying to enter Super User mode, as
 * otherwise they would need to logout and then go back and go to the Super User login context.
 */
public class ForceLoginFilter implements Filter {

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final J2EContext context = new J2EContext(((HttpServletRequest) request), ((HttpServletResponse) response));
        context.getRequest().getSession().invalidate();
        final Client client = LoginConfigFactory.getConfig().getClients().findClient("FormClient");
        try {
            //noinspection ThrowableNotThrown
            client.redirect(context);
        } catch (final HttpAction ignored) {
        }
    }

    @Override public void destroy() {

    }
}