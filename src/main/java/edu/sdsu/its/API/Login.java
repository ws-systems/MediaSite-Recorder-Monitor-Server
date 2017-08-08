package edu.sdsu.its.API;

import com.google.gson.Gson;
import edu.sdsu.its.API.Models.SimpleMessage;
import edu.sdsu.its.API.Models.User;
import edu.sdsu.its.DB;
import org.apache.log4j.Logger;

import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.HashMap;

/**
 * @author Tom Paulus
 * Created on 7/14/17.
 */
@Path("/")
public class Login {
    private static final Logger LOGGER = Logger.getLogger(Login.class);

    @Context
    private HttpServletRequest request;

    /**
     * Check if a provided Session is Valid for Authentication
     *
     * @param session {@link HttpSession} Request Session
     * @return True if session is valid, false otherwise
     */
    static boolean authCheck(HttpSession session) {
        return session != null && (session.getAttribute("user") instanceof User);
    }

    @Path("login")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response loginUser(@FormParam("email") final String email,
                              @FormParam("password") String password) throws URISyntaxException {
        if (email == null || email.isEmpty() ||
                password == null || password.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SimpleMessage("error",
                            "Email or Password not included in request")
                            .asJson()
                    ).build();
        }

        try {
            password = new String(Base64.getDecoder().decode(password));

            User user = DB.loginUser(email, password);

            if (user != null) {
                LOGGER.debug("User \"" + email + "\" has logged-in successfully!");
                request.getSession().removeAttribute("login-status");
                request.getSession().setAttribute("user", user);

                String redirectAfter = (String) request.getSession().getAttribute("post-login-redirect");

                return Response.seeOther(redirectAfter == null ? new URI("/") : new URI(redirectAfter)).build();
            } else {
                LOGGER.info("User \"" + email + "\" login attempt FAILED");
                request.getSession().setAttribute("login-status", "failed");
                return Response.seeOther(new URI("/login")).build();
            }
        } catch (Exception e) {
            LOGGER.warn("Problem Logging In User", e);
        }
        request.getSession().setAttribute("login-status", "error");
        return Response.seeOther(new URI("/login")).build();
    }

    @Path("login")
    @GET
    public Response redirectLoginGETRequest() throws URISyntaxException {
        return Response.seeOther(new URI("/login")).build();
    }

    @Path("logout")
    @GET
    public Response logoutUser() throws URISyntaxException {
        request.getSession().removeAttribute("user");
        request.getSession().setAttribute("login-status", "logged out");
        return Response.seeOther(new URI("/login")).build();
    }

    @Path("login/change")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changePassword(@FormParam("current") String currentPassword,
                                   @FormParam("new") String newPassword) {
        if (currentPassword == null || currentPassword.isEmpty() ||
                newPassword == null || newPassword.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SimpleMessage("error",
                            "current or new password not included in request")
                            .asJson()
                    ).build();
        }

        try {
            User sessionUser = (User) request.getSession().getAttribute("user");

            currentPassword = new String(Base64.getDecoder().decode(currentPassword));
            User user = DB.loginUser(sessionUser.getEmail(), currentPassword);

            if (user == null || !user.getEmail().equals(sessionUser.getEmail())) {
                return Response.status(Response.Status.UNAUTHORIZED).entity(new Gson().toJson(new HashMap<String, String>() {{
                    put("status", "Error");
                    put("message", "Current Password Not Correct");
                }})).build();
            }

            sessionUser.setPassword(newPassword);
            try {
                DB.updateUser(sessionUser);
            } catch (PersistenceException e) {
                return Response.status(Response.Status.PRECONDITION_FAILED).entity(new SimpleMessage("error",
                        "Could not update User, Precondition Failed. This may be caused by a duplicate email").asJson()).build();
            }

            return Response.status(Response.Status.OK).entity(new SimpleMessage("OK", "Password Updated").asJson()).build();
        } catch (Exception e) {
            LOGGER.warn("Problem changing user's password", e);
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new SimpleMessage("Error", "Something went wrong unexpectedly. Check Application logs for details.").asJson()).build();
    }
}
