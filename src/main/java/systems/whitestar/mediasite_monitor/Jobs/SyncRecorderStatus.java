package systems.whitestar.mediasite_monitor.Jobs;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.quartz.*;
import systems.whitestar.mediasite_monitor.Models.Recorder;
import systems.whitestar.mediasite_monitor.Models.Status;
import systems.whitestar.mediasite_monitor.DB;
import systems.whitestar.mediasite_monitor.Hooks.Hook;
import systems.whitestar.mediasite_monitor.Mediasite.Recorders;

import java.io.IOException;
import java.sql.Timestamp;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @author Tom Paulus
 * Created on 5/14/17.
 */
@Log4j
@NoArgsConstructor
@AllArgsConstructor
public class SyncRecorderStatus implements Job {
    public static final String JOB_GROUP = "recorder_status";
    public static final String TRIGGER_NAME_STEM = "SyncRecorderStatusTrigger";
    public static final String JOB_NAME_STEM = "SyncRecorderStatus";

    private String mRecorderID;

    /**
     * Schedule the Sync Job
     *
     * @param scheduler         {@link Scheduler} Quartz Scheduler Instance
     * @param intervalInMinutes How often the job should run in Minutes
     * @throws SchedulerException Something went wrong scheduling the job
     */
    public void schedule(Scheduler scheduler, int intervalInMinutes) throws SchedulerException {
        JobDetail job = newJob(this.getClass())
                .withIdentity(JOB_NAME_STEM + "-" + mRecorderID, JOB_GROUP)
                .withDescription(mRecorderID)
                .build();

        // Trigger the job to run now, and then repeat every X Minutes
        Trigger trigger = newTrigger()
                .withIdentity(TRIGGER_NAME_STEM + "-" + mRecorderID, JOB_GROUP)
                .withSchedule(simpleSchedule()
                        .withIntervalInMinutes(intervalInMinutes)
                        .repeatForever())
                .startNow()
                .build();

        // Tell quartz to schedule the job using our trigger
        scheduler.scheduleJob(job, trigger);
        log.debug("Scheduled Sync for Recorder with ID - " + this.mRecorderID);
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        this.mRecorderID = context.getJobDetail().getDescription();
        log.info("Fetching Recorder Status for Recorder with ID: " + this.mRecorderID);

        Recorder recorder;
        try {
            recorder = DB.getRecorder("id = '" + this.mRecorderID + "'")[0];
        } catch (IndexOutOfBoundsException e) {
            log.error("Could not locate Recorder Record in DB for recorder ID - " + this.mRecorderID);
            return;
        }

        Status previousStatus = recorder.getStatus();
        Status currentStatus = null;

        try {
            currentStatus = Recorders.getRecorderStatus(Recorders.getRecorderIP(this.mRecorderID));
        } catch (RuntimeException e) {
            log.error("Problem retrieving recorder status from API - Invalid IP", e);
        }

        if (currentStatus == null) {
            log.error("Problem retrieving recorder status from API/Recorder");
            currentStatus = Status.UNKNOWN;
        }

        log.debug(String.format("Recorder Status is \"%s\"", currentStatus));
        recorder.setStatus(currentStatus);
        recorder.setLastSeen(new Timestamp(System.currentTimeMillis()));
        DB.updateRecorder(recorder);
        log.info("Finished Updating Recorder Status for Recorder with ID: " + this.mRecorderID);

        try {
            if ((previousStatus == null || previousStatus.okay()) && currentStatus.inAlarm()) {
                log.warn("Recorder " + mRecorderID + "has entered ALARM state!");
                Hook.fire(Hook.RECORDER_ALARM_ACTIVATE, new Recorder(mRecorderID, currentStatus));
            } else if ((previousStatus == null || previousStatus.inAlarm()) && currentStatus.okay()) {
                log.info("Recorder" + mRecorderID + " has cleared ALARM state and is now OKAY.");
                Hook.fire(Hook.RECORDER_ALARM_CLEAR, new Recorder(mRecorderID, currentStatus));
            }
        } catch (IOException e) {
            log.error("Problem firing Alarm Status Update Hook", e);
        }

        try {
            Hook.fire(Hook.RECORDER_STATUS_UPDATE, DB.getRecorder("id='" + mRecorderID + "'")[0]);
        } catch (IOException e) {
            log.error("Problem firing Recorder Status Update Hook", e);
        }
    }
}
