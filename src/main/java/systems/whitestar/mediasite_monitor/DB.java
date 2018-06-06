package systems.whitestar.mediasite_monitor;

import lombok.Setter;
import lombok.extern.log4j.Log4j;
import systems.whitestar.mediasite_monitor.Models.*;

import javax.persistence.*;
import java.sql.Timestamp;
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

        if (props.getProperty("javax.persistence.jdbc.url").startsWith("jdbc:mysql:")) {
            log.info("Using MySQL DB Driver");
            props.setProperty("javax.persistence.jdbc.driver", "com.mysql.jdbc.Driver");
        } else if (props.getProperty("javax.persistence.jdbc.url").startsWith("jdbc:postgresql:")) {
            log.info("Using PostgreSQL DB Driver");
            props.setProperty("javax.persistence.jdbc.driver", "org.postgresql.Driver");
        } else {
            log.fatal("Unsupported DB Type");
            throw new RuntimeException("Unsupported DB Type");
        }

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

        return users.toArray(new User[0]);
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

        return recorders.toArray(new Recorder[0]);
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

    /**
     * Get an Monitor Agent from the DB with given restrictions
     *
     * @param restriction {@link String} Restriction on the Search, as a WHERE SQL Statement, the WHERE is already included
     * @return {@link Agent[]} Matching Agents
     */
    public static Agent[] getAgent(final String restriction) {
        EntityManager entityManager = sessionFactory.createEntityManager();
        entityManager.getTransaction().begin();

        List<Agent> agents = entityManager.createQuery("select a from Agent a " + (restriction != null && !restriction.isEmpty() ? " where " + restriction : "")).getResultList();
        log.debug(String.format("Found %d Agents in DB that match restriction \"%s\"", agents.size(), restriction));

        entityManager.getTransaction().commit();
        entityManager.close();

        return agents.toArray(new Agent[0]);
    }

    /**
     * Update a Monitor Agent in the DB
     *
     * @param agent {@link Agent} Agent to update
     */
    public static void updateAgent(final Agent agent) {
        EntityManager entityManager = sessionFactory.createEntityManager();
        entityManager.getTransaction().begin();
        entityManager.persist(entityManager.contains(agent) ? agent : entityManager.merge(agent));
        entityManager.getTransaction().commit();
        entityManager.close();
    }

    /**
     * Delete an Agent from the DB. This should ony be done after the
     *
     * @param agent {@link Agent} Agent to delete
     */
    public static void deleteAgent(final Agent agent) {
        log.warn(String.format("Deleting Agent with ID: %s", agent.getId()));
        EntityManager entityManager = sessionFactory.createEntityManager();
        entityManager.getTransaction().begin();
        entityManager.remove(entityManager.contains(agent) ? agent : entityManager.merge(agent));
        entityManager.getTransaction().commit();
        entityManager.close();
    }

    public static AgentJob getNewAgentJob(Agent agent) {
        EntityManager entityManager = sessionFactory.createEntityManager();
        entityManager.getTransaction().begin();

        List<AgentJob> jobs = entityManager.createQuery("select j from AgentJob j WHERE j.status = " + AgentJob.AgentJobStatus.CREATED.getStatus() + " order by j.priority desc").getResultList();
        entityManager.getTransaction().commit();
        entityManager.close();

        for (AgentJob job : jobs) {
            if (job.filter(agent)) return job;
        }

        return null;
    }

    public static AgentJob getAgentJob(final String jobID) {
        EntityManager entityManager = sessionFactory.createEntityManager();
        entityManager.getTransaction().begin();

        AgentJob job = (AgentJob) entityManager.createQuery("select j from AgentJob j WHERE j.id = '" + jobID + "'").getSingleResult();
        entityManager.getTransaction().commit();
        entityManager.close();

        return job;
    }

    public static AgentJob[] getAgentJobs(final String restriction) {
        EntityManager entityManager = sessionFactory.createEntityManager();
        entityManager.getTransaction().begin();

        List<AgentJob> jobs = entityManager.createQuery("select j from AgentJob j " + (restriction != null && !restriction.isEmpty() ? " where " + restriction : "")).getResultList();
        log.debug(String.format("Found %d AgentJobs in DB that match restriction \"%s\"", jobs.size(), restriction));

        entityManager.getTransaction().commit();
        entityManager.close();

        return jobs.toArray(new AgentJob[0]);
    }

    public static long getAgentJobCount() {
        EntityManager entityManager = sessionFactory.createEntityManager();
        entityManager.getTransaction().begin();

        long jobCount = ((long) entityManager.createQuery("select count(j.id) from AgentJob j").getSingleResult());
        log.debug(String.format("Found %d AgentJobs in DB",jobCount));

        entityManager.getTransaction().commit();
        entityManager.close();

        return jobCount;
    }

    public static void updateAgentJob(final AgentJob job) {
        // Set job updated time to NOW
        job.setUpdated(new Timestamp(System.currentTimeMillis()));

        EntityManager entityManager = sessionFactory.createEntityManager();
        entityManager.getTransaction().begin();
        entityManager.persist(entityManager.contains(job) ? job : entityManager.merge(job));
        entityManager.getTransaction().commit();
        entityManager.close();
    }

    public static void deleteAgentJob(final AgentJob job){
        log.warn(String.format("Deleting Job with ID: %s", job.getId()));
        EntityManager entityManager = sessionFactory.createEntityManager();
        entityManager.getTransaction().begin();
        entityManager.remove(entityManager.contains(job) ? job : entityManager.merge(job));
        entityManager.getTransaction().commit();
        entityManager.close();
    }

    public static void clearAgentJobQueue() {
        EntityManager entityManager = sessionFactory.createEntityManager();
        entityManager.getTransaction().begin();
        List<AgentJob> jobs = entityManager.createQuery("select j from AgentJob j").getResultList();
        for (AgentJob job : jobs) {
            entityManager.remove(job);
        }

        entityManager.getTransaction().commit();
        entityManager.close();
    }
}
