package systems.whitestar.mediasite_monitor.Scheduler;

import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.quartz.*;
import systems.whitestar.mediasite_monitor.DB;
import systems.whitestar.mediasite_monitor.Models.AgentJob;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @author Tom Paulus
 * Created on 5/10/17.
 */
@Log4j
@NoArgsConstructor
public class SyncRecorderDB implements Job {
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
        AgentJob job = new systems.whitestar.mediasite_monitor.Jobs.SyncRecorderDB().create(null);
        DB.updateAgentJob(job);
    }
}
