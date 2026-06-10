package io.goobi.viewer.managedbeans;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;
import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
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

        AdminConfigEditorBean.unlockFile(testFile, "session-a");
        assertFalse(Files.exists(VimSwapFile.getSwapFilePath(testFile)),
                "unlockFile should delete the Web-UI swap file");
    }

    /**
     * @see AdminConfigEditorBean#lockFile(Path, String)
     * @verifies create owner-tagged swap file on lock
     */
    @Test
    void lockFile_shouldCreateOwnerTaggedSwapFileOnLock() {
        assertTrue(AdminConfigEditorBean.lockFile(testFile, "session-a"));
        assertTrue(Files.exists(VimSwapFile.getSwapFilePath(testFile)));
    }

    /**
     * @see AdminConfigEditorBean#renewLock(Path, String)
     * @verifies renew lock held by same session
     */
    @Test
    void renewLock_shouldRenewLockHeldBySameSession() {
        AdminConfigEditorBean.lockFile(testFile, "session-a");
        assertTrue(AdminConfigEditorBean.renewLock(testFile, "session-a"));
    }

    /**
     * @see AdminConfigEditorBean#renewLock(Path, String)
     * @verifies return false for foreign or absent lock
     */
    @Test
    void renewLock_shouldReturnFalseForForeignOrAbsentLock() {
        AdminConfigEditorBean.lockFile(testFile, "session-a");
        assertFalse(AdminConfigEditorBean.renewLock(testFile, "session-b"));
        assertFalse(AdminConfigEditorBean.renewLock(tempDir.resolve("never-locked.xml"), "session-a"));
    }

    /**
     * @see AdminConfigEditorBean#removeExpiredLocks()
     * @verifies not throw when no locks present
     */
    @Test
    void removeExpiredLocks_shouldNotThrowWhenNoLocksPresent() {
        assertDoesNotThrow(AdminConfigEditorBean::removeExpiredLocks);
    }

    /**
     * @see AdminConfigEditorBean#unlockFile(Path, String)
     * @verifies delete only own swap file
     */
    @Test
    void unlockFile_shouldDeleteOnlyOwnSwapFile() {
        AdminConfigEditorBean.lockFile(testFile, "session-a");
        AdminConfigEditorBean.unlockFile(testFile, "session-b"); // not the owner
        assertTrue(Files.exists(VimSwapFile.getSwapFilePath(testFile)));
        AdminConfigEditorBean.unlockFile(testFile, "session-a"); // owner
        assertFalse(Files.exists(VimSwapFile.getSwapFilePath(testFile)));
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

    /**
     * @see AdminConfigEditorBean#isFileLocked(Path, String)
     * @verifies return true if file locked by other session id
     */
    @Test
    void isFileLocked_shouldReturnTrueIfFileLockedByOtherSessionId() {
        AdminConfigEditorBean.lockFile(testFile, "session-a");
        assertTrue(AdminConfigEditorBean.isFileLocked(testFile, "session-b"));
    }

    /**
     * @see AdminConfigEditorBean#isFileLocked(Path, String)
     * @verifies return false if file locked by own session id
     */
    @Test
    void isFileLocked_shouldReturnFalseIfFileLockedByOwnSessionId() {
        AdminConfigEditorBean.lockFile(testFile, "session-a");
        assertFalse(AdminConfigEditorBean.isFileLocked(testFile, "session-a"));
    }

    /**
     * @see AdminConfigEditorBean#isFileLocked(Path, String)
     * @verifies return false if file not locked
     */
    @Test
    void isFileLocked_shouldReturnFalseIfFileNotLocked() {
        assertFalse(AdminConfigEditorBean.isFileLocked(testFile, "session-a"));
    }

    /**
     * @see AdminConfigEditorBean#setCurrentFileName(String)
     * @verifies not change selection on postback
     */
    @Test
    void setCurrentFileName_shouldNotChangeSelectionOnPostback() throws Exception {
        FacesContext facesContext = ContextMocker.mockFacesContext();
        try {
            Mockito.when(facesContext.isPostback()).thenReturn(true);
            AdminConfigEditorBean bean = new AdminConfigEditorBean();
            // Without the postback guard this would attempt a selection and throw FileNotFoundException
            // (no such record in the file listing); with the guard the call is a no-op.
            bean.setCurrentFileName("no-such-file.xml");
            assertEquals("-", bean.getCurrentFileName());
        } finally {
            facesContext.release();
        }
    }

    /**
     * @see AdminConfigEditorBean#isFileLocked(Path, String)
     * @verifies return true if file locked by external vim
     */
    @Test
    void isFileLocked_shouldReturnTrueIfFileLockedByExternalVim() throws IOException {
        // Write a vim-style swap file owning a live pid (the current JVM process), which check() reports as LOCKED_BY_VIM
        Path swp = VimSwapFile.getSwapFilePath(testFile);
        byte[] block = new byte[4096];
        block[0] = 'b';
        block[1] = '0';
        int livePid = (int) ProcessHandle.current().pid();
        ByteBuffer.wrap(block).order(ByteOrder.LITTLE_ENDIAN).putInt(24, livePid);
        Files.write(swp, block);

        assertTrue(AdminConfigEditorBean.isFileLocked(testFile, "session-a"));
    }
}
