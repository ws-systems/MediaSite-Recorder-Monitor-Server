package systems.whitestar.mediasite_monitor.Agent;

import lombok.extern.log4j.Log4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import systems.whitestar.mediasite_monitor.DB;
import systems.whitestar.mediasite_monitor.Jobs.TestAgentJob;
import systems.whitestar.mediasite_monitor.Jobs.TestAgentJobUrgent;
import systems.whitestar.mediasite_monitor.Models.Agent;
import systems.whitestar.mediasite_monitor.Models.AgentJob;

import javax.ws.rs.core.Response;

import static org.junit.Assert.*;

/**
 * @author Tom Paulus
 * Created on 6/4/18.
 */
@Log4j
public class TestAgents {


    @Before
    public void setUp() throws Exception {
        DB.setup();
    }

    @After
    public void tearDown() throws Exception {
        DB.shutdown();
    }

    @Test
    public void getOldAgents() {
        int maxAge = 1;
        Agent[] agents = DB.getAgent("a.lastSeen < (current_date() - " + maxAge + "/24)");

        assertNotNull(agents);
        log.info(String.format("Found %d old agents", agents.length));
    }
}