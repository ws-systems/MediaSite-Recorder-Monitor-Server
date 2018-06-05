package systems.whitestar.mediasite_monitor.Jobs;

import systems.whitestar.mediasite_monitor.Models.AgentJob;

import java.util.Map;

/**
 * Interface that Agent Jobs must implement. This interface is for the Server components of the jobs, an interface with
 * the same name should exist on the agent that includes the methods necessary to execute the job.
 *
 * @author Tom Paulus
 * Created on 6/3/18.
 */
@SuppressWarnings("unused")
public interface AgentJobInterface {
    /**
     * Create a new Agent Job
     * @param payload {@link Map} Job Specifications and Parameters
     * @return {@link AgentJob} New Agent Job to be pushed to the DB
     */
    AgentJob create(Map<String, String> payload);

    /**
     * Process the job results as delivered from the agent.
     *
     * @param payload {@link Map} Agent Response Payload
     */
    void process(Map<String, String> payload);
}
