package edu.sdsu.its.Jobs;

import edu.sdsu.its.API.Models.Recorder;
import edu.sdsu.its.DB;
import edu.sdsu.its.Hooks.Hook;
import edu.sdsu.its.Mediasite.Recorders;
import lombok.NoArgsConstructor;
import org.apache.log4j.Logger;
import org.quartz.*;

import java.io.IOException;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @author Tom Paulus
 * Created on 5/10/17.
 */
@NoArgsConstructor
public class SyncRecorderDB implements Job {
    private static final Logger LOGGER = Logger.getLogger(SyncRecorderDB.class);

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
        LOGGER.info("Starting Recorder Sync Job");

        Recorder[] recorders = Recorders.getRecorders();
        if (recorders == null) {
            LOGGER.fatal("Problem retrieving recorder list from API");
            return;
        }
        LOGGER.debug(String.format("Retrieved %d recorders from MS API", recorders.length));

        LOGGER.debug("Updating Recorder DB");
        for (Recorder recorder : recorders) {
            LOGGER.debug(String.format("Inserting/Updating Recorder %s - \"%s\"", recorder.getName(), recorder.toString()));
            try {
                DB.updateRecorder(Recorder.merge(DB.getRecorder("id = '" + recorder.getId() + "'")[0], recorder));
            } catch (IndexOutOfBoundsException e) {
                LOGGER.debug("New Recorder - Cannot execute Update, Inserting new record.");
                DB.updateRecorder(recorder);
            }
        }
        LOGGER.debug("Updated Recorder DB");


        try {
            Hook.fire(Hook.RECORDER_RECORD_UPDATE, recorders);
        } catch (IOException e) {
            LOGGER.error("Problem firing Recorder DB Update Hook", e);
        }

        LOGGER.info("Finished Recorder Sync Job");
    }
}
