package systems.whitestar.mediasite_monitor.Auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import lombok.extern.log4j.Log4j;
import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer;
import org.pac4j.core.config.Config;
import org.pac4j.core.config.ConfigFactory;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.http.DefaultUrlResolver;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.jax.rs.servlet.pac4j.ServletSessionStore;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import systems.whitestar.mediasite_monitor.Secret;

import java.util.UUID;


/**
 * Creates Login Configuration of Pac4J. Using OpenIDConnect protocol to
 * authenticate with Jetbrains Hub, using the discovery URL saved in Vault.
 *
 * @author Tom Paulus
 * Created on 12/1/17.
 */
@Log4j
public class LoginConfigFactory implements ConfigFactory {
    private static final String SUPER_USER_USERNAME = "superuser";
    private static final String SUPER_USER_PASSWORD = generateSuperUserPassword();

    private static Config config = null;

    private static String generateSuperUserPassword() {
        // TODO Save and record path
        final String password = UUID.randomUUID().toString();
        log.warn("Super User Password is: " + password);
        return password;
    }

    @Override public Config build(Object... parameters) {
        OidcClient oidcClient = new OidcClient();
        final OidcConfiguration configuration = new OidcConfiguration();
        configuration.setDiscoveryURI(Secret.getInstance().getSecret("oidc.discoveryURL"));
        configuration.setClientId(Secret.getInstance().getSecret("oidc.clientId"));
        configuration.setSecret(Secret.getInstance().getSecret("oidc.secret"));
        configuration.setClientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        configuration.setUseNonce(true);
        configuration.setScope("openid email profile phone");

        configuration.setPreferredJwsAlgorithm(JWSAlgorithm.RS256);
        configuration.addCustomParam("prompt", "consent");

        oidcClient.setConfiguration(configuration);

        final FormClient formClient = new FormClient("/superuser-login", new Authenticator() {
            @SuppressWarnings("RedundantThrows")
            @Override
            public void validate(Credentials creds, final WebContext context) throws HttpAction, CredentialsException {
                UsernamePasswordCredentials credentials = ((UsernamePasswordCredentials) creds);
                if (credentials == null) {
                    throwsException("No credential");
                }
                String username = credentials.getUsername();
                String password = credentials.getPassword();
                if (CommonHelper.isBlank(username)) {
                    throwsException("Username cannot be blank");
                }
                if (CommonHelper.isBlank(password)) {
                    throwsException("Password cannot be blank");
                }
                if (CommonHelper.areNotEquals(username, SUPER_USER_USERNAME)) {
                    throwsException("Username : '" + username + "' not correct");
                }
                if (CommonHelper.areNotEquals(password, SUPER_USER_PASSWORD)) {
                    throwsException("Password is invalid");
                }
                final CommonProfile profile = new CommonProfile();
                profile.setId(username);
                profile.addAttribute(Pac4jConstants.USERNAME, username);
                credentials.setUserProfile(profile);
            }

            void throwsException(final String message) throws CredentialsException {
                throw new CredentialsException(message);
            }
        });


        Config config = new Config(Secret.getInstance().getSecret("odic.callbackURL"), oidcClient, formClient);
        config.setSessionStore(new ServletSessionStore());
        config.getClients().setDefaultClient(oidcClient);
        config.getClients().setUrlResolver(new DefaultUrlResolver());

        config.addAuthorizer("admin", new RequireAnyRoleAuthorizer("ROLE_ADMIN"));
        return config;
    }

    public static Config getConfig() {
        if (config == null) config = new LoginConfigFactory().build();
        return config;
    }
}
