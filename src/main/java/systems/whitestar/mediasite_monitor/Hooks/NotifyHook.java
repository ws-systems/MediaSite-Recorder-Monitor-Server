package systems.whitestar.mediasite_monitor.Hooks;

import lombok.extern.log4j.Log4j;
import org.apache.commons.mail.EmailException;
import systems.whitestar.mediasite_monitor.DB;
import systems.whitestar.mediasite_monitor.Models.Recorder;
import systems.whitestar.mediasite_monitor.Models.RecorderExpectation;
import systems.whitestar.mediasite_monitor.Models.User;
import systems.whitestar.mediasite_monitor.Notify;
import systems.whitestar.mediasite_monitor.Secret;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;

/**
 * @author Tom Paulus
 * Created on 7/21/17.
 */
@SuppressWarnings("unused")
@Log4j class NotifyHook extends EventHook {
    private static final String ALERT_TEMPLATE_PATH = "email_templates/recorder_in_alarm.twig";
    private static final String EXPECTATION_TEMPLATE_PATH = "email_templates/recorder_expectation_failed.twig";

    Object onRecorderAlarmActivate(Recorder recorder) {
        if (recorder == null) return false;

        User[] usersToNotify = DB.getUser("notify=1");
        Notify.Recipient[] recipients = new Notify.Recipient[usersToNotify.length];
        for (int i = 0; i < usersToNotify.length; i++) {
            User user = usersToNotify[i];
            recipients[i] = new Notify.Recipient(user.getName(),
                    user.getEmail());
        }

        try {
            Notify notification = Notify.builder()
                    .subject("[MS Monitor] Recorder in ALARM")
                    .recipients(recipients)
                    .message(Notify.messageFromTemplate(ALERT_TEMPLATE_PATH, new HashMap<String, Object>() {{
                        put("recorder", DB.getRecorder("id='" + recorder.getId() + "'")[0]);
                        String www_url = Secret.getInstance().getSecret("www_url");
                        if (www_url.endsWith("/")) www_url = www_url.substring(0, www_url.length() - 2);
                        put("url_base", www_url);
                        put("generated_on_date_footer", new Timestamp(new java.util.Date().getTime()).toString());
                    }}))
                    .build();
            String confirmation = notification.send();
            log.debug("Sent Notification Email - " + confirmation);
            return confirmation;
        } catch (IOException e) {
            log.error("Problem making Alert Message", e);
            log.info("Check Template - " + ALERT_TEMPLATE_PATH);
        } catch (EmailException e) {
            log.error("Problem sending Alert Email", e);
        }

        return false;
    }

    Object onRecorderAlarmClear(Recorder recorder) {
        // Intentionally Blank
        return null;
    }

    Object onUserCreate(User user) {
        // Intentionally Blank
        return null;
    }

    Object onUserUpdate(User user) {
        // Intentionally Blank
        return null;
    }

    Object onRecorderRecordUpdate(Recorder[] recorders) {
        // Intentionally Blank
        return null;
    }

    Object onRecorderStatusUpdate(Recorder recorder) {
        // Intentionally Blank
        return null;
    }

    Object onExpectationPass(RecorderExpectation expectation) {
        // Intentionally Blank
        return null;
    }

    Object onExpectationFail(RecorderExpectation expectation) {
        if (expectation == null) return false;

        User[] usersToNotify = DB.getUser("notify=1");
        Notify.Recipient[] recipients = new Notify.Recipient[usersToNotify.length];
        for (int i = 0; i < usersToNotify.length; i++) {
            User user = usersToNotify[i];
            recipients[i] = new Notify.Recipient(user.getName(),
                    user.getEmail());
        }

        try {
            Notify notification = Notify.builder()
                    .subject("[MS Monitor] Recorder Expectation FAILED")
                    .recipients(recipients)
                    .message(Notify.messageFromTemplate(EXPECTATION_TEMPLATE_PATH, new HashMap<String, Object>() {{
                        put("expectation", expectation);
                        put("recorder", DB.getRecorder("id='" + expectation.getRecorder().getId() + "'")[0]);
                        String www_url = Secret.getInstance().getSecret("www_url");
                        if (www_url.endsWith("/")) www_url = www_url.substring(0, www_url.length() - 2);
                        put("url_base", www_url);
                        put("generated_on_date_footer", new Timestamp(new java.util.Date().getTime()).toString());
                    }}))
                    .build();
            String confirmation = notification.send();
            log.debug("Sent Notification Email - " + confirmation);
            return confirmation;
        } catch (IOException e) {
            log.error("Problem making Expectation Alert Message", e);
            log.info("Check Template - " + EXPECTATION_TEMPLATE_PATH);
        } catch (EmailException e) {
            log.error("Problem sending Expectation Alert Email", e);
        }

        return false;
    }
}
