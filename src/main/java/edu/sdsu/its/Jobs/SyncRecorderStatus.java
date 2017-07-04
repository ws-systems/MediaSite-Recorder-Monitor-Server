package edu.sdsu.its.Jobs;

import edu.sdsu.its.API.Models.Status;
import edu.sdsu.its.DB;
import edu.sdsu.its.Mediasite.Recorders;
import org.apache.log4j.Logger;
import org.quartz.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @author Tom Paulus
 *         Created on 5/14/17.
 */
public class SyncRecorderStatus implements Job {
    private static final Logger LOGGER = Logger.getLogger(SyncRecorderStatus.class);
    private String mRecorderID;
    static final String JOB_GROUP = "recorder_status";
    static final String TRIGGER_NAME_STEM = "SyncTrigger";
    static final String JOB_NAME_STEM = "SyncRecorderStatus";

    public SyncRecorderStatus(String RECORDER_ID) {
        this.mRecorderID = RECORDER_ID;
    }

    @SuppressWarnings("unused") public SyncRecorderStatus() {
        // Intentionally Empty
    }

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

        Status status = null;
        try {
            status = Recorders.getRecorderStatus(Recorders.getRecorderIP(this.mRecorderID));
        } catch (RuntimeException e) {
            LOGGER.error("Problem retrieving recorder status from API - Invalid IP", e);
        }

        if (status == null) {
            LOGGER.error("Problem retrieving recorder status from API/Recorder");
            status = Status.UNAVAILABLE;
        }

        LOGGER.debug(String.format("Recorder Status is \"%s\"", status));
        writeStatusToDB(this.mRecorderID, status);
        LOGGER.info("Finished Updating Recorder Status for Recorder with ID: " + this.mRecorderID);
    }

    private static void writeStatusToDB(String recorderId, Status status) {
        Statement statement = null;
        Connection connection = DB.getConnection();

        try {
            statement = connection.createStatement();
            //language=SQL
            final String updateSQL = "UPDATE recorders\n" +
                    "SET status =  '" + status.getStateString() + "'\n" +
                    "WHERE id='" + recorderId + "';";

            LOGGER.info(String.format("Updating Recorder Status - \"%s\"", updateSQL));
            statement.execute(updateSQL);

        } catch (SQLException e) {
            LOGGER.error("Problem Updating Recorder Status - ID: " + recorderId, e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                    connection.close();
                } catch (SQLException e) {
                    LOGGER.warn("Problem Closing Statement", e);
                }
            }
        }
    }
}
