package systems.whitestar.mediasite_monitor.API;

import org.pac4j.core.context.J2EContext;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Access Session Details, like the Pac4J Common User Profile
 *
 * @author Tom Paulus
 * Created on 12/13/17.
 */
class Session {
    static CommonProfile getSessionProfile(HttpServletRequest request) {
        ProfileManager manager = new ProfileManager(new J2EContext(request, null));
        //noinspection unchecked
        Optional<CommonProfile> profile = manager.get(true);
        return profile.orElse(null);
    }
}
