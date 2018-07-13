package systems.whitestar.mediasite_monitor;

import lombok.extern.log4j.Log4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 * Test Vault IO Methods
 *
 * @author Tom Paulus
 * Created on 9/30/17.
 */
@Log4j
public class TestSecret {
    private static final String TEST_APP_NAME = UUID.randomUUID().toString();
    private static final String[] TEST_OBJECT = "The quick brown fox jumps over the lazy dog".split(" ");
    private static final String TEST_STRING = "world";

    @Before
    public void setUp() throws Exception {
        Secret secret = Secret.getInstance();

        Map<String, Object> samplePayload = new TreeMap<>();
        samplePayload.put("hello", TEST_STRING);
        samplePayload.put("abc", TEST_OBJECT);

        secret.setSecret(TEST_APP_NAME, samplePayload);
        log.info("Setting Test App - " + TEST_APP_NAME);
    }

    @After
    public void tearDown() throws Exception {
        log.warn("Deleting Test App - " + TEST_APP_NAME);
        Secret.getInstance().deleteSecret(TEST_APP_NAME);
    }

    @Test
    public void getSecretString() throws Exception {
        Secret secret = Secret.getInstance();
        String value = secret.getSecret(TEST_APP_NAME, "hello");
        assertNotNull(value);
        assertEquals(TEST_STRING, value);
    }

    @Test
    public void getSecretObject() throws Exception {
        Secret secret = Secret.getInstance();
        String[] value = secret.getSecret(TEST_APP_NAME, "abc", String[].class);
        assertNotNull(value);
        assertArrayEquals(TEST_OBJECT, value);
    }
}