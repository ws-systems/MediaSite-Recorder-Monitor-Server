package edu.sdsu.its.Hooks;

import edu.sdsu.its.API.Models.Recorder;
import edu.sdsu.its.API.Models.User;
import lombok.extern.log4j.Log4j;

/**
 * Example Hook Listener for Unit Tests.
 * The return of the hooks is irrelevant, the tests should check for the
 * Example Method being included in the HookEvent Results
 *
 * @author Tom Paulus
 * Created on 7/26/17.
 */
@SuppressWarnings("unused")
@Log4j
public class ExampleHook extends EventHook {
    Object onUserCreate(User user) {
        log.info("On User Create Hook Fired!");
        return true;
    }

    Object onUserUpdate(User user) {
        log.info("On User Update Hook Fired!");
        return true;
    }

    Object onRecorderRecordUpdate(Recorder[] recorders) {
        log.info("On Recorder Record Update Hook Fired!");
        return true;
    }

    Object onRecorderStatusUpdate(Recorder recorder) {
        log.info("On Recorder Status Update Hook Fired!");
        return true;
    }

    Object onRecorderAlarmActivate(Recorder recorder) {
        log.info("On Recorder Alarm Activate Hook Fired!");
        return true;
    }

    Object onRecorderAlarmClear(Recorder recorder) {
        log.info("On Recorder Alarm Clear Hook Fired!");
        return true;
    }
}
