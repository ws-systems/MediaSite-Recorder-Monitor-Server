package edu.sdsu.its.API.Models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import lombok.*;

import javax.persistence.*;

import static edu.sdsu.its.DB.PASSWORD_ENCRYPTOR;

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
    @Column(name = "first_name")
    String firstName;

    @Expose
    @NonNull
    @Getter
    @Setter
    @Column(name = "last_name")
    String lastName;

    @Expose
    @NonNull
    @Getter
    @Setter
    String email;

    @Expose(serialize = false)
    @Getter
    String password;

    @Expose
    @NonNull
    @Getter
    @Setter
    Boolean notify;


    public void setPassword(String password) {
        this.password = PASSWORD_ENCRYPTOR.encryptPassword(password);
    }

    public boolean complete() {
        if (firstName == null || firstName.isEmpty()) return false;
        if (lastName == null || lastName.isEmpty()) return false;
        if (email == null || email.isEmpty()) return false;
        if (password == null || password.isEmpty()) return false;
        return notify != null;

    }

    public static User merge(User existingUser, final User newUser) {
        if (newUser.getPK() != 0 ) {
            existingUser.PK = newUser.getPK();
        }
        if (newUser.getEmail() != null && !newUser.getEmail().isEmpty()) {
            existingUser.setEmail(newUser.getEmail());
        }
        if (newUser.getFirstName() != null && !newUser.getFirstName().isEmpty()) {
            existingUser.setFirstName(newUser.getFirstName());
        }
        if (newUser.getLastName() != null && !newUser.getLastName().isEmpty()) {
            existingUser.setLastName(newUser.getLastName());
        }
        if (newUser.getPassword() != null && !newUser.getPassword().isEmpty()) {
            existingUser.setPassword(newUser.getPassword());
        }
        if (newUser.getNotify() != null) {
            existingUser.setNotify(newUser.getNotify());
        }

        return existingUser;
    }

    public String asJson() {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        return gson.toJson(this);
    }
}
