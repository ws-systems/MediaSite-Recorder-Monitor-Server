package edu.sdsu.its;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Tom Paulus
 * Created on 5/5/17.
 */
public class TestDB {
    private final static Logger LOGGER = Logger.getLogger(TestDB.class);

    @Before
    public void setUp() throws Exception {
        DB.setup();
    }

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
        LOGGER.debug("Attempting to connect to the DB Server");
        assertNotNull(DB.getSessionFactory());
        LOGGER.info("DB Connection established");
        assertTrue(DB.getSessionFactory().isOpen());
    }

    @After
    public void tearDown() throws Exception {
        DB.shutdown();
    }
}
