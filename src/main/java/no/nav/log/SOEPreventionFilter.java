package no.nav.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import com.google.common.base.Throwables;
import org.slf4j.Marker;

import java.util.Arrays;
import java.util.HashSet;

import static no.nav.log.SOEPreventionFilter.ExceptionLoopDetector.hasLoop;

/**
 * Dette filteret er en hack for å unngå java.lang.StackOverflowError siden spring boot ikke støtter logback 1.3 P.T.
 * <p>
 * ref:
 * https://jira.qos.ch/browse/LOGBACK-1454
 * https://github.com/spring-projects/spring-boot/issues/12649
 * <p>
 * obs: Hele denne klassen kan fjernes når vi er over på en versjon av logback som takler exceptions med sirkusreferanser
 */
public class SOEPreventionFilter extends TurboFilter {

    @Override
    public FilterReply decide(
            Marker marker,
            Logger logger,
            Level level,
            String msg,
            Object[] objects,
            Throwable t
    ) {
        if (t != null) { // log.level("", ex)
            if (hasLoop(t)) {
                logger.log(marker, logger.getName(), Level.toLocationAwareLoggerInteger(level), msg, objects, new RuntimeException(t.getMessage()));
                return FilterReply.DENY;
            }
        } else { // log.level("lorum {} ipsum {}", str1, obj2, ex)
            Object lastArg = objects == null ? null : objects[objects.length - 1];
            if (lastArg instanceof Throwable) {
                if (hasLoop((Throwable) lastArg)) {
                    Object[] copy = Arrays.copyOf(objects, objects.length);
                    copy[objects.length - 1] = new RuntimeException(((Throwable) lastArg).getMessage());
                    logger.log(marker, logger.getName(), Level.toLocationAwareLoggerInteger(level), msg, copy, t);
                    return FilterReply.DENY;
                }

                return FilterReply.NEUTRAL;
            }
        }

        return FilterReply.NEUTRAL;
    }

    static class ExceptionLoopDetector {

        /**
         * test om throwable lar seg proxye i logback
         */
        static boolean hasLoop(Throwable t) {
            try {
                //noinspection ResultOfMethodCallIgnored,UnstableApiUsage
                Throwables.getCausalChain(t);
                return searchSupressed(t, new HashSet<>());
            } catch (Exception e) {
                return true;
            }
        }

        private static boolean searchSupressed(Throwable t, HashSet<Throwable> seen) {
            boolean alreadyInSet = !seen.add(t);
            if (alreadyInSet) {
                return true;
            }
            for (Throwable s : t.getSuppressed()) {
                if (seen.contains(s)) {
                    return true;
                } else {
                    seen.add(s);
                    return searchSupressed(s, seen);
                }
            }
            return false;
        }
    }
}