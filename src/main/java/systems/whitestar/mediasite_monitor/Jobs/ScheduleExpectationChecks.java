package systems.whitestar.mediasite_monitor.Jobs;

import com.google.gson.Gson;
import lombok.extern.log4j.Log4j;
import org.quartz.*;
import systems.whitestar.mediasite_monitor.DB;
import systems.whitestar.mediasite_monitor.Models.AgentJob;
import systems.whitestar.mediasite_monitor.Models.DefaultAgentFilter;
import systems.whitestar.mediasite_monitor.Models.RecorderExpectation;
import systems.whitestar.mediasite_monitor.Schedule;

import java.util.Map;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @author Tom Paulus
 * Created on 5/9/18.
 */
@Log4j
public class ScheduleExpectationChecks implements AgentJobInterface, Job {
    public static final String JOB_GROUP = "schedule_expectations";
    public static final String TRIGGER_NAME = "ScheduleExpectationChecksTrigger";
    public static final String JOB_NAME = "ScheduleExpectationChecks";

    /**
     * Schedule the Sync Job
     *
     * @param scheduler {@link Scheduler} Quartz Scheduler Instance
     * @param hour      Hour (24-Hour Format) to run the Job
     * @param minute    Minute to run the Job
     * @param fireNow   If the job should be run Now, as well as at the next scheduled time.
     * @throws SchedulerException Something went wrong scheduling the job
     */
    public static void schedule(Scheduler scheduler, int hour, int minute, boolean fireNow) throws SchedulerException {
        JobDetail job = newJob(ScheduleExpectationChecks.class)
                .withIdentity(JOB_NAME, JOB_GROUP)
                .build();

        // Trigger the job to run every day at hour:minute
        Trigger trigger = newTrigger()
                .withIdentity(TRIGGER_NAME, JOB_GROUP)
                .withSchedule(
                        CronScheduleBuilder.dailyAtHourAndMinute(hour, minute)
                )
                .startNow()
                .build();

        // Tell quartz to schedule the job using our trigger
        scheduler.scheduleJob(job, trigger);

        if (fireNow) {
            JobDetail job_instant = newJob(ScheduleExpectationChecks.class)
                    .withIdentity(JOB_NAME + "-INSTANT", JOB_GROUP)
                    .storeDurably()
                    .build();

            scheduler.addJob(job_instant, true);
            scheduler.triggerJob(job_instant.getKey());

            log.info("Scheduled Instant Trigger");
        }
    }

    public AgentJob create(Map<String, String> payload) {
        return AgentJob.builder()
                .job(this.getClass())
                .priority(5) // Medium-Low Priority
                .payload(payload)
                .agentFilter(DefaultAgentFilter.class)
                .build();
    }

    public void process(Map<String, String> payload) {
        RecorderExpectation[] expectations = new Gson().fromJson(payload.get("expectations"), RecorderExpectation[].class);
        for (RecorderExpectation expectation : expectations) {
            try {
                RecorderExpectationCheck.schedule(
                        systems.whitestar.mediasite_monitor.Schedule.getScheduler(),
                        expectation);
            } catch (SchedulerException e) {
                log.error("Problem scheduling Recorder Expectation Check", e);
            }
        }
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("Starting Schedule Expectation Check Scheduler Job");

        AgentJob job = this.create(null);
        DB.updateAgentJob(job);

        log.info("Finished Schedule Expectation Check Scheduler Job");

        if (context.getJobDetail().getKey().getName().endsWith("-INSTANT"))
            try {
                Schedule.getScheduler().deleteJob(context.getJobDetail().getKey());
            } catch (SchedulerException e) {
                log.warn("Could not delete Instant Fire Job", e);
            }
    }
}
