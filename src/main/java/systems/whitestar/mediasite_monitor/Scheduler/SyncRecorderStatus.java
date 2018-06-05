package systems.whitestar.mediasite_monitor.Scheduler;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.quartz.*;
import systems.whitestar.mediasite_monitor.Models.Agent;
import systems.whitestar.mediasite_monitor.Models.AgentJob;
import systems.whitestar.mediasite_monitor.Models.Recorder;
import systems.whitestar.mediasite_monitor.Models.Status;
import systems.whitestar.mediasite_monitor.DB;
import systems.whitestar.mediasite_monitor.Hooks.Hook;
import systems.whitestar.mediasite_monitor.Mediasite.Recorders;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @author Tom Paulus
 * Created on 5/14/17.
 */
@Log4j
@NoArgsConstructor
@AllArgsConstructor
public class SyncRecorderStatus implements Job {
    public static final String JOB_GROUP = "recorder_status";
    public static final String TRIGGER_NAME_STEM = "SyncRecorderStatusTrigger";
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
        log.debug("Scheduled Sync for Recorder with ID - " + this.mRecorderID);
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        final String recorderID = context.getJobDetail().getDescription();
        log.info("Fetching Recorder Status for Recorder with ID: " + recorderID);

        Map<String, String> payload = new HashMap<String, String>() {{
            put("recorderID", recorderID);
        }};

        AgentJob job = new systems.whitestar.mediasite_monitor.Jobs.SyncRecorderStatus().create(payload);
        DB.updateAgentJob(job);
    }
}
