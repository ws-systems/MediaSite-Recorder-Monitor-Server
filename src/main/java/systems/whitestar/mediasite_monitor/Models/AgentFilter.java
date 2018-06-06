package systems.whitestar.mediasite_monitor.Models;

/**
 * @author Tom Paulus
 * Created on 6/3/18.
 */
public interface AgentFilter {
    /**
     * Determine whether an agent is allowed to execute a given job
     * @param agent {@link Agent} Potential Executing Agent
     * @param job {@link AgentJob} Job to be executed
     * @return If the specified agent is allowed to execute the job
     */
    boolean filterJob(Agent agent, AgentJob job);
}
