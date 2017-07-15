package edu.sdsu.its;

import edu.sdsu.its.API.Models.Recorder;
import edu.sdsu.its.API.Models.Status;
import edu.sdsu.its.API.Models.User;
import org.apache.log4j.Logger;
import org.jasypt.util.password.StrongPasswordEncryptor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tom Paulus
 *         Created on 5/5/17.
 */
public class DB {
    private static final Logger LOGGER = Logger.getLogger(DB.class);
    private static final StrongPasswordEncryptor PASSWORD_ENCRYPTOR = new StrongPasswordEncryptor();


    /**
     * Create and return a new DB Connection
     * Don't forget to close the connection!
     *
     * @return {@link Connection} DB Connection
     */
    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            final String db_url = Vault.getParam("db-url");
            final String db_user = Vault.getParam("db-user");
            final String db_pass = Vault.getParam("db-password");

            if (db_url != null && db_user != null && db_pass != null) {
                conn = DriverManager.getConnection(
                        db_url,
                        db_user,
                        db_pass);
            } else {
                LOGGER.warn("Not all DB Credentials retrieved from Vault");
            }
        } catch (Exception e) {
            LOGGER.fatal("Problem Initializing DB Connection", e);
        }

        return conn;
    }

    private static void executeStatement(final String sql) {
        new Thread() {
            @Override
            public void run() {
                Statement statement = null;
                Connection connection = getConnection();

                try {
                    statement = connection.createStatement();
                    LOGGER.info(String.format("Executing SQL Statement - \"%s\"", sql));
                    statement.execute(sql);

                } catch (SQLException e) {
                    LOGGER.error("Problem Executing Statement \"" + sql + "\"", e);
                } finally {
                    if (statement != null) {
                        try {
                            statement.close();
                            connection.close();
                        } catch (SQLException e) {
                            LOGGER.warn("Problem Closing Statement", e);
                        }
                    }
                }
            }
        }.start();
    }

    public static String getPreference(final String name) {
        Connection connection = getConnection();
        Statement statement = null;
        String preference = null;

        try {
            statement = connection.createStatement();
            final String sql = "SELECT value FROM preferences WHERE setting = '" + name + "';";
            LOGGER.info(String.format("Executing SQL Query - \"%s\"", sql));
            ResultSet resultSet = statement.executeQuery(sql);

            if (resultSet.next()) {
                preference = resultSet.getString("value");
            }

            resultSet.close();
        } catch (SQLException e) {
            LOGGER.error(String.format("Problem querying DB for Preference with name \"%s\"", name), e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                    connection.close();
                } catch (SQLException e) {
                    LOGGER.warn("Problem Closing Statement", e);
                }
            }
        }

        return preference;
    }

    public static void setPreference(final String name, final String value) {
        final String sql = String.format("INSERT INTO preferences (setting, value) \n" +
                "    VALUES \n" +
                "      ('%s',\n" +
                "      '%s')\n" +
                "ON DUPLICATE KEY UPDATE \n" +
                "  `setting` = '%s';", name, value, value);
        executeStatement(sql);
    }

    /**
     * Add a new User to the DB
     *
     * @param user {@link User} User to Create
     * @return {@link boolean} If user was created successfully
     */
    public static boolean createUser(final User user) {
        if (DB.getUser("email = '" + sanitize(user.getEmail().toLowerCase()) + "'").length > 0)
            return false;

        final String sql = "INSERT INTO users(email, first_name, last_name, notify, password) VALUES ( '" + sanitize(user.getEmail().toLowerCase()) +
                "', '" + sanitize(user.getFirstName()) + "', '" + sanitize(user.getLastName()) + "', " + (user.getNotify() ? 1 : 0) + "," +
                "'" + PASSWORD_ENCRYPTOR.encryptPassword(user.getPassword()) + "');";
        executeStatement(sql);
        return true;
    }

    /**
     * Get an Array of Users who match the specified criteria.
     * id is the Internal Identifier
     * Username is the Public Identifier
     *
     * @param restriction {@link String} Restriction on the Search, as a WHERE SQL Statement, the WHERE is already included
     * @return {@link User[]} Array of User Objects
     */
    public static User[] getUser(String restriction) {
        Connection connection = getConnection();
        Statement statement = null;
        List<User> users = new ArrayList<>();

        try {
            statement = connection.createStatement();
            restriction = restriction == null || restriction.isEmpty() ? "" : " WHERE " + restriction;
            final String sql = "SELECT * FROM users " + restriction + " ORDER BY last_name ASC;";
            LOGGER.info(String.format("Executing SQL Query - \"%s\"", sql));
            ResultSet resultSet = statement.executeQuery(sql);

            while (resultSet.next()) {
                User user = new User(
                        resultSet.getString("first_name"),
                        resultSet.getString("last_name"),
                        resultSet.getString("email"),
                        resultSet.getBoolean("notify"));
                users.add(user);
            }

            LOGGER.debug(String.format("Retrieved %d users from DB", users.size()));
            resultSet.close();
        } catch (SQLException e) {
            LOGGER.error("Problem querying DB for Users", e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                    connection.close();
                } catch (SQLException e) {
                    LOGGER.warn("Problem Closing Statement", e);
                }
            }
        }

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
        Connection connection = getConnection();
        Statement statement = null;
        User user = null;
        String passHash = "";

        try {
            statement = connection.createStatement();
            final String sql = "SELECT * FROM users WHERE email='" + sanitize(email.toLowerCase()) + "';";
            LOGGER.info(String.format("Executing SQL Query - \"%s\"", sql));
            ResultSet resultSet = statement.executeQuery(sql);

            if (resultSet.next()) {
                user = new User(
                        resultSet.getString("first_name"),
                        resultSet.getString("last_name"),
                        resultSet.getString("email"),
                        resultSet.getBoolean("notify"));

                passHash = resultSet.getString("password");
            }
            resultSet.close();
        } catch (SQLException e) {
            LOGGER.error("Problem querying DB for User by Username", e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                    connection.close();
                } catch (SQLException e) {
                    LOGGER.warn("Problem Closing Statement", e);
                }
            }
        }


        return password != null && !passHash.isEmpty() && PASSWORD_ENCRYPTOR.checkPassword(password, passHash) ? user : null;
    }

    /**
     * Update a Users Password. No verification of the password is done, and the statement will silently fail if the
     * user's email does not exist.
     *
     * @param email    {@link String} User's Email
     * @param password {@link String} User's new Password
     */
    public static void updateUserPassword(final String email, final String password) {
        //language=SQL
        final String sql = "UPDATE users SET password='" + PASSWORD_ENCRYPTOR.encryptPassword(password) + "'' WHERE email='" + email + "'';";

        executeStatement(sql);
    }

    // TODO Update User

    public static void deleteUser(final String email) {
        LOGGER.warn(String.format("Deleting User with Email: %s",email));
        //language=SQL
        final String sql = "DELETE FROM users WHERE email = '" + email + "';";
        executeStatement(sql);
    }

    /**
     * Get an Array of Recorders who match the specified criteria.
     *
     * @param restriction {@link String} Restriction on the Search, as a WHERE SQL Statement, the WHERE is already included
     * @return {@link Recorder[]} Array of Recorders
     */
    public static Recorder[] getRecorder(String restriction) {
        Connection connection = getConnection();
        Statement statement = null;
        List<Recorder> recorders = new ArrayList<>();

        try {
            statement = connection.createStatement();
            restriction = restriction == null || restriction.isEmpty() ? "" : " WHERE " + restriction;
            final String sql = "SELECT * FROM recorders " + restriction + " ORDER BY id ASC;";
            LOGGER.info(String.format("Executing SQL Query - \"%s\"", sql));
            ResultSet resultSet = statement.executeQuery(sql);

            while (resultSet.next()) {
                Recorder recorder = new Recorder(
                        resultSet.getString("id"),
                        resultSet.getString("name"),
                        resultSet.getString("description"),
                        resultSet.getString("serial_number"),
                        resultSet.getString("version"),
                        resultSet.getString("last_version_update_date"),
                        resultSet.getString("physical_address"),
                        resultSet.getString("image_version"),
                        Status.getByCode(resultSet.getInt("status")),
                        resultSet.getTimestamp("last_seen"));
                recorders.add(recorder);
            }

            LOGGER.debug(String.format("Retrieved %d recorders from DB", recorders.size()));
            resultSet.close();
        } catch (SQLException e) {
            LOGGER.error("Problem querying DB for Recorders", e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                    connection.close();
                } catch (SQLException e) {
                    LOGGER.warn("Problem Closing Statement", e);
                }
            }
        }

        return recorders.toArray(new Recorder[]{});
    }

    private static String sanitize(final String s) {
        return s.replace("'", "");
    }
}
