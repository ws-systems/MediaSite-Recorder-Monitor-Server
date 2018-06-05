package systems.whitestar.mediasite_monitor.Hooks;

import systems.whitestar.mediasite_monitor.Models.Recorder;
import systems.whitestar.mediasite_monitor.Models.User;
import systems.whitestar.mediasite_monitor.DB;
import systems.whitestar.mediasite_monitor.Notify;
import lombok.extern.log4j.Log4j;
import org.apache.commons.mail.EmailException;
import systems.whitestar.mediasite_monitor.Secret;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;

/**
 * @author Tom Paulus
 *         Created on 7/21/17.
 */
@SuppressWarnings("unused")
@Log4j
class NotifyHook extends EventHook {
    private static final String ALERT_TEMPLATE_PATH = "email_templates/recorder_in_alarm.twig";

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
                        if (www_url.endsWith("/")) www_url = www_url.substring(0, www_url.length()-2);
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

    @Override Object onRecorderAlarmClear(Recorder recorder) {
        // Intentionally Blank
        return null;
    }

    @Override Object onUserCreate(User user) {
        // Intentionally Blank
        return null;
    }

    @Override Object onUserUpdate(User user) {
        // Intentionally Blank
        return null;
    }

    @Override Object onRecorderRecordUpdate(Recorder[] recorders) {
        // Intentionally Blank
        return null;
    }

    @Override Object onRecorderStatusUpdate(Recorder recorder) {
        // Intentionally Blank
        return null;
    }
}
