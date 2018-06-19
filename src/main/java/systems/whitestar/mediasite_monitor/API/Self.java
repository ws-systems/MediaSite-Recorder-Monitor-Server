package systems.whitestar.mediasite_monitor.API;

import com.google.gson.Gson;
import lombok.extern.log4j.Log4j;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.jax.rs.annotations.Pac4JProfile;
import org.pac4j.jax.rs.annotations.Pac4JSecurity;
import systems.whitestar.mediasite_monitor.DB;
import systems.whitestar.mediasite_monitor.Hooks.Hook;
import systems.whitestar.mediasite_monitor.Models.SimpleMessage;
import systems.whitestar.mediasite_monitor.Models.User;

import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Operations to the Logged In User's Profile
 *
 * @author Tom Paulus
 * Created on 6/7/18.
 */
@Log4j
@Path("me")
@Pac4JSecurity(authorizers = "isAuthenticated")
public class Self {
    @Context
    private HttpServletRequest request;

    /**
     * Subscribe the currently logged-in user to Recorder Notifications
     *
     * @return {@link Response} Status Message
     */
    @Path("subscribe")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response subscribeSelf(@Pac4JProfile CommonProfile profile) {
        log.info(String.format("Subscribing %s to Recorder Notifications", profile.getEmail()));
        User[] users = DB.getUser("external_id = '" + profile.getId() + "'");
        if (users.length != 1) {
            return Response.status(Response.Status.BAD_REQUEST).entity(
                    new SimpleMessage("Error",
                            "Current User does not exist. Try logging out and in again.").asJson()
            ).build();
        }

        users[0].setNotify(true);
        DB.updateUser(users[0]);

        return Response.accepted().build();
    }

    /**
     * Update the currently logged-in user's profile
     *
     * @param payload {@link String} User JSON Object
     * @return {@link Response} Updated User JSON
     */
    @Path("profile")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSelf(@Pac4JProfile CommonProfile profile,
                               final String payload) {
        final User existingUser;
        try {
            existingUser = DB.getUser("email = '" + profile.getEmail() + "'")[0];
        } catch (IndexOutOfBoundsException e) {
            log.info(String.format("Could not find a user with email \"%s\"", profile.getEmail()));
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new SimpleMessage("Error",
                            "No user with that email could be found in the system").asJson())
                    .build();
        }

        User newUser = new Gson().fromJson(payload, User.class);

        final User mergedUser = User.merge(existingUser, newUser);
        try {
            DB.updateUser(mergedUser);
        } catch (PersistenceException e) {
            return Response.status(Response.Status.PRECONDITION_FAILED).entity(new SimpleMessage("error",
                    "Could not update User, Precondition Failed. This may be caused by a duplicate email").asJson()).build();
        }
        try {
            Hook.fire(Hook.USER_UPDATE, mergedUser);
        } catch (IOException e) {
            log.error("Problem firing User Update Hook", e);
        }
        return Response.status(Response.Status.ACCEPTED).entity(new SimpleMessage(
                "okay",
                "User updated successfully!"
        ).asJson()).build();
    }
}
