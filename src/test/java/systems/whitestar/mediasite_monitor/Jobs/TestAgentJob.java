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
public class TestAgentJob implements AgentJobInterface {
    public static boolean jobExecuted = false;

    @Override public AgentJob create(Map<String, String> payload) {
        return AgentJob.builder()
                .job(this.getClass())
                .agentFilter(DefaultAgentFilter.class)
                .payload(new HashMap<>())
                .priority(0)
                .build();
    }

    @Override public void process(Map<String, String> payload) {
        log.info("Test Job Competed");
        jobExecuted = true;
    }
}
