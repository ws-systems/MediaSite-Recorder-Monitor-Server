package systems.whitestar.mediasite_monitor.Jobs;

import lombok.extern.log4j.Log4j;
import systems.whitestar.mediasite_monitor.Models.AgentJob;
import systems.whitestar.mediasite_monitor.Models.DefaultAgentFilter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Tom Paulus
 * Created on 6/4/18.
 */
@Log4j
public class TestAgentJobUrgent extends TestAgentJob {
    @Override public AgentJob create(Map<String, String> payload) {
        AgentJob job = super.create(payload);
        job.setPriority(100);

        return job;
    }
}
