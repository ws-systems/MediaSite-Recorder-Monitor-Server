package systems.whitestar.mediasite_monitor.Models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;

/**
 * @author Tom Paulus
 * Created on 5/5/17.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "PK"),
                @UniqueConstraint(columnNames = "email")
        }
)
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @Getter
    @Column(name = "PK")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int PK;

    @Expose
    @NonNull
    @Getter
    @Setter
    @Column(name = "name")
    String name;

    @Expose
    @NonNull
    @Getter
    @Setter
    String email;

    @Expose
    @Getter
    @Setter
    Boolean notify;

    @Expose
    @Getter
    @Setter
    @ColumnDefault("FALSE")
    Boolean admin;

    @Expose(serialize = false)
    @Getter
    @Setter
    @Column(name = "external_id")
    String externalId;

    public static User merge(User existingUser, final User newUser) {
        if (newUser.getPK() != 0) {
            existingUser.PK = newUser.getPK();
        }
        if (newUser.getEmail() != null && !newUser.getEmail().isEmpty()) {
            existingUser.setEmail(newUser.getEmail());
        }
        if (newUser.getName() != null && !newUser.getName().isEmpty()) {
            existingUser.setName(newUser.getName());
        }
        if (newUser.getNotify() != null) {
            existingUser.setNotify(newUser.getNotify());
        }
        if (newUser.getAdmin() != null) {
            existingUser.setAdmin(newUser.getAdmin());
        }
        if (newUser.getExternalId() != null) {
            existingUser.setExternalId(newUser.getExternalId());
        }

        return existingUser;
    }

    public boolean complete() {
        if (name == null || name.isEmpty()) return false;
        if (email == null || email.isEmpty()) return false;
        return notify != null;

    }

    public String asJson() {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        return gson.toJson(this);
    }
}
