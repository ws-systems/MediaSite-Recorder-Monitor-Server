package systems.whitestar.mediasite_monitor.Jobs;

import systems.whitestar.mediasite_monitor.Models.AgentJob;

import java.util.Map;

/**
 * @author Tom Paulus
 * Created on 6/3/18.
 */
public interface AgentJobInterface {
    AgentJob create(Map<String, String> payload);
    void process(Map<String, String> payload);
}
