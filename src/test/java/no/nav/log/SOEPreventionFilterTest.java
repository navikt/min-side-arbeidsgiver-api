package no.nav.log;

import no.nav.log.SOEPreventionFilter.ExceptionLoopDetector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// https://jira.qos.ch/browse/LOGBACK-1454
class SOEPreventionFilterTest {
    @Test
    void detectsLoopFromCircularInit() {
        Exception circular = new Exception("foo");
        Exception e2 = new Exception(circular);
        circular.initCause(e2);

        assertTrue(ExceptionLoopDetector.hasLoop(circular));
    }

    @Test
    void detectsLoopFromCircularSupressed() {
        Exception circular = new Exception("foo");
        Exception e2 = new Exception(circular);
        circular.addSuppressed(e2);

        assertTrue(ExceptionLoopDetector.hasLoop(circular));
    }

    @Test
    void noFalsePositive() {
        assertFalse(ExceptionLoopDetector.hasLoop(new RuntimeException("foo")));
    }
}