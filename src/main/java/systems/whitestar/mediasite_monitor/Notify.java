package systems.whitestar.mediasite_monitor;

import lombok.*;
import lombok.extern.log4j.Log4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * Send notification email to a given recipient or group of recipients
 * TODO Add Unit Tests that check that the Builder and Template Render work as intended
 *
 * @author Tom Paulus
 * Created on 7/11/17.
 */
@Log4j
@Builder
public class Notify {
    protected final HtmlEmail mEmail = new HtmlEmail();

    private Recipient[] recipients;
    private String subject;
    private String message;

    @SuppressWarnings("unused") // Used by @Builder
    public Notify(Recipient[] recipients, String subject, String message) {
        this.recipients = recipients;
        this.subject = subject;
        this.message = message;

        // TODO have these in the Settings Table and allow them to be changed via UI
        @NonNull final String emailHost = DB.getPreference("email.host");
        @NonNull final int emailPort = Integer.parseInt(DB.getPreference("email.port"));
        @NonNull final String emailUser = DB.getPreference("email.username");
        @NonNull final String emailPass = DB.getPreference("email.password");
        @NonNull final String emailFromAdd = DB.getPreference("email.from_email");
        @NonNull final String emailFromName = DB.getPreference("email.from_name");
        @NonNull final boolean emailSSL = Boolean.parseBoolean(DB.getPreference("email.ssl"));


        mEmail.setHostName(emailHost);
        mEmail.setSmtpPort(emailPort);
        mEmail.setAuthenticator(new DefaultAuthenticator(emailUser, emailPass));
        mEmail.setSSLOnConnect(emailSSL);
        try {
            mEmail.setFrom(emailFromAdd, emailFromName);
            for (Recipient recipient : recipients) mEmail.addTo(recipient.getEmail(), recipient.getName());

            mEmail.setSubject(subject);
            mEmail.setHtmlMsg(message);
        } catch (EmailException e) {
            log.error("Problem Making Email", e);
        }
    }

    /**
     * Prepare a Message's HTML Content which is loaded from a jTwig template in the project's resource folder.
     *
     * @param templatePath   {@link String} Path of the Template from within the Resources Folder
     * @param templateValues {@link Map} Variables to fill in the Template
     * @return {@link String} Filled Message
     */
    public static String messageFromTemplate(final String templatePath, Map<String, Object> templateValues) throws IOException {
        JtwigTemplate template = JtwigTemplate.inlineTemplate(
                IOUtils.toString(Notify.class.getClassLoader().getResourceAsStream(templatePath),
                        "utf-8"));
        JtwigModel model = JtwigModel.newModel();
        for (String key : templateValues.keySet()) {
            model.with(key, templateValues.get(key));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        template.render(model, out);

        return out.toString("utf-8");
    }

    public String send() throws EmailException {
        log.info("Sending Notification Email to - " + Arrays.toString(recipients));
        log.info("Subject - " + subject);
        return mEmail.send();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Recipient {
        private String name;
        private String email;

        @Override
        public String toString() {
            return name + '<' + email + '>';
        }
    }
}
