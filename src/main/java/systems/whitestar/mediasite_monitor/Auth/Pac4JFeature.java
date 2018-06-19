package systems.whitestar.mediasite_monitor.Auth;

import org.pac4j.jax.rs.features.JaxRsConfigProvider;
import org.pac4j.jax.rs.features.Pac4JSecurityFeature;
import org.pac4j.jax.rs.jersey.features.Pac4JValueFactoryProvider;
import org.pac4j.jax.rs.servlet.features.ServletJaxRsContextFactoryProvider;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

/**
 * @author Tom Paulus
 * Created on 6/18/18.
 */
@Provider
public class Pac4JFeature implements Feature {
    public boolean configure(FeatureContext context) {
        context
                .register(new JaxRsConfigProvider(LoginConfigFactory.getConfig()))
                .register(new Pac4JSecurityFeature())
                .register(new Pac4JValueFactoryProvider.Binder()) // only with Jersey
                .register(new ServletJaxRsContextFactoryProvider());

        return true;
    }
}
