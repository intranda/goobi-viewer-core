package io.goobi.viewer.controller.shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProcessOutputReader implements Runnable {

    private static final Logger logger = LogManager.getLogger(ProcessOutputReader.class);

    private InputStream inputStream;
    private String output = "";
    private Charset charset = StandardCharsets.UTF_8;
    private boolean keepOutput = true;

    /**
     * 
     * @param inputStream
     */
    public ProcessOutputReader(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public void run() {
        try {
            StringBuilder textBuilder = new StringBuilder();
            try (Reader reader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
                int c = 0;
                while ((c = reader.read()) != -1) {
                    textBuilder.append((char) c);
                }
            }
            output = textBuilder.toString();
        } catch (UncheckedIOException | IOException e) {
            logger.warn("Input finished with error: {}", e.getMessage());
        }

    }

    public static char[] decodeWithCharset(byte[] origBytes, Charset charset) throws CharacterCodingException {
        CharsetDecoder decoder = charset.newDecoder();

        ByteBuffer byteBuffer = ByteBuffer.wrap(origBytes);
        CharBuffer charBuffer = decoder.decode(byteBuffer);

        return charBuffer.array();
    }

    public String getOutput() {
        return output;
    }

    public void writeToFile(File file) throws IOException {
        getFileFromString(getOutput(), file, false);
    }

    private static File getFileFromString(String string, File file, boolean append) throws IOException {
        try (FileWriter writer = new FileWriter(file, append)) {
            writer.write(string);
        }
        return file;
    }

    public boolean isKeepOutput() {
        return keepOutput;
    }

    public void setKeepOutput(boolean keepOutput) {
        this.keepOutput = keepOutput;
    }

}
