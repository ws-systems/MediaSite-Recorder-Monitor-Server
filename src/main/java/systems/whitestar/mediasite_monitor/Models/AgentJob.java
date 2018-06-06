package systems.whitestar.mediasite_monitor.Models;

import lombok.*;
import lombok.extern.log4j.Log4j;

import javax.persistence.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Tom Paulus
 * Created on 6/3/18.
 */
@Log4j
@Data
@Entity
@Table(name = "agent_jobs")
@AllArgsConstructor
@NoArgsConstructor
public class AgentJob {
    private Class job;

    @ElementCollection(fetch = FetchType.EAGER)
    private Map<String, String> payload;

    private AgentJobStatus status;

    @Id
    private String id;

    private Class agentFilter;

    @ManyToOne
    private Agent agent;

    @Column(name = "job_created")
    private Timestamp created;

    @Column(name = "job_updated")
    private Timestamp updated;

    private int priority;


    @Builder
    public AgentJob(Class job, Map<String, String> payload, Class agentFilter, int priority) {
        this.job = job;
        this.payload = payload;
        this.status = AgentJobStatus.CREATED;
        this.id = UUID.randomUUID().toString();
        this.agentFilter = agentFilter;
        this.priority = priority;
        this.created = new Timestamp(System.currentTimeMillis());
        this.updated = new Timestamp(System.currentTimeMillis());
    }

    public boolean filter(Agent agent) {
        try {
            //noinspection unchecked
            Method filterMethod = agentFilter.getMethod("filterJob", Agent.class, AgentJob.class); // Based off of the AgentFilter Interface
            return (boolean) filterMethod.invoke(agentFilter.newInstance(), agent, this);
        } catch (NoSuchMethodException e) {
            log.error("Class does not correctly implement the interface as it is missing the 'filterJob' method", e);
            throw new RuntimeException(e);
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("Cannot instantiate new instance of job class", e);
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            log.error("Could not process job - method threw exception", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentJob job1 = (AgentJob) o;
        return getPriority() == job1.getPriority() &&
                Objects.equals(getJob(), job1.getJob()) &&
                Objects.equals(getPayload(), job1.getPayload()) &&
                getStatus() == job1.getStatus() &&
                Objects.equals(getId(), job1.getId()) &&
                Objects.equals(getAgentFilter(), job1.getAgentFilter()) &&
                Objects.equals(getAgent(), job1.getAgent());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getJob(), getPayload(), getStatus(), getId(), getAgentFilter(), getAgent(), getPriority());
    }

    public enum AgentJobStatus {
        CREATED(0),
        RECEIVED(1),
        EXECUTED(2),
        DONE(3);

        @Getter
        private int status;

        AgentJobStatus(int status) {
            this.status = status;
        }
    }
}
