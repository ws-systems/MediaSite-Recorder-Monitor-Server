package edu.sdsu.its;

import edu.sdsu.its.Hooks.ExampleHook;
import edu.sdsu.its.Hooks.Hook;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests that all Hooks are firing as they should and that all of the necessary hooks are being found via reflection.
 *
 * @author Tom Paulus
 * Created on 7/26/17.
 */
public class TestHooks {
    private static final int HOOK_DELAY = 5; // Allow for n seconds for all hooks to finish firing async

    @Test
    public void onUserCreate() throws Exception {
        List<Hook.HookEvent> results = Hook.fire(Hook.USER_CREATE, null);
        TimeUnit.SECONDS.sleep(HOOK_DELAY);

        assertNotNull(results);
        assertTrue(results.size() > 0);
        assertHookResponseContainsHook(ExampleHook.class, results);
    }

    @Test
    public void onUserUpdate() throws Exception {
        List<Hook.HookEvent> results = Hook.fire(Hook.USER_UPDATE, null);
        TimeUnit.SECONDS.sleep(HOOK_DELAY);

        assertNotNull(results);
        assertTrue(results.size() > 0);
        assertHookResponseContainsHook(ExampleHook.class, results);
    }

    @Test
    public void onRecorderRecordUpdate() throws Exception {
        List<Hook.HookEvent> results = Hook.fire(Hook.RECORDER_RECORD_UPDATE, null);
        TimeUnit.SECONDS.sleep(HOOK_DELAY);

        assertNotNull(results);
        assertTrue(results.size() > 0);
        assertHookResponseContainsHook(ExampleHook.class, results);
    }

    @Test
    public void onRecorderStatusUpdate() throws Exception {
        List<Hook.HookEvent> results = Hook.fire(Hook.RECORDER_STATUS_UPDATE, null);
        TimeUnit.SECONDS.sleep(HOOK_DELAY);

        assertNotNull(results);
        assertTrue(results.size() > 0);
        assertHookResponseContainsHook(ExampleHook.class, results);
    }

    @Test
    public void onRecorderAlarmActivate() throws Exception {
        List<Hook.HookEvent> results = Hook.fire(Hook.RECORDER_ALARM_ACTIVATE, null);
        TimeUnit.SECONDS.sleep(HOOK_DELAY);

        assertNotNull(results);
        assertTrue(results.size() > 0);
        assertHookResponseContainsHook(ExampleHook.class, results);
    }

    @Test
    public void onRecorderAlarmClear() throws Exception {
        List<Hook.HookEvent> results = Hook.fire(Hook.RECORDER_ALARM_CLEAR, null);
        TimeUnit.SECONDS.sleep(HOOK_DELAY);

        assertNotNull(results);
        assertTrue(results.size() > 0);
        assertHookResponseContainsHook(ExampleHook.class, results);
    }

    private static void assertHookResponseContainsHook(Class expectedClass, List<Hook.HookEvent> hookEvents) {
        boolean contains = false;

        for (Hook.HookEvent hookEvent : hookEvents) {
            final Class actualClass = hookEvent.getMethod().getDeclaringClass();
            if (expectedClass.equals(actualClass)) {
                contains = true;
                break;
            }
        }

        assertTrue(contains);
    }
}
