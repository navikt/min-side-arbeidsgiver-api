package no.nav.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

import java.util.Arrays;

/**
 * Dette filteret er en hack for å unngå java.lang.StackOverflowError siden spring boot ikke støtter logback 1.3 P.T.
 *
 * ref:
 * https://jira.qos.ch/browse/LOGBACK-1454
 * https://github.com/spring-projects/spring-boot/issues/12649
 *
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
            if (ExceptionLoopDetector.hasLoop(t)) {
                logger.log(marker, logger.getName(), Level.toLocationAwareLoggerInteger(level), msg, objects, new RuntimeException(t.getMessage()));
                return FilterReply.DENY;
            }
        } else { // log.level("lorum {} ipsum {}", str1, obj2, ex)
            Object lastArg = objects == null ? null : objects[objects.length - 1];
            if (lastArg instanceof Throwable) {
                if (ExceptionLoopDetector.hasLoop((Throwable) lastArg)) {
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
                new ThrowableProxy(t);
                return false;
            } catch (StackOverflowError e) {
                return true;
            }
        }
    }
}