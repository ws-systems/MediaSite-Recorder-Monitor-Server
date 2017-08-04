package edu.sdsu.its.API;

import com.google.gson.Gson;
import edu.sdsu.its.API.Models.Preference;
import edu.sdsu.its.API.Models.SimpleMessage;
import edu.sdsu.its.API.Models.User;
import edu.sdsu.its.DB;
import edu.sdsu.its.Jobs.SyncRecorderDB;
import edu.sdsu.its.Jobs.SyncRecorderStatus;
import edu.sdsu.its.Schedule;
import org.apache.log4j.Logger;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Set;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

/**
 * API Endpoints associated with Job Rates and Pausing/Resuming Job Triggers.
 *
 * @author Tom Paulus
 * Created on 8/2/17.
 */
@SuppressWarnings("unchecked")
@Path("rates")
public class Rates {
    private static final Logger LOGGER = Logger.getLogger(Rates.class);

    @Context
    private HttpServletRequest request;

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateRates(final String payload) {
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

                try {
                    switch (preference.getSetting()) {
                        case "sync_db.enable":
                            updateListTrigger(Boolean.parseBoolean(preference.getValue()));
                            break;

                        case "sync_db.frequency":
                            updateListSchedule(Integer.parseInt(preference.getValue()));
                            break;

                        case "sync_recorder.enable":
                            updateStatusTrigger(Boolean.parseBoolean(preference.getValue()));
                            break;

                        case "sync_recorder.frequency":
                            updateStatusSchedule(Integer.parseInt(preference.getValue()));
                            break;
                    }
                } catch (SchedulerException e) {
                    LOGGER.error("Problem Updating Job Triggers", e);
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new SimpleMessage("error",
                            "Something went unexpectedly wrong updating the job rates and triggers. Sorry!").asJson())
                            .build();
                }
            }
        }

        return Response.status(Response.Status.ACCEPTED).entity(new SimpleMessage("okay",
                "Rates have been updated").asJson())
                .build();

    }

    private void updateListTrigger(final boolean enabled) throws SchedulerException {
        final Scheduler scheduler = Schedule.getScheduler();

        final GroupMatcher<TriggerKey> matcher = GroupMatcher.groupEquals(SyncRecorderDB.JOB_GROUP);
        if (enabled) {
            LOGGER.info("Recorder List Trigger is being Enabled");
            scheduler.resumeTriggers(matcher);
        } else {
            LOGGER.warn("Recorder List Trigger is being Disabled");
            scheduler.pauseTriggers(matcher);
        }
    }

    private void updateStatusTrigger(final boolean enabled) throws SchedulerException {
        final Scheduler scheduler = Schedule.getScheduler();

        final GroupMatcher<TriggerKey> matcher = GroupMatcher.groupEquals(SyncRecorderStatus.JOB_GROUP);
        if (enabled) {
            LOGGER.info("Recorder Status Trigger is being Enabled");
            scheduler.resumeTriggers(matcher);
        } else {
            LOGGER.warn("Recorder Status Trigger is being Disabled");
            scheduler.pauseTriggers(matcher);
        }
    }

    private void updateListSchedule(final int frequency) throws SchedulerException {
        LOGGER.info(String.format("Rescheduling List Sync Job to run every %d minutes", frequency));

        final Scheduler scheduler = Schedule.getScheduler();
        LOGGER.debug("Paused Trigger Groups: " + Arrays.toString(scheduler.getPausedTriggerGroups().toArray()));

        // retrieve the current triggers
        Set<TriggerKey> triggerKeys = scheduler.getTriggerKeys(GroupMatcher.triggerGroupStartsWith(SyncRecorderDB.TRIGGER_NAME));

        if (triggerKeys.size() == 0) {
            LOGGER.warn("Couldn't find any Triggers for List Sync Job - Aborting Update to trigger - Does not exist");
            return;
        }


        for (TriggerKey oldKey : triggerKeys) {
            Trigger oldTrigger = scheduler.getTrigger(oldKey);

            // obtain a builder that would produce the trigger
            TriggerBuilder tb = oldTrigger.getTriggerBuilder();

            // update the schedule associated with the builder, and build the new trigger
            // (other builder methods could be called, to change the trigger in any desired way)
            Trigger newTrigger = tb
                    .withIdentity(oldTrigger.getKey().getName(), oldTrigger.getKey().getGroup())
                    .withSchedule(simpleSchedule()
                            .withIntervalInMinutes(frequency)
                            .repeatForever())
                    .startNow()
                    .build();

            scheduler.rescheduleJob(oldKey, newTrigger);
        }
    }

    private void updateStatusSchedule(final int frequency) throws SchedulerException {
        LOGGER.info(String.format("Rescheduling Status Sync Job to run every %d minutes", frequency));

        final Scheduler scheduler = Schedule.getScheduler();
        LOGGER.debug("Paused Trigger Groups: " + Arrays.toString(scheduler.getPausedTriggerGroups().toArray()));

        // retrieve the current triggers
        Set<TriggerKey> triggerKeys = scheduler.getTriggerKeys(GroupMatcher.triggerGroupStartsWith(SyncRecorderStatus.JOB_GROUP));

        if (triggerKeys.size() == 0) {
            LOGGER.warn("Couldn't find any Triggers for Status Sync Job - Aborting Update to trigger - Does not exist");
            return;
        }

        for (TriggerKey oldKey : triggerKeys) {
            Trigger oldTrigger = scheduler.getTrigger(oldKey);

            // obtain a builder that would produce the trigger
            TriggerBuilder tb = oldTrigger.getTriggerBuilder();

            // update the schedule associated with the builder, and build the new trigger
            // (other builder methods could be called, to change the trigger in any desired way)
            Trigger newTrigger = tb
                    .withIdentity(oldTrigger.getKey().getName(), oldTrigger.getKey().getGroup())
                    .withSchedule(simpleSchedule()
                            .withIntervalInMinutes(frequency)
                            .repeatForever())
                    .startNow()
                    .build();

            scheduler.rescheduleJob(oldKey, newTrigger);
        }
    }
}
