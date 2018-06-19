package systems.whitestar.mediasite_monitor;

import lombok.extern.log4j.Log4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import systems.whitestar.mediasite_monitor.Models.Recorder;
import systems.whitestar.mediasite_monitor.Jobs.*;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.IOException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Initialize and Tear down the WebApp and DB
 *
 * @author Tom Paulus
 * Created on 10/21/2016.
 */
@Log4j
@WebListener
public class Init implements ServletContextListener {
    private static final int CLEANUP_FREQUENCY = 10;

    /**
     * Initialize the WebApp with the Default User if no users exist.
     * <p>
     * Also, Start the DB Sync Job, as well as schedule Recorder Sync Scheduler for the already existent recorders
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Setup Hibernate ORM Connector
        DB.setup();

        // Set Defaults in Preferences table in DB
        try {
            setPreferenceDefaults();
        } catch (IOException e) {
            log.error("Problem Updating Preferences Table via Defaults", e);
        }

        // Setup Sync Scheduler
        startSyncAgents();

        // Schedule Cleanup Jobs
        startCleanupJobs();
    }

    /**
     * Stop All Scheduler Scheduler and Shut Down the Scheduler
     * Deregister DB Driver to prevent memory leaks.
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            List<JobExecutionContext> currentlyExecuting = Schedule.getScheduler().getCurrentlyExecutingJobs();

            for (JobExecutionContext jobExecutionContext : currentlyExecuting) {
                JobKey jobKey = jobExecutionContext.getJobDetail().getKey();
                Schedule.getScheduler().interrupt(jobKey);
                Schedule.getScheduler().deleteJob(jobKey);
            }
        } catch (SchedulerException e) {
            log.error("Problem Clearing Job Queue", e);
        }

        try {
            Schedule.getScheduler().clear();
            Schedule.getScheduler().shutdown(false);
        } catch (SchedulerException e) {
            log.error("Problem shutting down scheduler", e);
        }

        DB.shutdown();

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        // Loop through all drivers
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() == cl) {
                // This driver was registered by the webapp's ClassLoader, so deregister it:
                try {
                    log.info(String.format("Deregistering JDBC driver: %s", driver));
                    DriverManager.deregisterDriver(driver);
                } catch (SQLException ex) {
                    log.fatal(String.format("Error deregistering JDBC driver: %s", driver), ex);
                }
            } else {
                // driver was not registered by the webapp's ClassLoader and may be in use elsewhere
                log.info(String.format("Not deregistering JDBC driver %s as it does not belong to this webapp's ClassLoader", driver));
            }
        }
    }

    private void setPreferenceDefaults() throws IOException {
        log.info("Updating Preferences Table with Default Values if necessary");

        Properties properties = new Properties();
        properties.load(this.getClass().getClassLoader().getResourceAsStream("defaults.properties"));

        Map<String, String> dbPreferences = DB.getPreferences();

        for (Map.Entry<Object, Object> preference : properties.entrySet()) {
            if (!dbPreferences.containsKey(preference.getKey().toString())) {
                log.info("No value in DB for " + String.valueOf(preference.getKey()) + "; Setting to Default Value of " + String.valueOf(preference.getValue()));
                DB.setPreference(String.valueOf(preference.getKey()),
                        String.valueOf(preference.getValue()));
            } else {
                log.debug("Value " + String.valueOf(preference.getKey()) + " is already set in DB. Skipping!");
            }
        }
    }

    private void startSyncAgents() {
        final Scheduler scheduler = Schedule.getScheduler();

        try {
            scheduler.clear();
            scheduler.startDelayed(60);
        } catch (SchedulerException e) {
            log.error("Problem Starting Scheduler", e);
        }
        String envDisable = System.getenv("MS_SYNC_DISABLE");

        try {
            if (!(envDisable != null && envDisable.toUpperCase().equals("TRUE"))) {
                log.info("Scheduling Recorder DB Sync");
                if (!Boolean.parseBoolean(DB.getPreference("sync_db.enable"))) {
                    log.info("Pausing Sync Recorder DB Trigger Group");
                    log.debug("Trigger Group: " + SyncRecorderDB.JOB_GROUP);
                    scheduler.pauseTriggers(GroupMatcher.groupEquals(SyncRecorderDB.JOB_GROUP));
                }

                final String syncDBFreq = DB.getPreference("sync_db.frequency");
                if (syncDBFreq != null) {
                    SyncRecorderDB.schedule(scheduler, Integer.parseInt(syncDBFreq));
                    log.debug("DB Sync Frequency: " + syncDBFreq);
                } else {
                    log.warn("DB Sync Frequency has not been set - aborting");
                    throw new SchedulerException("Trigger Frequency not set");
                }
            } else
                log.warn("DB Sync has been DISABLED via Environment Variable (MS_SYNC_DISABLE)");
        } catch (SchedulerException e) {
            log.error("Problem Scheduling DB Sync Job", e);
        }

        try {
            if (!(envDisable != null && envDisable.toUpperCase().equals("TRUE"))) {
                final String statusSyncFreq = DB.getPreference("sync_recorder.frequency");
                final int syncRate;
                if (statusSyncFreq != null) {
                    syncRate = Integer.parseInt(statusSyncFreq);
                    log.debug("Status Sync Frequency: " + statusSyncFreq);
                } else {
                    log.warn("Status Sync Frequency has not been set - aborting");
                    throw new SchedulerException("Trigger Frequency not set");
                }

                if (!Boolean.parseBoolean(DB.getPreference("sync_recorder.enable"))) {
                    log.info("Pausing Sync Recorder Status Trigger Group");
                    log.debug("Trigger Group: " + SyncRecorderStatus.JOB_GROUP);
                    scheduler.pauseTriggers(GroupMatcher.groupEquals(SyncRecorderStatus.JOB_GROUP));
                }

                for (Recorder recorder : DB.getRecorder("")) {
                    log.info(String.format("Scheduling Sync for Recorder with ID %s ", recorder.getId()));
                    SyncRecorderStatus.schedule(scheduler, syncRate, recorder.getId());
                }
            } else
                log.warn("Recorder Sync has been DISABLED via Environment Variable (MS_SYNC_DISABLE)");
        } catch (SchedulerException e) {
            log.error("Problem Scheduling Recorder Sync Job(s)", e);
        }
    }

    private void startCleanupJobs() {
        try {
            CleanupAgents.schedule(Schedule.getScheduler(), CLEANUP_FREQUENCY);
            CleanupAgentJobs.schedule(Schedule.getScheduler(), CLEANUP_FREQUENCY);
        } catch (SchedulerException e) {
            log.error("Problem scheduling cleanup jobs", e);
        }
    }
}