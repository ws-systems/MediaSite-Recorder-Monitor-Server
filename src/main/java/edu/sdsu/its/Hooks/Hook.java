package edu.sdsu.its.Hooks;


import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fire Event Hooks on significant events thought MS Monitor.
 * All Hooks must be in the same package as the Hooks Class and should implement EventHook.
 * <p>
 * TODO Add Hook for Auditing (Pending)
 *
 * @author Tom Paulus
 * Created on 7/21/17.
 */
public enum Hook {
    USER_CREATE("onUserCreate"),
    USER_UPDATE("onUserUpdate"),
    RECORDER_RECORD_UPDATE("onRecorderRecordUpdate"),
    RECORDER_STATUS_UPDATE("onRecorderStatusUpdate"),
    RECORDER_ALARM_ACTIVATE("onRecorderAlarmActivate"),
    RECORDER_ALARM_CLEAR("onRecorderAlarmClear");

    private static final Logger LOGGER = Logger.getLogger(Hook.class);

    private @Getter
    String name;

    Hook(String name) {
        this.name = name;
    }

    /**
     * Fire a Hook to signal that a significant event has occurred
     *
     * @param hook    {@link Hook} Event
     * @param context {@link Object} Event Context (Why the event occurred)
     * @return {@link HookEvent[]} Hook Results
     * @throws IOException Thrown if the hook cannot be fired
     */
    public static List<HookEvent> fire(final Hook hook, final Object context) throws IOException {
        ImmutableSet<ClassPath.ClassInfo> classes = ClassPath.from(Hook.class.getClassLoader()).getTopLevelClasses(Hook.class.getPackage().getName());
        LOGGER.debug(String.format("Found %d classes of Event Hook Listeners", classes.size()));

        final List<HookEvent> statuses = Collections.synchronizedList(new ArrayList<>(classes.size()));

        for (final ClassPath.ClassInfo clazz : classes) {
            final Method[] methods = clazz.load().getDeclaredMethods();
            LOGGER.debug(String.format("Found %d methods for Hook Listener Class %s", methods.length, clazz.getName()));

            for (final Method method : methods) {
                final boolean isAbstract = Modifier.isAbstract(method.getModifiers());
                if (method.getName().equals(hook.getName()) && !isAbstract) {
                    new Thread(() -> {
                        LOGGER.debug(String.format("Starting new thread for Hook Method %s in class %s",
                                method.getName(),
                                clazz.getPackageName() + '.' + clazz.getName()));
                        if (context != null) {
                            LOGGER.debug("Hook Context - " + context.toString());
                        }
                        try {
                            statuses.add(new HookEvent(method, method.invoke(clazz.load().newInstance(), context)));
                        } catch (IllegalAccessException | InvocationTargetException | InstantiationException | IllegalArgumentException e) {
                            LOGGER.error("Problem firing Hook - " + hook, e);
                            statuses.add(new HookEvent(method, false));
                        }
                        LOGGER.debug(String.format("Thread to fire Hook Method %s in class %s Completed",
                                method.getName(),
                                clazz.getPackageName() + '.' + clazz.getName()));
                    }).start();

                    break;

                } else if (isAbstract) {
                    LOGGER.debug(String.format("Implementation of hook %s in class %s is abstract, skipping!", hook.getName(), clazz.getName()));
                }
            }
        }

        return statuses;
    }

    @AllArgsConstructor
    @Getter
    public static class HookEvent {
        private Method method;
        private Object response;
    }
}
