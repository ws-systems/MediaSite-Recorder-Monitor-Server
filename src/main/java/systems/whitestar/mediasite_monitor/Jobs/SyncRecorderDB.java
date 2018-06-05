package systems.whitestar.mediasite_monitor.Jobs;

import com.google.gson.Gson;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.quartz.*;
import systems.whitestar.mediasite_monitor.DB;
import systems.whitestar.mediasite_monitor.Hooks.Hook;
import systems.whitestar.mediasite_monitor.Mediasite.Recorders;
import systems.whitestar.mediasite_monitor.Models.AgentJob;
import systems.whitestar.mediasite_monitor.Models.DefaultAgentFilter;
import systems.whitestar.mediasite_monitor.Models.Recorder;

import java.io.IOException;
import java.util.Map;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Sync the recorder DB with the Mediasite API
 *
 * @author Tom Paulus
 * Created on 5/10/17.
 */
@Log4j
@NoArgsConstructor
public class SyncRecorderDB implements AgentJobInterface, Job {
    public static final String JOB_GROUP = "list_update";
    public static final String TRIGGER_NAME = "SyncRecorderListTrigger";
    public static final String JOB_NAME = "SyncRecorderList";

    /**
     * Schedule the Sync Job
     *
     * @param scheduler         {@link Scheduler} Quartz Scheduler Instance
     * @param intervalInMinutes How often the job should run in Minutes
     * @throws SchedulerException Something went wrong scheduling the job
     */
    public static void schedule(Scheduler scheduler, int intervalInMinutes) throws SchedulerException {
        JobDetail job = newJob(SyncRecorderDB.class)
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
        AgentJob job = this.create(null);
        DB.updateAgentJob(job);
    }

    public AgentJob create(Map<String, String> payload) {
        log.info("Starting Recorder Sync Job");

        return AgentJob.builder()
                .job(this.getClass())
                .priority(0)  // Low Priority Job
                .agentFilter(DefaultAgentFilter.class)
                .build();
    }

    public void process(Map<String, String> payload) {
        Recorder[] recorders = new Gson().fromJson(payload.getOrDefault("recorders", "[]"), Recorder[].class);
        log.debug(String.format("Agent retrieved %d recorders from MS API", recorders.length));

        log.debug("Updating Recorder DB");
        for (Recorder recorder : recorders) {
            log.debug(String.format("Inserting/Updating Recorder %s - \"%s\"", recorder.getName(), recorder.toString()));
            try {
                DB.updateRecorder(Recorder.merge(DB.getRecorder("id = '" + recorder.getId() + "'")[0], recorder));
            } catch (IndexOutOfBoundsException e) {
                log.debug("New Recorder - Cannot execute Update, Inserting new record.");
                DB.updateRecorder(recorder);
            }
        }
        log.debug("Updated Recorder DB");

        try {
            Hook.fire(Hook.RECORDER_RECORD_UPDATE, recorders);
        } catch (IOException e) {
            log.error("Problem firing Recorder DB Update Hook", e);
        }

        log.info("Finished Recorder Sync Job");
    }
}
