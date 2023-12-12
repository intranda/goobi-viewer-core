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

    private final static long DEFAULT_TIMEOUT_MILLIS = 8000;
    
    private String command[];
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

        logger.trace("execute shell command: " + StringUtils.join(command, " ")); //$NON-NLS-2$
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

            int result = process.waitFor(timeoutInMillis, TimeUnit.MILLISECONDS) ? 0 : -1;
            logger.debug("Done with shell command");
            bufferThread.join(timeoutInMillis);
            errorBufferThread.join(timeoutInMillis);
            logger.trace("All output read");
            return result;
        } finally {
            // Thread.sleep(2000);
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
            if(process != null) {
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
        } else {
            return "";
        }
    }

    public String getErrorOutput() {
        if (errorReader != null) {
            return errorReader.getOutput();
        } else {
            return "";
        }
    }

}

