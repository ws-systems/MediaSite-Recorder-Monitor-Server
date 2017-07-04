package edu.sdsu.its;

import edu.sdsu.its.API.Models.Recorder;
import edu.sdsu.its.API.Models.User;
import edu.sdsu.its.Jobs.SyncRecorderDB;
import edu.sdsu.its.Jobs.SyncRecorderStatus;
import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.SchedulerException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.List;

/**
 * Initialize and Teardown the WebApp and DB
 *
 * @author Tom Paulus
 *         Created on 10/21/2016.
 */
@WebListener
public class Init implements ServletContextListener {
    private static final Logger LOGGER = Logger.getLogger(Init.class);
    private static final int DEFAULT_ID = 550046348;
    private static final String DEFAULT_FIRST_NAME = "Administrator";
    private static final String DEFAULT_LAST_NAME = "User";
    private static final String DEFAULT_EMAIL = "admin";
    private static final String DEFAULT_PASSWORD = "changeme";

    /**
     * Initialize the Webapp with the Default User if no users exist.
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
         try {
            Schedule.getScheduler().clear();
            Schedule.getScheduler().startDelayed(30);
        } catch (SchedulerException e) {
            LOGGER.error("Problem Starting Scheduler", e);
        }
        try {
            String envDisable = System.getenv("MS_SYNC_DISABLE");
            if (Boolean.parseBoolean(Vault.getParam("syncEnable")) && !(envDisable != null && envDisable.toUpperCase().equals("TRUE"))) {
                final int syncInterval = Integer.parseInt(DB.getPreference("sync-frequency"));
                SyncRecorderDB.schedule(Schedule.getScheduler(), syncInterval);

                for (Recorder recorder : DB.getRecorder("")) {
                    LOGGER.info(String.format("Scheduling Sync for Recorder with ID %s - Interval %d", recorder.getId(), syncInterval));
                    new SyncRecorderStatus(recorder.getId()).schedule(Schedule.getScheduler(), syncInterval);
                }

            } else if (envDisable != null && envDisable.toUpperCase().equals("TRUE"))
                LOGGER.warn("Sync has been DISABLED via Environment Variable (MS_SYNC_DISABLE)");
            else
                LOGGER.warn("Sync has been DISABLED - Check Vault Config to Enable");
        } catch (SchedulerException e) {
            LOGGER.error("Problem Scheduling Sync Jobs", e);
        }
    }

    /**
     * Stop All Scheduler Jobs and Shut Down the Scheduler
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
            LOGGER.error("Problem Clearing Job Queue", e);
        }

        try {
            Schedule.getScheduler().clear();
            Schedule.getScheduler().shutdown(false);
        } catch (SchedulerException e) {
            LOGGER.error("Problem shutting down scheduler", e);
        }

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        // Loop through all drivers
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() == cl) {
                // This driver was registered by the webapp's ClassLoader, so deregister it:
                try {
                    LOGGER.info(String.format("Deregistering JDBC driver: %s", driver));
                    DriverManager.deregisterDriver(driver);
                } catch (SQLException ex) {
                    LOGGER.fatal(String.format("Error deregistering JDBC driver: %s", driver), ex);
                }
            } else {
                // driver was not registered by the webapp's ClassLoader and may be in use elsewhere
                LOGGER.info(String.format("Not deregistering JDBC driver %s as it does not belong to this webapp's ClassLoader", driver));
            }
        }
    }
}