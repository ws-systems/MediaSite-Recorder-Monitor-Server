package systems.whitestar.mediasite_monitor.Jobs;

import lombok.extern.log4j.Log4j;
import org.quartz.*;
import systems.whitestar.mediasite_monitor.DB;
import systems.whitestar.mediasite_monitor.Models.AgentJob;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Delete old completed agent jobs and recover lost in-progress agent jobs.
 *
 * @author Tom Paulus
 * Created on 6/4/18.
 */
@Log4j
public class CleanupAgentJobs implements Job {
    public static final String JOB_GROUP = "cleanup";
    public static final String TRIGGER_NAME = "CleanupAgentJobsTrigger";
    public static final String JOB_NAME = "CleanupAgentJobs";

    private static final int MAX_COMPLETED_JOB_AGE = 6; // Hours
    private static final int MAX_STALE_JOB_AGE = 30; // Minutes

    /**
     * Schedule the Cleanup Job for Agent Jobs
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
        // Delete old completed jobs
        AgentJob[] completedJobs = DB.getAgentJobs("j.job_updated < (current_date() - " + MAX_COMPLETED_JOB_AGE + "/24) " +
                "and j.status = " + AgentJob.AgentJobStatus.DONE.getStatus());

        for (AgentJob job : completedJobs) {
            log.info(String.format("Removing old completed job - %s (ID: %s)", job.getJob().getName(), job.getId()));
            DB.deleteAgentJob(job);
        }
        log.info(String.format("Removed %d old completed Agent Jobs", completedJobs.length));


        // Reset Status of old in-progress jobs
        AgentJob[] staleJobs = DB.getAgentJobs("j.job_updated < (current_date() - " + MAX_STALE_JOB_AGE + "/(24*60)) " +
                "and (j.status != " + AgentJob.AgentJobStatus.CREATED.getStatus() + " and j.status !=" + AgentJob.AgentJobStatus.DONE.getStatus() + ")");

        for (AgentJob job : staleJobs) {
            log.info(String.format("Resetting Stale Job with ID: %s", job.getId()));
            job.setStatus(AgentJob.AgentJobStatus.CREATED);
            job.setAgent(null);
            DB.updateAgentJob(job);
        }
        log.info(String.format("Cleaned up %d stale Agent Jobs", staleJobs.length));
    }
}
