package edu.sdsu.its.Hooks;

import edu.sdsu.its.API.Models.Recorder;
import edu.sdsu.its.API.Models.User;


/**
 * All abstract methods in this class should be included in the Hook Enum so they can be
 * appropriately fired thought the monitor.
 *
 * @author Tom Paulus
 *         Created on 7/21/17.
 */
@SuppressWarnings( {"WeakerAccess", "unused"})
public abstract class EventHook {
    /**
     * Fired on create of a New User. Useful for sending a welcome email, or other tasks that should be
     * run when the user is initially created.
     *
     * @param user {@link User} Created User
     * @return {@link Object} Hook Response
     */
    abstract Object onUserCreate(User user);

    /**
     * Fired whenever a user is updated. Useful for Audit Logs, or Notification Emails to the affected user.
     *
     * @param user {@link User} Updated User
     * @return {@link Object} Hook Response
     */
    abstract Object onUserUpdate(User user);

    /**
     * Fired on updates to the List of Recorders associated with a Mediasite Server.
     * This is the source of the Recorder Information, including IP Addresses, Recorder Names, and Versions
     *
     * @param recorders {@link Recorder[]} Recorders
     * @return {@link Object} Hook Response
     */
    abstract Object onRecorderRecordUpdate(Recorder[] recorders);

    /**
     * Fired when the status of a Recorder is updated after a query to the Recorder's Onboard API.
     * This hook is fired AFTER the Alarm-Class Hooks to ensure that Alarms can be addressed in a timely manner.
     *
     * @param recorder {@link Recorder} Updated Recorder
     * @return {@link Object} Hook Response
     */
    abstract Object onRecorderStatusUpdate(Recorder recorder);

    /**
     * Fired when a Recorder enters a non-normal status, which affects the recorder performance.
     *
     * @param recorder {@link Recorder} Recorder in Alarm
     * @return {@link Object} Hook Response
     */
    abstract Object onRecorderAlarmActivate(Recorder recorder);

    /**
     * Fired when a Recorder comes back online after being in a bad state.
     *
     * @param recorder {@link Recorder} Recorder formerly in Alarm
     * @return {@link Object} Hook Response
     */
    abstract Object onRecorderAlarmClear(Recorder recorder);
}
