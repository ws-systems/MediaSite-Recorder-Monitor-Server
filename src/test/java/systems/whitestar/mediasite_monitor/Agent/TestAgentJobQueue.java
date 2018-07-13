package systems.whitestar.mediasite_monitor.Agent;

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
public class TestAgentJobQueue {
    private Agent testAgent;
    private AgentJob urgentJob;
    private AgentJob regularJob;


    @Before
    public void setUp() throws Exception {
        DB.setup();
        DB.clearAgentJobQueue();

        regularJob = new TestAgentJob().create(null);
        urgentJob = new TestAgentJobUrgent().create(null);

        DB.updateAgentJob(regularJob);
        DB.updateAgentJob(urgentJob);

        testAgent = new Agent();
        testAgent.setAuthorized(true);
    }

    @After
    public void tearDown() throws Exception {
        DB.clearAgentJobQueue();
        DB.shutdown();
    }

    @Test
    public void pop() {
        AgentJob job = DB.getNewAgentJob(testAgent);
        assertNotNull(job);
        assertEquals("Job priority not upheld", urgentJob, job);
        job.setStatus(AgentJob.AgentJobStatus.RECEIVED);
        DB.updateAgentJob(job);
    }

    @Test
    public void testFilter() {
        Agent badAgent = new Agent();
        badAgent.setAuthorized(false);

        assertFalse("Unauthorized Agent given a job", regularJob.filter(badAgent));
        assertTrue(regularJob.filter(testAgent));
    }

    @Test
    public void getJobByID() {
        assertEquals(regularJob, DB.getAgentJob(regularJob.getId()));
    }

    @Test
    public void processJob() {
        Response response = new AgentJobQueue().push(regularJob.getId(), "{}");

        assertNotNull(response);
        assertEquals(2, response.getStatus() / 100); // 200 Class Response Code

        assertTrue(TestAgentJob.jobExecuted);
    }
}