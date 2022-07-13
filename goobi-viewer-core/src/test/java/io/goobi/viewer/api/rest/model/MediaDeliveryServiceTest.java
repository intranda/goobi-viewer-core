package io.goobi.viewer.api.rest.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class MediaDeliveryServiceTest {

    @Test
    public void test_matchRangeHeader() {
        {
            String range = "n-n";
            assertFalse(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }
        {
            String range = "bytes=13";
            assertFalse(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }
        {
            String range = "bytes=14-";
            assertTrue(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }
        {
            String range = "bytes=14-53";
            assertTrue(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }
        {
            String range = "bytes=14-15,165-23,52-";
            assertTrue(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }
        {
            String range = "bytes=14-34,15-6346";
            assertTrue(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }
        {
            String range = "bytes=-123";
            assertTrue(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }
        {
            String range = "bytes=14-34, -6346";
            assertTrue(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }
        {
            //test catastrophic backtracking
            String range = "bytes=14-34,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346";
            assertTrue(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }
        {
            //test catastrophic backtracking
            String range = "bytes=14-34,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-";
            assertTrue(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }{
            //test catastrophic backtracking
            String range = "bytes=14-34,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,15-6346,dfsdfsd";
            assertFalse(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }
        {
            String range = "bytes=-";
            assertFalse(MediaDeliveryService.matchesRangeHeaderPattern(range));
        }
    }

}
