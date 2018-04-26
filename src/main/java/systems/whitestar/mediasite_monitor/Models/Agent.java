package systems.whitestar.mediasite_monitor.Models;

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
    private String id;

    private String name;

    private boolean authorized;

    @Column(name = "last_seen")
    private Timestamp lastSeen;
}
