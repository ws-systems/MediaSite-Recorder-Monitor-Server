package edu.sdsu.its.API;

import com.google.gson.Gson;
import edu.sdsu.its.API.Models.Preference;
import edu.sdsu.its.API.Models.SimpleMessage;
import edu.sdsu.its.API.Models.User;
import edu.sdsu.its.DB;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;

/**
 * API Endpoints associated with External Integrations, like Mediasite and Email.
 * Used to modify the settings for these services, like access credentials, etc.
 *
 * @author Tom Paulus
 * Created on 8/1/17.
 */
@Path("integrations")
public class Integrations {
    private static final Logger LOGGER = Logger.getLogger(Integrations.class);

    @Context
    private HttpServletRequest request;

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSetting(final String payload) {
        if (payload == null || payload.isEmpty())
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SimpleMessage("Error",
                            "No payload supplied").asJson())
                    .build();

        LOGGER.debug("Received Payload:" + payload);

        Preference[] preferences = new Gson().fromJson(payload, Preference[].class);
        LOGGER.debug(String.format("Requested Updates to %d settings", preferences.length));
        LOGGER.debug(Arrays.toString(preferences));

        for (Preference preference : preferences) {
            final String current = DB.getPreference(preference.getSetting());
            if (current == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new SimpleMessage("Error",
                                String.format("\"%s\" if not a valid setting name", preference.getSetting())).asJson())
                        .build();
            }

            if (!current.equals(preference.getValue())) {
                // Setting has been modified
                LOGGER.warn(String.format("User \"%s\" is updating the setting with name \"%s\"from \"%s\" to \"%s\"",
                        ((User) request.getSession().getAttribute("user")).getEmail(),
                        preference.getSetting(),
                        current,
                        preference.getValue()));

                DB.setPreference(preference.getSetting(), preference.getValue());
            }
        }

        return Response.status(Response.Status.ACCEPTED).entity(new SimpleMessage("okay",
                "Settings have been updated").asJson())
                .build();
    }
}
