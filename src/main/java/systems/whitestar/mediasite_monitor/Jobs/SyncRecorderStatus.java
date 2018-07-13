package systems.whitestar.mediasite_monitor.Jobs;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.quartz.*;
import systems.whitestar.mediasite_monitor.DB;
import systems.whitestar.mediasite_monitor.Hooks.Hook;
import systems.whitestar.mediasite_monitor.Models.AgentJob;
import systems.whitestar.mediasite_monitor.Models.DefaultAgentFilter;
import systems.whitestar.mediasite_monitor.Models.Recorder;
import systems.whitestar.mediasite_monitor.Models.Status;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Query recorders directly via their built-in API to determine their status.
 * This is a higher priority job.
 *
 * @author Tom Paulus
 * Created on 5/14/17.
 */
@Log4j
@NoArgsConstructor
public class SyncRecorderStatus implements AgentJobInterface, Job {
    public static final String JOB_GROUP = "recorder_status";
    public static final String TRIGGER_NAME_STEM = "SyncRecorderStatusTrigger";
    public static final String JOB_NAME_STEM = "SyncRecorderStatus";

    /**
     * Schedule the Sync Job
     *
     * @param scheduler         {@link Scheduler} Quartz Scheduler Instance
     * @param intervalInMinutes How often the job should run in Minutes
     * @param recorderID        {@link String} Recorder whose status should be updated
     * @throws SchedulerException Something went wrong scheduling the job
     */
    public static void schedule(Scheduler scheduler, int intervalInMinutes, String recorderID) throws SchedulerException {
        JobDetail job = newJob(SyncRecorderStatus.class)
                .withIdentity(JOB_NAME_STEM + "-" + recorderID, JOB_GROUP)
                .withDescription(recorderID)
                .build();

        // Trigger the job to run now, and then repeat every X Minutes
        Trigger trigger = newTrigger()
                .withIdentity(TRIGGER_NAME_STEM + "-" + recorderID, JOB_GROUP)
                .withSchedule(simpleSchedule()
                        .withIntervalInMinutes(intervalInMinutes)
                        .repeatForever())
                .startNow()
                .build();

        // Tell quartz to schedule the job using our trigger
        scheduler.scheduleJob(job, trigger);
        log.debug("Scheduled Sync for Recorder with ID - " + recorderID);
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        final String recorderID = context.getJobDetail().getDescription();
        log.info("Fetching Recorder Status for Recorder with ID: " + recorderID);

        Map<String, String> payload = new HashMap<String, String>() {{
            put("recorderID", recorderID);
        }};

        AgentJob job = this.create(payload);
        DB.updateAgentJob(job);
    }

    public AgentJob create(Map<String, String> payload) {
        String recorderID = payload.get("recorderID");
        log.info("Fetching Recorder Status for Recorder with ID: " + recorderID);

        return AgentJob.builder()
                .job(this.getClass())
                .priority(10) // Medium Priority
                .payload(payload)
                .agentFilter(DefaultAgentFilter.class)
                .build();
    }

    public void process(Map<String, String> payload) {
        String recorderID = payload.get("recorderID");

        Recorder recorder;
        try {
            recorder = DB.getRecorder("id = '" + recorderID + "'")[0];
        } catch (IndexOutOfBoundsException e) {
            log.error("Could not locate Recorder Record in DB for recorder ID - " + recorderID);
            return;
        }

        Status previousStatus = recorder.getStatus();
        Status currentStatus = null;

        try {
            currentStatus = new Gson().fromJson(payload.get("status"), Status.class);
        } catch (JsonSyntaxException e) {
            log.error("Problem retrieving recorder status from agent - invalid json response", e);
        }

        if (currentStatus == null) {
            log.warn("Problem retrieving recorder status from API/Recorder");
            int retry_max = Integer.parseInt(Objects.requireNonNull(DB.getPreference("sync_recorder.retry_count")));
            if (recorder.getRetryCount() >= retry_max) {
                log.warn("Retry count exceed for recorder - " + recorder.getName());
                currentStatus = Status.UNKNOWN;
            } else {
                // Retry Count not met yet, we can try again
                log.warn(String.format("Will retry %d more times", recorder.getRetryCount() - retry_max));
                log.info("Hook Fire suppressed!");
                recorder.setRetryCount(recorder.getRetryCount() + 1);
                DB.updateRecorder(recorder);

                log.info("Finished Updating Recorder Status for Recorder with ID: " + recorderID);
                return;
            }
        } else {
            recorder.setRetryCount(0);
        }

        log.debug(String.format("Recorder Status is \"%s\"", currentStatus));
        recorder.setStatus(currentStatus);
        recorder.setLastSeen(new Timestamp(System.currentTimeMillis()));
        DB.updateRecorder(recorder);
        log.info("Finished Updating Recorder Status for Recorder with ID: " + recorderID);

        try {
            if ((previousStatus == null || previousStatus.okay()) && currentStatus.inAlarm()) {
                log.warn("Recorder " + recorderID + "has entered ALARM state!");
                Hook.fire(Hook.RECORDER_ALARM_ACTIVATE, new Recorder(recorderID, currentStatus));
            } else if ((previousStatus == null || previousStatus.inAlarm()) && currentStatus.okay()) {
                log.info("Recorder" + recorderID + " has cleared ALARM state and is now OKAY.");
                Hook.fire(Hook.RECORDER_ALARM_CLEAR, new Recorder(recorderID, currentStatus));
            }
        } catch (IOException e) {
            log.error("Problem firing Alarm Status Update Hook", e);
        }

        try {
            Hook.fire(Hook.RECORDER_STATUS_UPDATE, DB.getRecorder("id='" + recorderID + "'")[0]);
        } catch (IOException e) {
            log.error("Problem firing Recorder Status Update Hook", e);
        }
    }
}
