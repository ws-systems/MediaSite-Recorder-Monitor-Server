package edu.sdsu.its.Jobs;

import edu.sdsu.its.DB;
import edu.sdsu.its.Mediasite.Recorders;
import org.apache.log4j.Logger;
import org.quartz.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import static edu.sdsu.its.DB.getConnection;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @author Tom Paulus
 *         Created on 5/10/17.
 */
public class SyncRecorderDB implements Job {
    private static final Logger LOGGER = Logger.getLogger(SyncRecorderDB.class);

    public SyncRecorderDB() {
    }

    /**
     * Schedule the Sync Job
     *
     * @param scheduler         {@link Scheduler} Quartz Scheduler Instance
     * @param intervalInMinutes How often the job should run in Minutes
     * @throws SchedulerException Something went wrong scheduling the job
     */
    public static void schedule(Scheduler scheduler, int intervalInMinutes) throws SchedulerException {
        JobDetail job = newJob(SyncRecorderDB.class)
                .withIdentity("SyncUserListJob", "group1")
                .build();

        // Trigger the job to run now, and then repeat every X Seconds
        Trigger trigger = newTrigger()
                .withIdentity("SyncTrigger", "group1")
                .withSchedule(simpleSchedule()
                        .withIntervalInMinutes(intervalInMinutes)
                        .repeatForever())
                .build();

        // Tell quartz to schedule the job using our trigger
        scheduler.scheduleJob(job, trigger);
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOGGER.info("Starting Recorder Sync Job");

        Recorders.Recorder[] recorders = Recorders.getRecorders();
        if (recorders == null) {
            LOGGER.fatal("Problem retrieving recorder list from API");
            return;
        }
        LOGGER.debug(String.format("Retrieved %d recorders from MS API", recorders.length));

        Statement statement = null;
        Connection connection = null;

        try {
            connection = getConnection();
            statement = connection.createStatement();

            //language=SQL
            final String incrementStrikeSQL = "UPDATE recorders\n" +
                    "SET `strikes` = `strikes` + 1;";
            LOGGER.debug(String.format("Incrementing Strike Count for Recorders - \"%s\"", incrementStrikeSQL));
            statement.addBatch(incrementStrikeSQL);

            for (Recorders.Recorder recorder : recorders) {
                //language=SQL
                String insertUpdateSQL =
                        "INSERT INTO recorders (id, name, description, serial_number, version, last_version_update_date, physical_address, image_version)\n" +
                                "VALUES\n" +
                                "  ('" + recorder.getId() + "',\n" +
                                "   '" + recorder.getName() + "',\n" +
                                "   '" + recorder.getDescription() + "',\n" +
                                "   '" + recorder.getSerialNumber() + "',\n" +
                                "   '" + recorder.getVersion() + "',\n" +
                                "   '" + recorder.getLastVersionUpdateDate() + "',\n" +
                                "   '" + recorder.getPhysicalAddress() + "',\n" +
                                "   '" + recorder.getImageVersion() + "')\n" +
                                "ON DUPLICATE KEY UPDATE\n" +
                                "  `name`                     = '" + recorder.getName() + "',\n" +
                                "  `description`              = '" + recorder.getDescription() + "',\n" +
                                "  `version`                  = '" + recorder.getVersion() + "',\n" +
                                "  `last_version_update_date` = '" + recorder.getLastVersionUpdateDate() + "',\n" +
                                "  `physical_address`         = '" + recorder.getPhysicalAddress() + "',\n" +
                                "  `image_version`            = '" + recorder.getImageVersion() + "',\n" +
                                "  `last_seen`                = NOW(),\n" +
                                "  `strikes`                  = 0;";
                LOGGER.debug(String.format("Inserting/Updating Recorder %s - \"%s\"", recorder.getName(), insertUpdateSQL));
                statement.addBatch(insertUpdateSQL);
            }

            //language=SQL
            final String maxStrikeCount = DB.getPreference("sync-max-strike");
            final String setOfflineSQL = "UPDATE recorders\n" +
                    "SET online = `strikes` < " + maxStrikeCount + ";";
            LOGGER.debug(String.format("Setting DB record for offline recorders - \"%s\"", setOfflineSQL));
            statement.addBatch(setOfflineSQL);

            LOGGER.info("Executing Sync Batch");
            int[] updateCounts = statement.executeBatch();
            LOGGER.info("Update Counts - " + Arrays.toString(updateCounts));
        } catch (Exception e) {
            LOGGER.warn("Problem Syncing Recorders with DB", e);
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

        LOGGER.info("Finished Recorder Sync Job");
    }
}
