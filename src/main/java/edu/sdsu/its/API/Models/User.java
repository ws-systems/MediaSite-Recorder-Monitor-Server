package edu.sdsu.its.API.Models;

import com.google.gson.annotations.Expose;
import lombok.*;

import javax.persistence.*;

import static edu.sdsu.its.DB.PASSWORD_ENCRYPTOR;

/**
 * @author Tom Paulus
 * Created on 5/5/17.
 */
@Entity
@Table(name = "users")
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
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
    @Setter
    @Access(AccessType.PROPERTY)
    String password;

    @Expose
    @NonNull
    @Getter
    @Setter
    Boolean notify;

    public String getPassword() {
        return PASSWORD_ENCRYPTOR.encryptPassword(password);
    }

    public String getRawPassword() {
        return password;
    }
}
