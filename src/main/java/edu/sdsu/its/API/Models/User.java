package edu.sdsu.its.API.Models;

/**
 * @author Tom Paulus
 *         Created on 5/5/17.
 */
public class User {
    String first_name;
    String last_name;
    String email;
    String password;
    Boolean notify;

    public String getEmail() {
        return email;
    }

    public User(String first_name, String last_name, String email, String password) {
        this.first_name = first_name;
        this.last_name = last_name;
        this.email = email;
        this.password = password;
        this.notify = false;
    }
}
