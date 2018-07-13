package systems.whitestar.mediasite_monitor.Jobs;

import com.google.gson.Gson;
import lombok.extern.log4j.Log4j;
import org.quartz.*;
import systems.whitestar.mediasite_monitor.DB;
import systems.whitestar.mediasite_monitor.Hooks.Hook;
import systems.whitestar.mediasite_monitor.Models.AgentJob;
import systems.whitestar.mediasite_monitor.Models.DefaultAgentFilter;
import systems.whitestar.mediasite_monitor.Models.RecorderExpectation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @author Tom Paulus
 * Created on 5/9/18.
 */
@Log4j
public class RecorderExpectationCheck implements AgentJobInterface, Job {
    public static final String JOB_GROUP = "expectations";
    public static final String TRIGGER_NAME = "ExpectationCheckTrigger";
    public static final String JOB_NAME = "ExpectationCheck";

    /**
     * Schedule the Sync Job
     *
     * @param scheduler   {@link Scheduler} Quartz Scheduler Instance
     * @param expectation {@link RecorderExpectation} Recorder Status Expectation
     * @throws SchedulerException Something went wrong scheduling the job
     */
    public static void schedule(Scheduler scheduler, RecorderExpectation expectation) throws SchedulerException {
        String nameStem = String.format("-%s-%d", expectation.getRecorder(), expectation.getRecurrenceId());

        JobDetail job = newJob(RecorderExpectationCheck.class)
                .withIdentity(JOB_NAME + nameStem, JOB_GROUP)
                .usingJobData("expectation", new Gson().toJson(expectation))
                .build();

        // Trigger the job to run every day at hour:minute
        Trigger trigger = newTrigger()
                .withIdentity(TRIGGER_NAME + nameStem, JOB_GROUP)
                .withPriority(99)
                .startAt(expectation.getCheckTime())
                .build();

        // Tell quartz to schedule the job using our trigger
        scheduler.scheduleJob(job, trigger);
    }

    public AgentJob create(Map<String, String> payload) {
        log.info("Started Schedule Expectation Check Job");

        return AgentJob.builder()
                .job(this.getClass())
                .priority(100)  // High Priority Job
                .agentFilter(DefaultAgentFilter.class)
                .payload(payload)
                .build();
    }

    public void process(Map<String, String> payload) {
        String checkResult = payload.get("result");
        RecorderExpectation expectation = new Gson().fromJson(payload.get("expectation"), RecorderExpectation.class);

        switch (checkResult.toUpperCase()) {
            case "FAILED":
                log.warn("Expectation check returned result - FAILED");
                try {
                    Hook.fire(Hook.EXPECTATION_CHECK_FAILED, expectation);
                } catch (IOException e) {
                    log.error("Could not fire EXPECTATION_CHECK_FAILED Hook", e);
                }
                break;
            case "OK":
                log.info("Expectation check returned result - OK");
                try {
                    Hook.fire(Hook.EXPECTATION_CHECK_PASSED, expectation);
                } catch (IOException e) {
                    log.error("Could not fire EXPECTATION_CHECK_PASSED Hook", e);
                }
                break;
            case "SKIPPED":
                log.info("Expectation check returned result - SKIPPED");
                break;
            default:
                log.error(String.format("Received unexpected result string - \"%s\"", checkResult));
                break;
        }
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("Starting Schedule Expectation Check Job");
        final String expectation = context.getJobDetail().getJobDataMap().getString("expectation");

        Map<String, String> payload = new HashMap<String, String>() {{
            put("expectation", expectation);
        }};

        AgentJob job = this.create(payload);
        DB.updateAgentJob(job);

        try {
            context.getScheduler().deleteJob(context.getJobDetail().getKey());
        } catch (SchedulerException e) {
            log.warn("Could not delete Job - " + context.getJobDetail().getKey().getName(), e);
        }

        log.info("Finished Schedule Expectation Check Job");
    }
}
