package systems.whitestar.mediasite_monitor.Jobs;

import lombok.extern.log4j.Log4j;
import org.quartz.*;
import systems.whitestar.mediasite_monitor.DB;
import systems.whitestar.mediasite_monitor.Models.Agent;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Remove old unauthorized agents from the DB.
 *
 * @author Tom Paulus
 * Created on 6/4/18.
 */
@Log4j
public class CleanupAgents implements Job {
    public static final String JOB_GROUP = "cleanup";
    public static final String TRIGGER_NAME = "CleanupAgentsTrigger";
    public static final String JOB_NAME = "CleanupAgents";

    // Max number of hours an Unauthorized Agent can be offline before being deleted from the DB
    public static final int MAX_AGE = 12;

    /**
     * Schedule the Cleanup Job for Agents
     *
     * @param scheduler         {@link Scheduler} Quartz Scheduler Instance
     * @param intervalInMinutes How often the job should run in Minutes
     * @throws SchedulerException Something went wrong scheduling the job
     */
    public static void schedule(Scheduler scheduler, int intervalInMinutes) throws SchedulerException {
        JobDetail job = newJob(CleanupAgents.class)
                .withIdentity(JOB_NAME, JOB_GROUP)
                .build();

        // Trigger the job to run now, and then repeat every X Minutes
        Trigger trigger = newTrigger()
                .withIdentity(TRIGGER_NAME, JOB_GROUP)
                .withSchedule(simpleSchedule()
                        .withIntervalInMinutes(intervalInMinutes)
                        .repeatForever())
                .startNow()
                .build();

        // Tell quartz to schedule the job using our trigger
        scheduler.scheduleJob(job, trigger);
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        // Get all agents that have not checked-in within the past MAX_AGE hours and are unauthorized
        Agent[] agents = DB.getAgent("a.lastSeen < (current_date() - " + MAX_AGE + "/24) and a.authorized = false");

        for (Agent agent : agents) {
            log.info(String.format("Removing old unauthorized agent - %s (ID: %s)", agent.getName(), agent.getId()));
            DB.deleteAgent(agent);
        }

        log.info(String.format("Removed %d old agents", agents.length));
    }
}
