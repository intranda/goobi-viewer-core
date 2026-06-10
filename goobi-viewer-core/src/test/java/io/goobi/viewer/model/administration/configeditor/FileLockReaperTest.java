package io.goobi.viewer.model.administration.configeditor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;

class FileLockReaperTest extends AbstractTest {

    /** @see FileLockReaper#reap() @verifies not throw when invoked */
    @Test
    void reap_shouldNotThrowWhenInvoked() {
        FileLockReaper reaper = new FileLockReaper();
        assertDoesNotThrow(reaper::reap);
    }

    /** @see FileLockReaper#init() @verifies start and stop without throwing */
    @Test
    void lifecycle_shouldStartAndStopWithoutThrowing() {
        FileLockReaper reaper = new FileLockReaper();
        assertDoesNotThrow(reaper::init);
        assertDoesNotThrow(reaper::destroy);
    }
}
