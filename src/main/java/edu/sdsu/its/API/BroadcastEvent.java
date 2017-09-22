package edu.sdsu.its.API;

import com.google.gson.GsonBuilder;
import edu.sdsu.its.API.Models.Recorder;
import edu.sdsu.its.API.Models.Status;
import edu.sdsu.its.Hooks.Hook;
import lombok.ToString;
import org.apache.log4j.Logger;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;
import org.glassfish.jersey.media.sse.SseFeature;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

/**
 * @author Tom Paulus
 * Created on 9/15/17.
 */
@Singleton
@Path("stream")
public class BroadcastEvent {
    private static final Logger LOGGER = Logger.getLogger(BroadcastEvent.class);
    private static SseBroadcaster broadcaster = new SseBroadcaster();

    static void broadcastEvent(Event event) {
        OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
        OutboundEvent outboundEvent = eventBuilder.name("message")
                .mediaType(MediaType.TEXT_PLAIN_TYPE)
                .data(String.class, event.asJson())
                .build();
        LOGGER.info(String.format("Broadcasting new Message to Clients - Type: %s; Recorder ID: %s", event.cause.getName(), event.recorder.getId()));
        LOGGER.debug(event);
        try {
            broadcaster.broadcast(outboundEvent);
        } catch (Exception e) {
            LOGGER.warn(String.format("Problem Broadcasting Event - Type: %s; Recorder ID: %s", event.cause.getName(), event.recorder.getId()));
        }
    }

    @GET
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public EventOutput listenToBroadcast() {
        final EventOutput eventOutput = new EventOutput();
        broadcaster.add(eventOutput);
        return eventOutput;
    }

    @ToString
    public static class Event {
        String id;
        Hook cause;
        Recorder recorder;

        public Event(Hook cause, Recorder recorder) {
            this.id = UUID.randomUUID().toString();
            this.cause = cause;
            this.recorder = recorder;
        }

        public String asJson() {
            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapterFactory(new Status.StatusAdapterFactory());
            return builder.create().toJson(this);        }

        public void broadcast() {
            broadcastEvent(this);
        }
    }
}
