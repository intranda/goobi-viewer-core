package io.goobi.viewer.model.administration.configeditor;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility für vim-kompatible Swap-Dateien (B0-Block-Format).
 * pid==0 im B0-Block ist der Web-UI-Marker. Vim schreibt immer eine echte PID.
 */
public final class VimSwapFile {

    private static final Logger logger = LogManager.getLogger(VimSwapFile.class);

    private static final int BLOCK_SIZE = 4096;
    private static final int OFFSET_PID = 24;
    private static final int OFFSET_UNAME = 28;
    private static final int OFFSET_HNAME = 68;
    private static final int OFFSET_FNAME = 108;
    private static final int LEN_UNAME = 40;
    private static final int LEN_HNAME = 40;
    private static final int LEN_FNAME = 900;
    private static final String WEBUI_UNAME = "goobi-viewer-web";

    public enum Status {
        NONE,
        LOCKED_BY_WEBUI,
        LOCKED_BY_VIM,
        STALE_VIM
    }

    private VimSwapFile() {}

    /**
     * Returns the path of the vim-style swap file for the given file.
     * The swap file is named {@code .<filename>.swp} and located in the same directory as {@code file}.
     *
     * @param file the file to lock
     * @return the swap file path
     */
    public static Path getSwapFilePath(Path file) {
        return file.getParent().resolve("." + file.getFileName().toString() + ".swp");
    }

    /**
     * Creates a vim-compatible B0-block swap file for {@code file}, using {@code pid=0} as the Web-UI marker.
     * Vim will recognise the swap file and warn any user who tries to open the file concurrently.
     *
     * @param file     the file to lock
     * @param hostname the hostname to embed in the swap file; if {@code null}, the local hostname is used
     * @throws IOException if the swap file cannot be written
     */
    public static void create(Path file, String hostname) throws IOException {
        Path swp = getSwapFilePath(file);
        byte[] block = new byte[BLOCK_SIZE];
        block[0] = (byte) 'b';
        block[1] = (byte) '0';
        byte[] version = "VIM 8.1\n\0\0".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(version, 0, block, 2, Math.min(version.length, 10));
        ByteBuffer.wrap(block).order(ByteOrder.LITTLE_ENDIAN).putInt(12, BLOCK_SIZE);
        // pid = 0 (already zero from array init) — Web-UI marker
        writeNullTerminated(block, OFFSET_UNAME, LEN_UNAME, WEBUI_UNAME);
        String h = (hostname != null) ? hostname : getLocalHostname();
        writeNullTerminated(block, OFFSET_HNAME, LEN_HNAME, h);
        writeNullTerminated(block, OFFSET_FNAME, LEN_FNAME, file.toAbsolutePath().toString());
        Files.write(swp, block);
    }

    /**
     * Deletes the Web-UI swap file for {@code file} if it exists and is owned by the Web UI ({@code pid=0}).
     * Swap files owned by a running vim process (non-zero pid) are never deleted.
     * If the swap file cannot be read, a warning is logged and no deletion is attempted.
     *
     * @param file the file whose swap file should be removed
     */
    public static void delete(Path file) {
        Path swp = getSwapFilePath(file);
        if (!Files.exists(swp)) {
            return;
        }
        try {
            int pid = readPid(swp);
            if (pid != 0) {
                logger.trace("Not deleting vim-owned swap file (pid={}): {}", pid, swp);
                return;
            }
            Files.delete(swp);
            logger.trace("Deleted web-UI swap file: {}", swp);
        } catch (IOException e) {
            logger.warn("Could not read/delete swap file {}: {}", swp, e.getMessage());
        }
    }

    /**
     * Checks the lock status of {@code file} by inspecting its swap file.
     * <ul>
     *   <li>{@link Status#NONE} – no swap file exists</li>
     *   <li>{@link Status#LOCKED_BY_WEBUI} – swap file present with {@code pid=0} (Web-UI lock)</li>
     *   <li>{@link Status#LOCKED_BY_VIM} – swap file owned by an active vim process, or unreadable/truncated</li>
     *   <li>{@link Status#STALE_VIM} – swap file owned by a non-zero pid that no longer exists in {@code /proc}</li>
     * </ul>
     * On systems where {@code /proc} is unavailable, a non-zero pid is conservatively reported as
     * {@link Status#LOCKED_BY_VIM}.
     *
     * @param file the file to check
     * @return the lock status
     */
    public static Status check(Path file) {
        Path swp = getSwapFilePath(file);
        if (!Files.exists(swp)) {
            return Status.NONE;
        }
        try {
            int pid = readPid(swp);
            if (pid == 0) {
                return Status.LOCKED_BY_WEBUI;
            }
            if (isProcAvailable()) {
                if (Files.exists(Path.of("/proc/" + pid))) {
                    return Status.LOCKED_BY_VIM;
                }
                return Status.STALE_VIM;
            }
            return Status.LOCKED_BY_VIM;
        } catch (IOException e) {
            logger.warn("Could not read swap file {}: {}", swp, e.getMessage());
            return Status.LOCKED_BY_VIM;
        }
    }

    private static int readPid(Path swp) throws IOException {
        byte[] bytes = Files.readAllBytes(swp);
        if (bytes.length < OFFSET_PID + 4) {
            throw new IOException("Swap file too short: " + bytes.length + " bytes");
        }
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt(OFFSET_PID);
    }

    private static void writeNullTerminated(byte[] block, int offset, int maxLen, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        int len = Math.min(bytes.length, maxLen - 1);
        System.arraycopy(bytes, 0, block, offset, len);
        block[offset + len] = 0;
    }

    private static boolean isProcAvailable() {
        return Files.isDirectory(Path.of("/proc"));
    }

    private static String getLocalHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
