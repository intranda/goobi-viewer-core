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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ShellCommand {

    private static final Logger logger = LogManager.getLogger(ShellCommand.class);

    private static final long DEFAULT_TIMEOUT_MILLIS = 8000;

    private String[] command;
    private ProcessOutputReader outputReader;
    private ProcessOutputReader errorReader;
    private boolean keepOutput = true;

    public ShellCommand(String... command) {
        if (command == null || command.length == 0) {
            throw new IllegalArgumentException("Cannot execute empty shell command");
        }
        this.command = command;
    }

    public int exec() throws IOException, InterruptedException {
        return exec(DEFAULT_TIMEOUT_MILLIS);
    }

    public int exec(long timeoutInMillis) throws IOException, InterruptedException {
        if (logger.isDebugEnabled()) {
            logger.debug("execute shell command: {}", StringUtils.join(command, " ")); //$NON-NLS-2$
        }
        InputStream is = null;
        InputStream es = null;
        OutputStream out = null;
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
            is = process.getInputStream();
            es = process.getErrorStream();
            out = process.getOutputStream();

            outputReader = new ProcessOutputReader(is);
            outputReader.setKeepOutput(isKeepOutput());
            Thread bufferThread = new Thread(outputReader);
            bufferThread.start();

            errorReader = new ProcessOutputReader(es);
            errorReader.setKeepOutput(isKeepOutput());
            Thread errorBufferThread = new Thread(errorReader);
            errorBufferThread.start();

            boolean terminatedNormally = process.waitFor(timeoutInMillis, TimeUnit.MILLISECONDS);
            if (!terminatedNormally) {
                logger.warn("Script '{}' was interrupted due to timeout", StringUtils.join(command, " "));
            }
            int result = process.exitValue();
            logger.debug("Shell command executed successfully");
            bufferThread.join(timeoutInMillis);
            errorBufferThread.join(timeoutInMillis);
            logger.trace("All output read");
            return result;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    is = null;
                }
            }
            if (es != null) {
                try {
                    es.close();
                } catch (IOException e) {
                    es = null;
                }

            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    out = null;
                }
            }
            if (process != null) {
                process.destroy();
            }
        }
    }

    public boolean isKeepOutput() {
        return keepOutput;
    }

    public void setKeepOutput(boolean keepOutput) {
        this.keepOutput = keepOutput;
    }

    public String getOutput() {
        if (outputReader != null) {
            return outputReader.getOutput();
        }

        return "";
    }

    public String getErrorOutput() {
        if (errorReader != null) {
            return errorReader.getOutput();
        }

        return "";
    }

}
