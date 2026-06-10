package io.goobi.viewer.model.administration.configeditor;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import io.goobi.viewer.AbstractTest;

class VimSwapFileTest extends AbstractTest {

    private Path tempDir;
    private Path testFile;

    @BeforeEach
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("vimswap-test");
        testFile = tempDir.resolve("config.xml");
        Files.writeString(testFile, "<config/>");
    }

    @AfterEach
    void tearDown() throws IOException {
        Path swp = VimSwapFile.getSwapFilePath(testFile);
        Files.deleteIfExists(swp);
        Files.deleteIfExists(testFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void getSwapFilePath_shouldReturnDotPrefixedSwpInSameDir() {
        Path swp = VimSwapFile.getSwapFilePath(testFile);
        assertEquals(tempDir, swp.getParent());
        assertEquals(".config.xml.swp", swp.getFileName().toString());
    }

    @Test
    void check_shouldReturnNoneWhenNoSwpExists() {
        assertEquals(VimSwapFile.Status.NONE, VimSwapFile.check(testFile));
    }

    @Test
    void create_shouldWriteValidB0Block() throws IOException {
        VimSwapFile.create(testFile, "testhost");
        Path swp = VimSwapFile.getSwapFilePath(testFile);
        assertTrue(Files.exists(swp));
        assertEquals(4096, Files.size(swp));

        byte[] bytes = Files.readAllBytes(swp);
        assertEquals('b', bytes[0]);
        assertEquals('0', bytes[1]);
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0, buf.getInt(24));
        String uname = new String(bytes, 28, 40, StandardCharsets.UTF_8).replace("\0", "");
        assertEquals("goobi-viewer-web", uname);
        String hname = new String(bytes, 68, 40, StandardCharsets.UTF_8).replace("\0", "");
        assertEquals("testhost", hname);
        String fname = new String(bytes, 108, 900, StandardCharsets.UTF_8).replace("\0", "");
        assertEquals(testFile.toAbsolutePath().toString(), fname);
    }

    @Test
    void check_shouldReturnLockedByWebuiAfterCreate() throws IOException {
        VimSwapFile.create(testFile, "testhost");
        assertEquals(VimSwapFile.Status.LOCKED_BY_WEBUI, VimSwapFile.check(testFile));
    }

    @Test
    void delete_shouldRemoveSwpFileWhenPidIsZero() throws IOException {
        VimSwapFile.create(testFile, "testhost");
        VimSwapFile.delete(testFile);
        assertFalse(Files.exists(VimSwapFile.getSwapFilePath(testFile)));
    }

    @Test
    void delete_shouldNotRemoveSwpFileWithNonZeroPid() throws IOException {
        Path swp = VimSwapFile.getSwapFilePath(testFile);
        byte[] block = new byte[4096];
        block[0] = 'b'; block[1] = '0';
        ByteBuffer.wrap(block).order(ByteOrder.LITTLE_ENDIAN).putInt(24, 12345);
        Files.write(swp, block);
        VimSwapFile.delete(testFile);
        assertTrue(Files.exists(swp), "vim-owned .swp must not be deleted");
        Files.delete(swp);
    }

    @Test
    void check_shouldReturnLockedByVimOrStaleForNonZeroPidWithDeadProcess() throws IOException {
        Path swp = VimSwapFile.getSwapFilePath(testFile);
        byte[] block = new byte[4096];
        block[0] = 'b'; block[1] = '0';
        ByteBuffer.wrap(block).order(ByteOrder.LITTLE_ENDIAN).putInt(24, 99999999);
        Files.write(swp, block);
        VimSwapFile.Status status = VimSwapFile.check(testFile);
        assertTrue(status == VimSwapFile.Status.STALE_VIM || status == VimSwapFile.Status.LOCKED_BY_VIM);
        Files.delete(swp);
    }

    @Test
    void check_shouldReturnLockedByVimForTruncatedFile() throws IOException {
        Path swp = VimSwapFile.getSwapFilePath(testFile);
        Files.write(swp, new byte[]{(byte)'b', (byte)'0'});
        assertEquals(VimSwapFile.Status.LOCKED_BY_VIM, VimSwapFile.check(testFile));
        Files.delete(swp);
    }

    @Test
    void delete_shouldNotThrowForNonExistentSwp() {
        assertDoesNotThrow(() -> VimSwapFile.delete(testFile));
    }

    /** @see VimSwapFile#delete(Path, String) @verifies delete swap file owned by the given session */
    @Test
    void delete_shouldDeleteSwapFileOwnedByTheGivenSession() throws IOException {
        VimSwapFile.create(testFile, "testhost", "session-A");
        VimSwapFile.delete(testFile, "session-A");
        assertFalse(Files.exists(VimSwapFile.getSwapFilePath(testFile)));
    }

    /** @see VimSwapFile#delete(Path, String) @verifies not delete swap file owned by another session */
    @Test
    void delete_shouldNotDeleteSwapFileOwnedByAnotherSession() throws IOException {
        VimSwapFile.create(testFile, "testhost", "session-A");
        VimSwapFile.delete(testFile, "session-B");
        assertTrue(Files.exists(VimSwapFile.getSwapFilePath(testFile)),
                "a .swp owned by another web-UI session must not be deleted");
    }

    /** @see VimSwapFile#check(Path) @verifies still report web-UI lock for owner-tagged swap file */
    @Test
    void check_shouldStillReportWebuiLockForOwnerTaggedSwapFile() throws IOException {
        VimSwapFile.create(testFile, "testhost", "session-A");
        assertEquals(VimSwapFile.Status.LOCKED_BY_WEBUI, VimSwapFile.check(testFile));
    }
}
