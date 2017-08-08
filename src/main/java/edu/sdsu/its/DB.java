package edu.sdsu.its;

import edu.sdsu.its.API.Models.Preference;
import edu.sdsu.its.API.Models.Recorder;
import edu.sdsu.its.API.Models.User;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.jasypt.util.password.StrongPasswordEncryptor;

import javax.persistence.*;
import java.util.List;
import java.util.Properties;

/**
 * @author Tom Paulus
 * Created on 5/5/17.
 */
@SuppressWarnings( {"unchecked"})
public class DB {
    private static final Logger LOGGER = Logger.getLogger(DB.class);
    public static final StrongPasswordEncryptor PASSWORD_ENCRYPTOR = new StrongPasswordEncryptor();

    @Setter
    private static EntityManagerFactory sessionFactory;

    public static void setup() {
        Properties props = new Properties();
        props.setProperty("javax.persistence.jdbc.url", Vault.getParam("db-url"));
        props.setProperty("javax.persistence.jdbc.user", Vault.getParam("db-user"));
        props.setProperty("javax.persistence.jdbc.password", Vault.getParam("db-password"));

        try {
            sessionFactory = Persistence.createEntityManagerFactory("edu.sdsu.its.jpa", props);
            LOGGER.info("Session Factory is ready to go!");
        } catch (Exception e) {
            LOGGER.fatal("Could not start Session Factory", e);

            throw e;
        }
    }

    public static void shutdown() {
        if (sessionFactory != null) {
            LOGGER.warn("Closing Session Factory");
            sessionFactory.close();
        }
    }

    static EntityManagerFactory getSessionFactory() {
        return sessionFactory;
    }

    public static String getPreference(final String name) {
        try {
            EntityManager entityManager = sessionFactory.createEntityManager();
            entityManager.getTransaction().begin();

            Preference preference = (Preference) entityManager.createQuery("select p from Preference p where p.setting = '" + name + "'").getSingleResult();
            LOGGER.debug(preference);

            entityManager.getTransaction().commit();
            entityManager.close();

            return preference.getValue();
        } catch (NoResultException e) {
            LOGGER.warn("No Setting found with name - " + name);
        }

        return null;
    }

    public static void setPreference(final String name, final String value) {
        final Preference preference = new Preference(name, value);

        EntityManager entityManager = sessionFactory.createEntityManager();
        entityManager.getTransaction().begin();
        entityManager.persist(entityManager.contains(preference) ? preference : entityManager.merge(preference));
        entityManager.getTransaction().commit();
        entityManager.close();
    }

    /**
     * Add a new User to the DB or Update an existing User Record
     *
     * @param user {@link User} User to Create or Update
     * @return {@link User} Updated User, Null if Update Failed
     * @throws PersistenceException Thrown if there is a violation to the Constraints of the DB, like a duplicate Email
     */
    public static User updateUser(final User user) throws PersistenceException {
        User updatedUser = null;

        EntityManager entityManager = sessionFactory.createEntityManager();
        entityManager.getTransaction().begin();
        entityManager.persist(entityManager.contains(user) ? user : entityManager.merge(user));
        List<User> users = entityManager.createQuery("select u from User u where u.email = '" + user.getEmail() + "'").getResultList();

        if (users.size() == 1) {
            updatedUser = users.get(0);
            entityManager.getTransaction().commit();
        } else {
            LOGGER.warn("Failed to Update User - Rolling Back");
            LOGGER.debug(user.toString());
            entityManager.getTransaction().rollback();
        }

        entityManager.close();

        return updatedUser;
    }

    /**
     * Get an Array of Users who match the specified criteria.
     * id is the Internal Identifier
     * Username is the Public Identifier
     *
     * @param restriction {@link String} Restriction on the Search, as a WHERE SQL Statement, the WHERE is already included
     * @return {@link User[]} Array of User Objects
     */
    public static User[] getUser(final String restriction) {
        EntityManager entityManager = sessionFactory.createEntityManager();
        entityManager.getTransaction().begin();

        List<User> users = entityManager.createQuery("select u from User u" + (restriction != null && !restriction.isEmpty() ? " where " + restriction : "")).getResultList();
        LOGGER.debug(String.format("Found %d users in DB that match restriction \"%s\"", users.size(), restriction));

        entityManager.getTransaction().commit();
        entityManager.close();

        return users.toArray(new User[]{});
    }

    /**
     * Verify and retrieve a user based on their email address/password pair.
     *
     * @param email    {@link String} User's Email Address
     * @param password {@link String} User's Password
     * @return {@link User} User, Null if non existent or password incorrect
     */
    public static User loginUser(final String email, final String password) {
        try {
            User user = DB.getUser("email = '" + email + "'")[0];
            String passHash = user.getPassword();

            return password != null && !passHash.isEmpty() && PASSWORD_ENCRYPTOR.checkPassword(password, passHash) ? user : null;
        } catch (IndexOutOfBoundsException e) {
            LOGGER.warn(String.format("No user exists with the Email \"%s\"", email));
        }
        return null;
    }

    public static void deleteUser(final User user) {
        LOGGER.warn(String.format("Deleting User with Email: %s", user.getEmail()));
        EntityManager entityManager = sessionFactory.createEntityManager();
        entityManager.getTransaction().begin();
        entityManager.remove(entityManager.contains(user) ? user : entityManager.merge(user));
        entityManager.getTransaction().commit();
        entityManager.close();

    }

    /**
     * Get an Array of Recorders who match the specified criteria.
     *
     * @param restriction {@link String} Restriction on the Search, as a WHERE SQL Statement, the WHERE is already included
     * @return {@link Recorder[]} Array of Recorders
     */
    public static Recorder[] getRecorder(final String restriction) {
        EntityManager entityManager = sessionFactory.createEntityManager();
        entityManager.getTransaction().begin();

        List<Recorder> recorders = entityManager.createQuery("select r from Recorder r " + (restriction != null && !restriction.isEmpty() ? " where " + restriction : "")).getResultList();
        LOGGER.debug(String.format("Found %d Recorders in DB that match restriction \"%s\"", recorders.size(), restriction));

        entityManager.getTransaction().commit();
        entityManager.close();

        return recorders.toArray(new Recorder[]{});
    }

    /**
     * Create a new Recorder Record, or update an existing record if one with the same ID already exists in the DB.
     *
     * @param recorder {@link Recorder} Recorder to create or update
     */
    public static void updateRecorder(final Recorder recorder) {
        EntityManager entityManager = sessionFactory.createEntityManager();
        entityManager.getTransaction().begin();
        entityManager.persist(entityManager.contains(recorder) ? recorder : entityManager.merge(recorder));
        entityManager.getTransaction().commit();
        entityManager.close();
    }

    private static String sanitize(final String s) {
        return s.replace("'", "");
    }
}
