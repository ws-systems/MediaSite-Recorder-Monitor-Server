package systems.whitestar.mediasite_monitor;

import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer;
import org.pac4j.core.config.Config;
import org.pac4j.core.config.ConfigFactory;
import org.pac4j.core.context.session.J2ESessionStore;
import org.pac4j.core.http.DefaultUrlResolver;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;


/**
 * Creates Login Configuration of Pac4J. Using OpenIDConnect protocol to
 * authenticate with Jetbrains Hub, using the discovery URL saved in Vault.
 *
 * @author Tom Paulus
 * Created on 12/1/17.
 */
public class LoginConfigFactory implements ConfigFactory {
    @Override public Config build(Object... parameters) {
        OidcClient oidcClient = new OidcClient();
        final OidcConfiguration configuration = new OidcConfiguration();
        configuration.setDiscoveryURI(Secret.getInstance().getSecret("oidc.discoveryURL"));
        configuration.setClientId(Secret.getInstance().getSecret("oidc.clientId"));
        configuration.setSecret(Secret.getInstance().getSecret("oidc.secret"));
        configuration.setUseNonce(true);
        configuration.setScope("openid email profile phone");

        //configuration.setPreferredJwsAlgorithm(JWSAlgorithm.RS256);
        configuration.addCustomParam("prompt", "consent");

        oidcClient.setConfiguration(configuration);

        Config config = new Config(Secret.getInstance().getSecret("odic.callbackURL"), oidcClient);
        config.setSessionStore(new J2ESessionStore());
        config.getClients().setDefaultClient(oidcClient);
        config.getClients().setUrlResolver(new DefaultUrlResolver());

        config.addAuthorizer("admin", new RequireAnyRoleAuthorizer("ROLE_ADMIN"));
        return config;
    }
}
