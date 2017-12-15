package systems.whitestar.mediasite_monitor;

import lombok.Setter;
import lombok.extern.log4j.Log4j;
import systems.whitestar.mediasite_monitor.API.Models.Preference;
import systems.whitestar.mediasite_monitor.API.Models.Recorder;
import systems.whitestar.mediasite_monitor.API.Models.User;

import javax.persistence.*;
import java.util.List;
import java.util.Properties;

/**
 * @author Tom Paulus
 * Created on 5/5/17.
 */
@SuppressWarnings( {"unchecked"})
@Log4j
public class DB {
    @Setter
    private static EntityManagerFactory sessionFactory;

    public static void setup() {
        Properties props = new Properties();
        props.setProperty("javax.persistence.jdbc.url", Secret.getInstance().getSecret("db-url"));
        props.setProperty("javax.persistence.jdbc.user", Secret.getInstance().getSecret("db-user"));
        props.setProperty("javax.persistence.jdbc.password", Secret.getInstance().getSecret("db-password"));

        try {
            sessionFactory = Persistence.createEntityManagerFactory("systems.whitestar.mediasite_monitor.jpa", props);
            log.info("Session Factory is ready to go!");
        } catch (Exception e) {
            log.fatal("Could not start Session Factory", e);

            throw e;
        }
    }

    public static void shutdown() {
        if (sessionFactory != null) {
            log.warn("Closing Session Factory");
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
            log.debug(preference);

            entityManager.getTransaction().commit();
            entityManager.close();

            return preference.getValue();
        } catch (NoResultException e) {
            log.warn("No Setting found with name - " + name);
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
            log.warn("Failed to Update User - Rolling Back");
            log.debug(user.toString());
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
        log.debug(String.format("Found %d users in DB that match restriction \"%s\"", users.size(), restriction));

        entityManager.getTransaction().commit();
        entityManager.close();

        return users.toArray(new User[users.size()]);
    }

    /**
     * Delete a User from the DB
     *
     * @param user {@link User} User to delete
     */
    public static void deleteUser(final User user) {
        log.warn(String.format("Deleting User with Email: %s", user.getEmail()));
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
        log.debug(String.format("Found %d Recorders in DB that match restriction \"%s\"", recorders.size(), restriction));

        entityManager.getTransaction().commit();
        entityManager.close();

        return recorders.toArray(new Recorder[recorders.size()]);
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

    }
}
