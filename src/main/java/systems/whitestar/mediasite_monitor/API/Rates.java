package systems.whitestar.mediasite_monitor.API;

import com.google.gson.Gson;
import lombok.extern.log4j.Log4j;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.jax.rs.annotations.Pac4JProfile;
import org.pac4j.jax.rs.annotations.Pac4JSecurity;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import systems.whitestar.mediasite_monitor.DB;
import systems.whitestar.mediasite_monitor.Jobs.RecorderExpectationCheck;
import systems.whitestar.mediasite_monitor.Jobs.ScheduleExpectationChecks;
import systems.whitestar.mediasite_monitor.Jobs.SyncRecorderDB;
import systems.whitestar.mediasite_monitor.Jobs.SyncRecorderStatus;
import systems.whitestar.mediasite_monitor.Models.Preference;
import systems.whitestar.mediasite_monitor.Models.SimpleMessage;
import systems.whitestar.mediasite_monitor.Schedule;

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
@Log4j
@SuppressWarnings("unchecked")
@Path("rates")
@Pac4JSecurity(authorizers = "admin")
public class Rates {
    @Context
    private HttpServletRequest request;

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateRates(@Pac4JProfile CommonProfile profile,
                                final String payload) {
        if (payload == null || payload.isEmpty())
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new SimpleMessage("Error",
                            "No payload supplied").asJson())
                    .build();

        log.debug("Received Payload:" + payload);

        Preference[] preferences = new Gson().fromJson(payload, Preference[].class);
        log.debug(String.format("Requested Updates to %d settings", preferences.length));
        log.debug(Arrays.toString(preferences));

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
                log.warn(String.format("User \"%s\" is updating the setting with name \"%s\"from \"%s\" to \"%s\"",
                        profile.getAttribute("name"),
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

                        case "expectation_checks.enable":
                            updateExpectationTrigger(Boolean.parseBoolean(preference.getValue()));
                            break;

                        case "expectation_checks.time":
                            updateExpectationSchedule(
                                    Integer.parseInt(preference.getValue().split(":")[0]),
                                    Integer.parseInt(preference.getValue().split(":")[1])
                            );
                            break;
                    }
                } catch (SchedulerException e) {
                    log.error("Problem Updating Job Triggers", e);
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
            log.info("Recorder List Trigger is being Enabled");
            scheduler.resumeTriggers(matcher);
        } else {
            log.warn("Recorder List Trigger is being Disabled");
            scheduler.pauseTriggers(matcher);
        }
    }

    private void updateStatusTrigger(final boolean enabled) throws SchedulerException {
        final Scheduler scheduler = Schedule.getScheduler();

        final GroupMatcher<TriggerKey> matcher = GroupMatcher.groupEquals(SyncRecorderStatus.JOB_GROUP);
        if (enabled) {
            log.info("Recorder Status Trigger is being Enabled");
            scheduler.resumeTriggers(matcher);
        } else {
            log.warn("Recorder Status Trigger is being Disabled");
            scheduler.pauseTriggers(matcher);
        }
    }

    private void updateExpectationTrigger(final boolean enabled) throws SchedulerException {
        final Scheduler scheduler = Schedule.getScheduler();

        final GroupMatcher<TriggerKey> scheduleMatcher = GroupMatcher.groupEquals(ScheduleExpectationChecks.JOB_GROUP);
        final GroupMatcher<TriggerKey> expectationMatcher = GroupMatcher.groupContains(RecorderExpectationCheck.JOB_GROUP);

        if (enabled) {
            log.info("Expectation Schedule Pull Trigger is being Enabled");
            scheduler.resumeTriggers(scheduleMatcher);

            log.info("Already scheduled expectation checks (if any) are being resumed");
            scheduler.resumeTriggers(expectationMatcher);
        } else {
            log.warn("Expectation Schedule Pull Trigger is being Disabled");
            scheduler.pauseTriggers(scheduleMatcher);

            log.info("Pausing Already scheduled expectation checks (if any)");
            scheduler.pauseTriggers(expectationMatcher);
        }
    }

    private void updateListSchedule(final int frequency) throws SchedulerException {
        log.info(String.format("Rescheduling List Sync Job to run every %d minutes", frequency));

        final Scheduler scheduler = Schedule.getScheduler();
        log.debug("Paused Trigger Groups: " + Arrays.toString(scheduler.getPausedTriggerGroups().toArray()));

        // retrieve the current triggers
        Set<TriggerKey> triggerKeys = scheduler.getTriggerKeys(GroupMatcher.triggerGroupStartsWith(SyncRecorderDB.JOB_GROUP));

        if (triggerKeys.size() == 0) {
            log.warn("Couldn't find any Triggers for List Sync Job - Aborting Update to trigger - Does not exist");
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
        log.info(String.format("Rescheduling Status Sync Job to run every %d minutes", frequency));

        final Scheduler scheduler = Schedule.getScheduler();
        log.debug("Paused Trigger Groups: " + Arrays.toString(scheduler.getPausedTriggerGroups().toArray()));

        // retrieve the current triggers
        Set<TriggerKey> triggerKeys = scheduler.getTriggerKeys(GroupMatcher.triggerGroupStartsWith(SyncRecorderStatus.JOB_GROUP));

        if (triggerKeys.size() == 0) {
            log.warn("Couldn't find any Triggers for Status Sync Job - Aborting Update to trigger - Does not exist");
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

    private void updateExpectationSchedule(int hour, int minute) throws SchedulerException {
        log.info(String.format("Rescheduling Expectation Schedule Pull Job to run every day at %d:%d", hour, minute));

        final Scheduler scheduler = Schedule.getScheduler();
        log.debug("Paused Trigger Groups: " + Arrays.toString(scheduler.getPausedTriggerGroups().toArray()));

        // retrieve the current triggers
        Set<TriggerKey> triggerKeys = scheduler.getTriggerKeys(GroupMatcher.triggerGroupStartsWith(ScheduleExpectationChecks.JOB_GROUP));

        if (triggerKeys.size() == 0) {
            log.warn("Couldn't find any Triggers for Expectation Schedule Pull Job - Aborting Update to trigger - Does not exist");
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
                    .withSchedule(
                            CronScheduleBuilder.dailyAtHourAndMinute(hour, minute)
                    )
                    .startNow()
                    .build();

            scheduler.rescheduleJob(oldKey, newTrigger);
        }
    }
}
