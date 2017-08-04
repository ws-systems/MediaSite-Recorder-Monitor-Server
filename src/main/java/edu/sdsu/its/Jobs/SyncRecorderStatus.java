package edu.sdsu.its.Jobs;

import edu.sdsu.its.API.Models.Recorder;
import edu.sdsu.its.API.Models.Status;
import edu.sdsu.its.DB;
import edu.sdsu.its.Hooks.Hook;
import edu.sdsu.its.Mediasite.Recorders;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.log4j.Logger;
import org.quartz.*;

import java.io.IOException;
import java.sql.Timestamp;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @author Tom Paulus
 * Created on 5/14/17.
 */
@NoArgsConstructor
@AllArgsConstructor
public class SyncRecorderStatus implements Job {
    private static final Logger LOGGER = Logger.getLogger(SyncRecorderStatus.class);

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
        LOGGER.debug("Scheduled Sync for Recorder with ID - " + this.mRecorderID);
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        this.mRecorderID = context.getJobDetail().getDescription();
        LOGGER.info("Fetching Recorder Status for Recorder with ID: " + this.mRecorderID);

        Recorder recorder;
        try {
            recorder = DB.getRecorder("id = '" + this.mRecorderID + "'")[0];
        } catch (IndexOutOfBoundsException e) {
            LOGGER.error("Could not locate Recorder Record in DB for recorder ID - " + this.mRecorderID);
            return;
        }

        Status previousStatus = recorder.getStatus();
        Status currentStatus = null;

        try {
            currentStatus = Recorders.getRecorderStatus(Recorders.getRecorderIP(this.mRecorderID));
        } catch (RuntimeException e) {
            LOGGER.error("Problem retrieving recorder status from API - Invalid IP", e);
        }

        if (currentStatus == null) {
            LOGGER.error("Problem retrieving recorder status from API/Recorder");
            currentStatus = Status.UNKNOWN;
        }

        LOGGER.debug(String.format("Recorder Status is \"%s\"", currentStatus));
        recorder.setStatus(currentStatus);
        recorder.setLastSeen(new Timestamp(System.currentTimeMillis()));
        DB.updateRecorder(recorder);
        LOGGER.info("Finished Updating Recorder Status for Recorder with ID: " + this.mRecorderID);

        try {
            if ((previousStatus == null || previousStatus.okay()) && currentStatus.inAlarm()) {
                LOGGER.warn("Recorder " + mRecorderID + "has entered ALARM state!");
                Hook.fire(Hook.RECORDER_ALARM_ACTIVATE, new Recorder(mRecorderID, currentStatus));
            } else if ((previousStatus == null || previousStatus.inAlarm()) && currentStatus.okay()) {
                LOGGER.info("Recorder" + mRecorderID + " has cleared ALARM state and is now OKAY.");
                Hook.fire(Hook.RECORDER_ALARM_CLEAR, new Recorder(mRecorderID, currentStatus));
            }
        } catch (IOException e) {
            LOGGER.error("Problem firing Alarm Status Update Hook", e);
        }

        try {
            Hook.fire(Hook.RECORDER_STATUS_UPDATE, DB.getRecorder("id='" + mRecorderID + "'")[0]);
        } catch (IOException e) {
            LOGGER.error("Problem firing Recorder Status Update Hook", e);
        }
    }
}
