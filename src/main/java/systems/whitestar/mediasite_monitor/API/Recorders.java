package systems.whitestar.mediasite_monitor.API;

import com.google.gson.Gson;
import lombok.extern.log4j.Log4j;
import org.pac4j.jax.rs.annotations.Pac4JSecurity;
import systems.whitestar.mediasite_monitor.API.Models.Recorder;
import systems.whitestar.mediasite_monitor.API.Models.SimpleMessage;
import systems.whitestar.mediasite_monitor.DB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Tom Paulus
 * Created on 5/10/17.
 */
@Log4j
@Path("recorders")
@Pac4JSecurity(authorizers = "isAuthenticated")
public class Recorders {
    /**
     * Get All recorders from the DB
     *
     * @return {@link Response} Recorder Array (empty if none in DB)
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllRecorders() {
        Gson gson = new Gson();
        log.info("[GET] Received Request for All Recorders");


        Recorder[] recorders = DB.getRecorder("");
        log.info(String.format("Retrieved %d recorders from DB", recorders.length));
        return Response.status(Response.Status.OK).entity(gson.toJson(recorders)).build();
    }


    /**
     * Get a specific recorder from the DB
     *
     * @param recorderID {@link String} Recorder ID to return
     * @return {@link Response} Recorder Object if found, else SimpleMessage with Error
     */
    @Path("/{id}")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRecorder(@PathParam("id") String recorderID) {
        Gson gson = new Gson();
        log.info("[GET] Received Request for Recorders Where ID=" + (recorderID == null ? "" : recorderID));


        final String restriction = String.format("id = \'%s\'", recorderID);
        log.debug("Recorder Query Restriction - " + restriction);

        Recorder[] recorders = DB.getRecorder(restriction);
        if (recorders.length > 0) {
            log.info("Retrieved recorder from DB");
            return Response.status(Response.Status.OK).entity(gson.toJson(recorders[0])).build();
        }
        return Response.status(Response.Status.NOT_FOUND).entity(gson.toJson(new SimpleMessage("Error", "No recorders with that ID were found"))).build();
    }
}
