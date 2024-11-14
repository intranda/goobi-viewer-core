package io.goobi.viewer.model.statistics.usage;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class StatisticsSummaryTest {

    @Test
    void test_isOlderThan() {
        LocalDateTime creationTime = LocalDateTime.of(2020, 1, 1, 12, 0, 0);
        LocalDateTime now = LocalDateTime.of(2020, 1, 1, 12, 1, 0); //one minute later

        StatisticsSummary summary = Mockito.spy(StatisticsSummary.class);
        Mockito.when(summary.getCreationTime()).thenReturn(creationTime);

        Assertions.assertTrue(summary.isOlderThan(30, ChronoUnit.SECONDS, now));
        Assertions.assertFalse(summary.isOlderThan(2, ChronoUnit.MINUTES, now));
    }

}
