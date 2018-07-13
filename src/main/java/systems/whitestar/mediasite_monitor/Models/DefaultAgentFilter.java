package systems.whitestar.mediasite_monitor.Models;

/**
 * @author Tom Paulus
 * Created on 6/3/18.
 */
public class DefaultAgentFilter implements AgentFilter {
    public boolean filterJob(Agent agent, AgentJob job) {
        return agent.isAuthorized();
    }
}
