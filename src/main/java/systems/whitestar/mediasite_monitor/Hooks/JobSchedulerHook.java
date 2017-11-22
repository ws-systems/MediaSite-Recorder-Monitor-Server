package systems.whitestar.mediasite_monitor.Hooks;

import systems.whitestar.mediasite_monitor.API.Models.Recorder;
import systems.whitestar.mediasite_monitor.API.Models.User;
import systems.whitestar.mediasite_monitor.DB;
import systems.whitestar.mediasite_monitor.Jobs.SyncRecorderStatus;
import systems.whitestar.mediasite_monitor.Schedule;
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
                if (!Schedule.getScheduler().checkExists(new JobKey(SyncRecorderStatus.JOB_NAME_STEM + "-" + recorder.getId(), SyncRecorderStatus.JOB_GROUP))) {
                    log.info("Creating New Status Sync Job for Recorder ID: " + recorder.getId());
                    final String syncFrequency = DB.getPreference("sync_recorder.frequency");
                    if (syncFrequency != null) {
                        new SyncRecorderStatus(recorder.getId())
                                .schedule(Schedule.getScheduler(), Integer.parseInt(syncFrequency));
                        scheduled++;
                    } else {
                        log.error("Sync Frequency is not defined - cannot schedule job");
                    }
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
