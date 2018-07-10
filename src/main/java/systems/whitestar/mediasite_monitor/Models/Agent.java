package systems.whitestar.mediasite_monitor.Models;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * @author Tom Paulus
 * Created on 11/22/17.
 */
@Data
@Entity
@Table(name = "agents")
@AllArgsConstructor
@NoArgsConstructor
public class Agent {
    @Id
    @NonNull
    @Expose
    private String id;

    @Expose
    private String name;

    @Expose
    private Boolean authorized = false;

    @Column(name = "last_seen")
    @Expose(deserialize = false)
    private Timestamp lastSeen;

    public static Agent merge(Agent existingAgent, final Agent newAgent) {
        if (newAgent.getId() != null && !newAgent.getId().isEmpty())
            existingAgent.setId(newAgent.getId());
        if (newAgent.getName() != null && !newAgent.getName().isEmpty())
            existingAgent.setName(newAgent.getName());
        if (newAgent.getAuthorized() != null)
            existingAgent.setAuthorized(newAgent.getAuthorized());

        return existingAgent;
    }

    public boolean isAuthorized() {
        return this.getAuthorized();
    }
}
