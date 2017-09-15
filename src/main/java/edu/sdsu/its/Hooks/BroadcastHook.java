package edu.sdsu.its.Hooks;


import edu.sdsu.its.API.BroadcastEvent;
import edu.sdsu.its.API.Models.Recorder;
import edu.sdsu.its.API.Models.User;
import lombok.extern.log4j.Log4j;

/**
 * Broadcast Events to Clients subscribed via Server Side Events.
 *
 * @author Tom Paulus
 * Created on 9/15/17.
 */
@SuppressWarnings("unused")
@Log4j
public class BroadcastHook extends EventHook {
    Object onUserCreate(User user) {
        // Intentionally Blank
        return null;
    }

    Object onUserUpdate(User user) {
        // Intentionally Blank
        return null;
    }

    Object onRecorderRecordUpdate(Recorder[] recorders) {
        for (Recorder recorder : recorders) {
            BroadcastEvent.Event event = new BroadcastEvent.Event(Hook.RECORDER_RECORD_UPDATE, recorder);
            log.info("Broadcasting Record Update event for Recorder ID - " + recorder.getId());
            log.debug(event);
            event.broadcast();
        }

        log.info(String.format("Broadcast %d events", recorders.length));
        return true;
    }

    Object onRecorderStatusUpdate(Recorder recorder) {
        BroadcastEvent.Event event = new BroadcastEvent.Event(Hook.RECORDER_STATUS_UPDATE, recorder);
        log.info("Broadcasting Status Update event for Recorder ID - " + recorder.getId());
        log.debug(event);
        event.broadcast();

        return true;
    }

    Object onRecorderAlarmActivate(Recorder recorder) {
        BroadcastEvent.Event event = new BroadcastEvent.Event(Hook.RECORDER_ALARM_ACTIVATE, recorder);
        log.info("Broadcasting Recorder In Alarm Event for Recorder ID - " + recorder.getId());
        log.debug(event);
        event.broadcast();

        return true;
    }

    Object onRecorderAlarmClear(Recorder recorder) {
        BroadcastEvent.Event event = new BroadcastEvent.Event(Hook.RECORDER_ALARM_CLEAR, recorder);
        log.info("Broadcasting Recorder Alarm Clear Event for Recorder ID - " + recorder.getId());
        log.debug(event);
        event.broadcast();

        return true;
    }
}
