package edu.sdsu.its;

import org.apache.log4j.Logger;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Tom Paulus
 *         Created on 5/5/17.
 */
public class TestDB {
    final static Logger LOGGER = Logger.getLogger(TestDB.class);

    /**
     * Check if the KeyServer has access to the correct credentials
     */
    @Test
    public void checkParams() {
        final String db_url = Vault.getParam("db-url");
        LOGGER.debug("Vault.db-url = " + db_url);
        assertTrue("URL is Empty", db_url != null && db_url.length() > 0);
        assertTrue("Invalid URL", db_url.startsWith("jdbc:mysql://"));

        final String db_user = Vault.getParam("db-user");
        LOGGER.debug("Vault.db-user = " + db_user);
        assertTrue("Username is Empty", db_user != null && db_user.length() > 0);

        final String db_password = Vault.getParam("db-password");
        LOGGER.debug("Vault.db-password = " + db_password);
        assertTrue("Password is Empty", db_password != null && db_password.length() > 0);
    }


    /**
     * Test DB Connection
     */
    @Test
    public void connect() {
        Connection connection = null;
        try {
            LOGGER.debug("Attempting to connect to the DB Server");
            connection = DB.getConnection();
            LOGGER.info("DB Connection established");
            assertTrue(connection.isValid(5));
        } catch (SQLException e) {
            LOGGER.error("Problem connecting to the DB Server", e);
            fail("SQL Exception thrown while trying to connect to the DB - " + e.getMessage());
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    LOGGER.debug("DB Connection Closed");
                }
            } catch (SQLException e) {
                LOGGER.error("Problem closing the DB Connection", e);
            }
        }
    }
}
