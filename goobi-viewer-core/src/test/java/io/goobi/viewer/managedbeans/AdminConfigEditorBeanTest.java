package io.goobi.viewer.managedbeans;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.model.administration.configeditor.VimSwapFile;

class AdminConfigEditorBeanTest extends AbstractTest {

    private Path tempDir;
    private Path testFile;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        tempDir = Files.createTempDirectory("configeditor-test");
        testFile = tempDir.resolve("config.xml");
        Files.writeString(testFile, "<config/>");
    }

    @AfterEach
    void tearDown() throws IOException {
        Path swp = VimSwapFile.getSwapFilePath(testFile);
        Files.deleteIfExists(swp);
        Files.deleteIfExists(testFile);
        Files.deleteIfExists(tempDir);
        AdminConfigEditorBean.clearLocksForSessionId("session-a");
        AdminConfigEditorBean.clearLocksForSessionId("session-b");
    }

    @Test
    void unlockFile_shouldDeleteSwpFile() throws IOException {
        AdminConfigEditorBean.lockFile(testFile, "session-a");
        VimSwapFile.create(testFile, "testhost");

        AdminConfigEditorBean.unlockFile(testFile, "session-a");
        assertFalse(Files.exists(VimSwapFile.getSwapFilePath(testFile)),
                "unlockFile should delete the Web-UI swap file");
    }

    @Test
    void unlockFile_shouldNotDeleteVimSwpFile() throws IOException {
        Path swp = VimSwapFile.getSwapFilePath(testFile);
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(4096).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        buf.put(0, (byte) 'b').put(1, (byte) '0');
        buf.putInt(24, 42); // pid != 0
        Files.write(swp, buf.array());

        AdminConfigEditorBean.unlockFile(testFile, "session-a");
        assertTrue(Files.exists(swp), "vim-owned .swp must not be deleted by unlockFile");
        Files.delete(swp);
    }

    @Test
    void clearLocksForSessionId_shouldNotThrow() {
        assertDoesNotThrow(() -> AdminConfigEditorBean.clearLocksForSessionId("session-a"));
    }
}
