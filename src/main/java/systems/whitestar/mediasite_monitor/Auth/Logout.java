package systems.whitestar.mediasite_monitor.Auth;

import org.pac4j.jax.rs.annotations.Pac4JLogout;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * @author Tom Paulus
 * Created on 6/18/18.
 */
@Path("logout")
public class Logout {

    @GET
    @Pac4JLogout(skipResponse = false, destroySession = true, defaultUrl = "../app")
    public void logout() {
        // do nothing
    }
}
