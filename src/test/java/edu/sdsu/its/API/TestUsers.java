package edu.sdsu.its.API;

import edu.sdsu.its.API.Models.User;
import edu.sdsu.its.DB;
import lombok.extern.log4j.Log4j;
import org.junit.*;
import org.junit.rules.ExpectedException;

import javax.persistence.PersistenceException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author Tom Paulus
 * Created on 8/7/17.
 */
@Log4j
public class TestUsers {
    private static final String TEST_USER_EMAIL = "its+devotests@mail.sdsu.edu";

    private User testUser;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void setUpClass() throws Exception {
        DB.setup();
    }

    @Before
    public void setUp() throws Exception {
        testUser = User.builder()
                .firstName("Test")
                .lastName("User")
                .email(TEST_USER_EMAIL)
                .password(UUID.randomUUID().toString())
                .notify(false)
                .build();

        testUser = DB.updateUser(testUser);
        TimeUnit.SECONDS.sleep(5);
        log.info("Created User -" + testUser.toString());
    }

    @Test
    public void getTestUser() throws Exception {
        User[] dbUsers = DB.getUser("u.email = '" + TEST_USER_EMAIL + "'");
        assertNotNull(dbUsers);
        assertTrue(dbUsers.length == 1);
        assertEquals(testUser, dbUsers[0]);
    }

    @Test
    public void updateTestUser() throws Exception {
        assertNotNull(testUser);

        testUser.setFirstName("Jane");
        assertEquals(testUser, DB.updateUser(testUser));

        testUser.setLastName("User");
        assertEquals(testUser, DB.updateUser(testUser));

        testUser.setEmail("another+email@example.org");
        assertEquals(testUser, DB.updateUser(testUser));

        testUser.setNotify(true);
        assertEquals(testUser, DB.updateUser(testUser));

        testUser.setPassword(UUID.randomUUID().toString());
        assertEquals(testUser, DB.updateUser(testUser));
    }

    @Test
    public void updateWithExistingEmail() throws Exception {
        User otherUser = User.builder()
                .firstName("Other Test")
                .lastName("Person")
                .email(TEST_USER_EMAIL)
                .password(UUID.randomUUID().toString())
                .notify(false)
                .build();

        thrown.expect(PersistenceException.class);
        thrown.expectMessage("org.hibernate.exception.ConstraintViolationException: could not execute statement");
        assertNull(DB.updateUser(otherUser));
        assertTrue(DB.getUser("u.email = '" + TEST_USER_EMAIL + "'").length == 1);
    }

    @After
    public void tearDown() throws Exception {
        if (testUser != null) {
            DB.deleteUser(testUser);
        } else {
            log.warn("Skipping User Delete Step, User does not exist");
        }
    }

    @AfterClass
    public static void tearDownDB() throws Exception {
        DB.shutdown();
    }
}
