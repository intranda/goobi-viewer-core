package io.goobi.viewer.model.administration.configeditor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.*;
import org.omnifaces.cdi.PushContext;

import io.goobi.viewer.AbstractTest;

class SwapFileWatcherTest extends AbstractTest {

    private Path tempDir;
    private PushContext mockPushContext;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        tempDir = Files.createTempDirectory("swapwatcher-test");
        mockPushContext = mock(PushContext.class);
    }

    @AfterEach
    void tearDown() throws IOException {
        try (var stream = Files.list(tempDir)) {
            stream.forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
        }
        Files.deleteIfExists(tempDir);
    }

    @Test
    void watcher_shouldPushOnSwpCreate() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> { latch.countDown(); return null; })
            .when(mockPushContext).send(anyString());

        SwapFileWatcher watcher = new SwapFileWatcher(mockPushContext);
        watcher.startWatching(tempDir);

        Path swp = tempDir.resolve(".config.xml.swp");
        Files.writeString(swp, "test");

        boolean notified = latch.await(3, TimeUnit.SECONDS);
        watcher.stopWatching();

        assertTrue(notified, "PushContext.send should be called when a .swp file appears");
    }

    @Test
    void watcher_shouldPushOnSwpDelete() throws Exception {
        Path swp = tempDir.resolve(".config.xml.swp");
        Files.writeString(swp, "test");

        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> { latch.countDown(); return null; })
            .when(mockPushContext).send(anyString());

        SwapFileWatcher watcher = new SwapFileWatcher(mockPushContext);
        watcher.startWatching(tempDir);
        Thread.sleep(200);

        Files.delete(swp);
        boolean notified = latch.await(3, TimeUnit.SECONDS);
        watcher.stopWatching();

        assertTrue(notified, "PushContext.send should be called when a .swp file is deleted");
    }

    @Test
    void watcher_shouldNotPushForNonSwpFiles() throws Exception {
        SwapFileWatcher watcher = new SwapFileWatcher(mockPushContext);
        watcher.startWatching(tempDir);
        Thread.sleep(200);

        Files.writeString(tempDir.resolve("config.xml"), "<config/>");
        Thread.sleep(500);

        watcher.stopWatching();
        verify(mockPushContext, never()).send(anyString());
    }

    @Test
    void stopWatching_shouldNotThrow() {
        SwapFileWatcher watcher = new SwapFileWatcher(mockPushContext);
        assertDoesNotThrow(watcher::stopWatching);
    }
}
