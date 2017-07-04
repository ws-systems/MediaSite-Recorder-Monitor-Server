package edu.sdsu.its.API.Models;

import com.google.gson.annotations.Expose;
import edu.sdsu.its.DB;

/**
 * @author Tom Paulus
 *         Created on 5/5/17.
 */
public class User {
    @Expose
    String first_name;
    @Expose
    String last_name;
    @Expose
    String email;
    @Expose(serialize = false)
    String password;
    @Expose
    Boolean notify;

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return first_name;
    }

    public String getLastName() {
        return last_name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getNotify() {
        return notify;
    }

    public User(String first_name, String last_name, String email, String password) {
        this.first_name = first_name;
        this.last_name = last_name;
        this.email = email;
        this.password = password;
        this.notify = false;
    }

    public User(String first_name, String last_name, String email, Boolean notify) {
        this.first_name = first_name;
        this.last_name = last_name;
        this.email = email;
        this.notify = notify;
    }

}
