package edu.sdsu.its.Hooks;

import edu.sdsu.its.API.Models.Recorder;
import edu.sdsu.its.API.Models.User;
import edu.sdsu.its.DB;
import edu.sdsu.its.Jobs.SyncRecorderStatus;
import edu.sdsu.its.Mediasite.Recorders;
import edu.sdsu.its.Schedule;
import lombok.extern.log4j.Log4j;
import org.quartz.JobKey;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.SchedulerException;


/**
 * Listen for hooks that require new jobs to be scheduled
 *
 * @author Tom Paulus
 *         Created on 7/24/17.
 */
@SuppressWarnings("unused")
@Log4j
class JobSchedulerHook extends EventHook {
    Object onRecorderRecordUpdate(Recorder[] recorders) {
        if (recorders == null) return false;

        int scheduled = 0;

        for (Recorder recorder : recorders) {
            try {
                if (!Schedule.getScheduler().checkExists(new JobKey(SyncRecorderStatus.TRIGGER_NAME_STEM + "-" + recorder.getId(), SyncRecorderStatus.JOB_GROUP))) {
                    log.info("Creating New Status Sync Job for Recorder ID: " + recorder.getId());
                    new SyncRecorderStatus(recorder.getId())
                            .schedule(Schedule.getScheduler(), Integer.parseInt(DB.getPreference("sync-frequency")));
                    scheduled++;
                }
            } catch (ObjectAlreadyExistsException e) {
                log.debug("Recorder already has sync scheduled");
            } catch (SchedulerException e) {
                log.warn("Problem Adding new Recorders to Sync Scheduler", e);
            }
        }

        return scheduled;
    }

    Object onUserCreate(User user) {
        // Intentionally Blank
        return null;
    }

    Object onUserUpdate(User user) {
        // Intentionally Blank
        return null;
    }

    Object onRecorderStatusUpdate(Recorder recorder) {
        // Intentionally Blank
        return null;
    }

    Object onRecorderAlarmActivate(Recorder recorder) {
        // Intentionally Blank
        return null;
    }

    Object onRecorderAlarmClear(Recorder recorder) {
        // Intentionally Blank
        return null;
    }
}
