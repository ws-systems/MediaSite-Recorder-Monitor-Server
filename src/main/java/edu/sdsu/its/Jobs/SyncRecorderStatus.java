package edu.sdsu.its.Jobs;

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
    private static final Logger LOGGER = Logger.getLogger(SyncRecorderDB.class);
    private final String RECORDER_ID;

    public SyncRecorderStatus(String RECORDER_ID) {
        this.RECORDER_ID = RECORDER_ID;
    }

    /**
     * Schedule the Sync Job
     *
     * @param scheduler         {@link Scheduler} Quartz Scheduler Instance
     * @param intervalInMinutes How often the job should run in Minutes
     * @throws SchedulerException Something went wrong scheduling the job
     */
    public void schedule(Scheduler scheduler, int intervalInMinutes) throws SchedulerException {
        JobDetail job = newJob(SyncRecorderDB.class)
                .withIdentity("SyncRecorderStatus-" + RECORDER_ID, "recorder_status")
                .build();

        // Trigger the job to run now, and then repeat every X Seconds
        Trigger trigger = newTrigger()
                .withIdentity("SyncTrigger", "recorder_status")
                .withSchedule(simpleSchedule()
                        .withIntervalInMinutes(intervalInMinutes)
                        .repeatForever())
                .build();

        // Tell quartz to schedule the job using our trigger
        scheduler.scheduleJob(job, trigger);
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOGGER.info("Fetching Recorder Status for Recorder with ID: " + this.RECORDER_ID);

        String status = Recorders.getRecorderStatus(this.RECORDER_ID);
        if (status == null || status.isEmpty()) {
            LOGGER.fatal("Problem retrieving recorder status from API");
            return;
        }

        LOGGER.debug(String.format("Recorder Status is \"%s\"", status));
        writeStatusToDB(this.RECORDER_ID, status);
        LOGGER.info("Finished Updating Recorder Status for Recorder with ID: " + this.RECORDER_ID);
    }

    private static void writeStatusToDB(String recorderId, String status) {
        Statement statement = null;
        Connection connection = DB.getConnection();

        try {
            statement = connection.createStatement();
            //language=SQL
            final String updateSQL = "UPDATE recorders\n" +
                    "SET status =  '" + status + "'\n" +
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
