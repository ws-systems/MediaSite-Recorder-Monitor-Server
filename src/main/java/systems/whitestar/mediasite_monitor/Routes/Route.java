package systems.whitestar.mediasite_monitor.Routes;

import lombok.extern.log4j.Log4j;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import systems.whitestar.mediasite_monitor.DB;
import systems.whitestar.mediasite_monitor.Meta;
import systems.whitestar.mediasite_monitor.Models.User;
import systems.whitestar.mediasite_monitor.Secret;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static systems.whitestar.mediasite_monitor.Routes.ManageAgents.ONLINE_DELTA_MINS;

/**
 * @author Tom Paulus
 * Created on 7/5/17.
 */
@Log4j class Route {
    private static final String SSO_UPDATE_INFO_URL = Secret.getInstance().getSecret("sso.update-info-URL");

    /**
     * Add Monitor Dashboard Build Information
     *
     * @param request {@link HttpServletRequest} Request
     */
    static void addMeta(HttpServletRequest request) {
        if (Meta.buildInfo != null) {
            request.setAttribute("app_version", Meta.buildInfo.getVersion());
            request.setAttribute("app_build", Meta.buildInfo.getBuild());
            request.setAttribute("app_build_time", Meta.buildInfo.getTime());
        }

        // TODO issue_link setings
//        request.setAttribute("hide_issue_link", Settings.getSetting("issues.hideLink"));
//        request.setAttribute("issue_link", Settings.getSetting("issues.link"));
    }

    /**
     * Set User Attributes in Request
     *
     * @param request {@link HttpServletRequest} Request
     */
    static void setUserData(HttpServletRequest request) {
        if (request.getSession().getAttribute("user") == null) {
            ProfileManager manager = new ProfileManager(new J2EContext(request, null));
            //noinspection unchecked
            Optional<CommonProfile> profile = manager.get(true);
            if (profile.isPresent()) {
                final User[] users = DB.getUser("external_id = '" + profile.get().getId() + "'");
                User user;
                if (users.length == 0) {
                    // Need to Create User
                    log.info(String.format("User with email %s does not yet exist - creating", profile.get().getEmail()));
                    user = User.builder()
                            .name((String) profile.get().getAttribute("name"))
                            .email(profile.get().getEmail())
                            .externalId(profile.get().getId())
                            .notify(false)
                            .build();
                    DB.updateUser(user);
                    request.setAttribute("first_login", true);
                } else {
                    // Incomplete Session
                    // Update User with SSO Provider Information
                    user = User.builder()
                            .name((String) profile.get().getAttribute("name"))
                            .email(profile.get().getEmail())
                            .externalId(profile.get().getId())
                            .build();

                    user = User.merge(users[0], user);
                    DB.updateUser(user);
                    request.setAttribute("first_login", false);
                }

                request.setAttribute("user", user);
            }
        } else {
            User user = (User) request.getSession().getAttribute("user");
            request.setAttribute("user", user);
            request.setAttribute("first_login", false);
        }

        request.setAttribute("sso_update_info_url", SSO_UPDATE_INFO_URL);
    }


    /**
     * Set attributes defined in the NavBar
     *
     * @param request {@link HttpServletRequest} Request
     */
    static void setNavBar(HttpServletRequest request) {
        // Agent Count is number of authorized and online agents
        request.setAttribute("agent_count", DB.getAgent("a.lastSeen > (current_date() + " + ONLINE_DELTA_MINS + "/(24*60)) and a.authorized = true").length);
    }
}
