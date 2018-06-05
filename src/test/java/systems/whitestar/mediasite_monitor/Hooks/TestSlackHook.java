package systems.whitestar.mediasite_monitor.Hooks;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import systems.whitestar.mediasite_monitor.DB;

import static org.junit.Assume.assumeTrue;
import static systems.whitestar.mediasite_monitor.Hooks.SlackHook.postSlackWebhook;

/**
 * @author Tom Paulus
 * Created on 5/5/18.
 */
public class TestSlackHook {
    private static final String TEST_TEMPLATE_PATH = "slack_templates/test_message.twig";

    @Before
    public void setUp() throws Exception {
        DB.setup();
        assumeTrue(Boolean.parseBoolean(DB.getPreference("slack.enable")));
    }

    @Test
    public void testPostSlackWebhook() {
        postSlackWebhook(TEST_TEMPLATE_PATH, null);
    }

    @After
    public void tearDown() throws Exception {
        DB.shutdown();
    }
}