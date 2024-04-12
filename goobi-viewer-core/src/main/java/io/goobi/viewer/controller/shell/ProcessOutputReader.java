/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
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
