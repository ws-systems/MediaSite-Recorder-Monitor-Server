package edu.sdsu.its.API;

import com.google.gson.Gson;
import edu.sdsu.its.API.Models.Recorder;
import edu.sdsu.its.API.Models.SimpleMessage;
import edu.sdsu.its.DB;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Tom Paulus
 *         Created on 5/10/17.
 */
@Path("recorders")
public class Recorders {
    private static final Logger LOGGER = Logger.getLogger(Recorders.class);

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
        LOGGER.info("[GET] Received Request for All Recorders");


        Recorder[] recorders = DB.getRecorder("");
        LOGGER.info(String.format("Retrieved %d recorders from DB", recorders.length));
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
        LOGGER.info("[GET] Received Request for Recorders Where ID=" + (recorderID == null ? "" : recorderID));


        final String restriction = String.format("id = \'%s\'", recorderID);
        LOGGER.debug("Recorder Query Restriction - " + restriction);

        Recorder[] recorders = DB.getRecorder(restriction);
        if (recorders.length > 0) {
            LOGGER.info("Retrieved recorder from DB");
            return Response.status(Response.Status.OK).entity(gson.toJson(recorders[0])).build();
        }
        return Response.status(Response.Status.NOT_FOUND).entity(gson.toJson(new SimpleMessage("Error", "No recorders with that ID were found"))).build();
    }
}
