package edu.sdsu.its.API;

import com.google.gson.Gson;
import edu.sdsu.its.API.Models.SimpleMessage;
import edu.sdsu.its.API.Models.User;
import edu.sdsu.its.DB;
import edu.sdsu.its.Hooks.Hook;
import org.apache.log4j.Logger;

import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * @author Tom Paulus
 * Created on 7/29/17.
 */
@Path("users")
public class Users {
    private static final Logger LOGGER = Logger.getLogger(Users.class);

    @Context
    private HttpServletRequest request;

    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllUsers() {
        LOGGER.debug("Getting All Users");
        final User[] users = DB.getUser("");
        LOGGER.debug(String.format("Found %d Users", users.length));

        return Response.status(Response.Status.OK).entity(new Gson().toJson(users)).build();
    }

    /**
     * Get a User from the DB
     *
     * @param userEmail {@link String} User's Email
     * @return Serialized User Object, SimpleMessage JSON with error if not found
     */
    @Path("{id}")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam("id") final String userEmail) {
        if (userEmail == null || userEmail.isEmpty())
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SimpleMessage("Error",
                            "No Email supplied in request").asJson())
                    .build();

        LOGGER.debug("Getting User with Email - " + userEmail);
        final User user;

        try {
            user = DB.getUser("email = '" + userEmail + "'")[0];
        } catch (IndexOutOfBoundsException e) {
            LOGGER.info(String.format("Could not find a user with email \"%s\"", userEmail));
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new SimpleMessage("Error",
                            "No user with that email could be found in the system").asJson())
                    .build();
        }

        return Response.status(Response.Status.OK).entity(new Gson().toJson(user)).build();
    }

    /**
     * Create a new User. Email addresses must be unique per user.
     *
     * @param payload {@link String} Updated User Object JSON
     * @return {@link Response} Status Message (SimpleMessage) JSON
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createUser(final String payload) {
        if (payload == null || payload.isEmpty())
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SimpleMessage("Error",
                            "No payload supplied").asJson())
                    .build();

        User user = new Gson().fromJson(payload, User.class);
        if (!user.complete()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SimpleMessage("Error",
                            "Incomplete payload supplied").asJson())
                    .build();
        }
        user.setPassword(user.getPassword()); // Hash Password for DB;

        try {
            DB.updateUser(user);
        } catch (PersistenceException e) {
            return Response.status(Response.Status.PRECONDITION_FAILED).entity(new SimpleMessage("error",
                    "Could not create User, Precondition Failed. This may be caused by a duplicate email").asJson()).build();
        }
        try {
            Hook.fire(Hook.USER_CREATE, user);
        } catch (IOException e) {
            LOGGER.error("Problem firing User Create Hook", e);
        }
        return Response.status(Response.Status.CREATED).entity(user.asJson()).build();
    }

    /**
     * Update a User.
     * Only fields that have been defined will be updated. Fields that are null will not be updated.
     *
     * @param userEmail {@link String} User Email
     * @param payload   {@link String} Updated User Object JSON
     * @return {@link Response} Status Message (SimpleMessage) JSON
     */
    @Path("{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUser(@PathParam("id") final String userEmail,
                               final String payload) {
        if (userEmail == null || userEmail.isEmpty())
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SimpleMessage("Error",
                            "No Email supplied in request").asJson())
                    .build();

        final User existingUser;
        try {
            existingUser = DB.getUser("email = '" + userEmail + "'")[0];
        } catch (IndexOutOfBoundsException e) {
            LOGGER.info(String.format("Could not find a user with email \"%s\"", userEmail));
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
            LOGGER.error("Problem firing User Update Hook", e);
        }
        return Response.status(Response.Status.ACCEPTED).entity(new SimpleMessage(
                "okay",
                "User updated successfully!"
        ).asJson()).build();
    }

    /**
     * Delete a User from the Database
     *
     * @param userEmail {@link String} User's Email Address
     * @return {@link Response} Status Message (SimpleMessage) JSON
     */
    @Path("{id}")
    @DELETE
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUser(@PathParam("id") final String userEmail) {
        if (userEmail == null || userEmail.isEmpty())
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SimpleMessage("Error",
                            "No Email supplied in request").asJson())
                    .build();

        LOGGER.warn(String.format("User \"%s\" is requesting to delete the User with Email \"%s\"",
                ((User) request.getSession().getAttribute("user")).getEmail(),
                userEmail));

        try {
            final User user = DB.getUser("email = '" + userEmail + "'")[0];
            DB.deleteUser(user);

            try {
                Hook.fire(Hook.USER_UPDATE, user);
            } catch (IOException e) {
                LOGGER.error("Problem firing User Update Hook", e);
            }
        } catch (IndexOutOfBoundsException e) {
            LOGGER.info(String.format("Could not find a user with email \"%s\"", userEmail));
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new SimpleMessage("Error",
                            "No user with that email could be found in the system").asJson())
                    .build();
        }

        return Response.status(Response.Status.ACCEPTED).entity(new SimpleMessage(
                "okay",
                String.format("User with email \"%s\" has been deleted", userEmail))
                .asJson()).build();
    }
}
