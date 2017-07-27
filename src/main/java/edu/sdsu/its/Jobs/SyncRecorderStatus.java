package edu.sdsu.its.Jobs;

import edu.sdsu.its.API.Models.Recorder;
import edu.sdsu.its.API.Models.Status;
import edu.sdsu.its.API.Models.User;
import edu.sdsu.its.DB;
import edu.sdsu.its.Hooks.Hook;
import edu.sdsu.its.Mediasite.Recorders;
import edu.sdsu.its.Notify;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.mail.EmailException;
import org.apache.log4j.Logger;
import org.quartz.*;

import java.io.IOException;
import java.sql.*;
import java.util.HashMap;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @author Tom Paulus
 *         Created on 5/14/17.
 */
@NoArgsConstructor
@AllArgsConstructor
public class SyncRecorderStatus implements Job {
    private static final Logger LOGGER = Logger.getLogger(SyncRecorderStatus.class);

    public static final String JOB_GROUP = "recorder_status";
    public static final String TRIGGER_NAME_STEM = "SyncTrigger";
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

        Status previousStatus = getPreviousStatus(mRecorderID);

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
        writeStatusToDB(this.mRecorderID, currentStatus);
        LOGGER.info("Finished Updating Recorder Status for Recorder with ID: " + this.mRecorderID);

        try {
            if (previousStatus.okay() && currentStatus.inAlarm()) {
                LOGGER.warn("Recorder " + mRecorderID + "has entered ALARM state!");
                Hook.fire(Hook.RECORDER_ALARM_ACTIVATE, new Recorder(mRecorderID, currentStatus));
            } else if (previousStatus.inAlarm() && currentStatus.okay()) {
                LOGGER.info("Recorder" + mRecorderID + " has cleared ALARM state and is now OKAY.");
                Hook.fire(Hook.RECORDER_ALARM_CLEAR, new Recorder(mRecorderID, currentStatus));
            }
        } catch (IOException e) {
            LOGGER.error("Problem firing Alarm Status Update Hook", e);
        }

        try {
            Hook.fire(Hook.RECORDER_STATUS_UPDATE, DB.getRecorder("id='" + mRecorderID + "'"));
        } catch (IOException e) {
            LOGGER.error("Problem firing Recorder Status Update Hook", e);
        }
    }

    private static Status getPreviousStatus(final String recorderId) {
        Connection connection = DB.getConnection();
        Statement statement = null;
        Status status = null;

        try {
            statement = connection.createStatement();
            final String sql = "SELECT `status` FROM recorders WHERE id='" + recorderId + "' LIMIT 1;";
            LOGGER.info(String.format("Executing SQL Query - \"%s\"", sql));
            ResultSet resultSet = statement.executeQuery(sql);

            if (resultSet.next()) {
                status = Status.getByCode(resultSet.getInt(1));
                if (status == null) {
                    LOGGER.error("Failed to get Recorder Status");
                    status = Status.UNAVAILABLE;
                }

                LOGGER.debug(String.format("Recorder Status for Recorder %s is %s (%d)", recorderId, status.getStateString(), status.getStateCode()));
            }

            resultSet.close();
        } catch (SQLException e) {
            LOGGER.error("Problem querying DB for Recorders", e);
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

        return status;
    }

    private static void writeStatusToDB(final String recorderId, final Status status) {
        Statement statement = null;
        Connection connection = DB.getConnection();

        try {
            statement = connection.createStatement();
            //language=SQL
            String updateSQL = "UPDATE recorders SET status =  " + status.getStateCode() + ",";
            if (status != Status.UNKNOWN) updateSQL += "last_seen = NOW() ";
            updateSQL += "WHERE id='" + recorderId + "';";

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
