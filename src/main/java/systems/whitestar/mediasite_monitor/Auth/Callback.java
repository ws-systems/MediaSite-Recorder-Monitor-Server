package systems.whitestar.mediasite_monitor.Auth;

import lombok.extern.log4j.Log4j;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.jax.rs.annotations.Pac4JCallback;
import org.pac4j.jax.rs.annotations.Pac4JProfile;
import systems.whitestar.mediasite_monitor.DB;
import systems.whitestar.mediasite_monitor.Models.User;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import java.util.Optional;

/**
 * @author Tom Paulus
 * Created on 6/18/18.
 */
@Log4j
@Path("callback")
public class Callback {

    @GET
    @Pac4JCallback(skipResponse = false, renewSession = true, defaultUrl = "../app", multiProfile = false)
    public User loginCB(@Context HttpServletRequest request,
                        @Pac4JProfile Optional<CommonProfile> profile) {
        if (profile.isPresent()) {
            User user;

            if (profile.get().getClientName().equals("FormClient")) {
                // SuperUser Login
                log.warn("Enabling Super User Mode!");
                user = User.builder()
                        .name("Super User")
                        .notify(false)
                        .admin(true)
                        .email("support@whitestar.systems")
                        .PK(-1)
                        .build();

                request.getSession().setAttribute("user", user);
                request.getSession().setAttribute("superuser", true);

                profile.get().addRole("admin");
            } else {
                // Regular OpenID Login
                final User[] users = DB.getUser("external_id = '" + profile.get().getId() + "'");

                if (users.length == 0) {
                    // Need to Create User
                    log.info(String.format("User with email %s does not yet exist - creating", profile.get().getEmail()));
                    user = User.builder()
                            .name((String) profile.get().getAttribute("name"))
                            .email(profile.get().getEmail())
                            .externalId(profile.get().getId())
                            .notify(false)
                            .admin(false)
                            .build();
                    DB.updateUser(user);
                    request.getSession().setAttribute("first_login", true);
                } else {
                    // Incomplete Session
                    // Update User with SSO Provider Information
                    user = User.builder()
                            .name((String) profile.get().getAttribute("name"))
                            .email(profile.get().getEmail())
                            .externalId(profile.get().getId())
                            .build();

                    user = User.merge(users[0], user);
                    DB.updateUser(user);
                    request.getSession().setAttribute("first_login", false);
                }

                request.getSession().setAttribute("user", user);
            }

            if (user.getAdmin())
                profile.get().addRole("ROLE_ADMIN");
            else
                profile.get().addRole("ROLE_USER");

            return user;
        } else {
            throw new WebApplicationException(401);
        }
    }

    @POST
    @Pac4JCallback(skipResponse = false, renewSession = true, defaultUrl = "../app", multiProfile = false)
    public User loginCBPOST(@Context HttpServletRequest request,
                        @Pac4JProfile Optional<CommonProfile> profile) {
        return loginCB(request, profile);
    }

}
