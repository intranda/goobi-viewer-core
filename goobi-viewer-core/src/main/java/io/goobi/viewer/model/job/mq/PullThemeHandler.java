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
package io.goobi.viewer.model.job.mq;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.MessageHandler;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.controller.shell.ShellCommand;
import io.goobi.viewer.controller.variablereplacer.VariableReplacer;
import io.goobi.viewer.managedbeans.AdminDeveloperBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.job.TaskType;

public class PullThemeHandler implements MessageHandler<MessageStatus> {

    private static final Logger logger = LogManager.getLogger(PullThemeHandler.class);

    private static final String BASH_STATEMENT_PULL_THEME_REPOSITORY =
            "git -C $VIEWERTHEMEPATH pull";

    private static final String ALREADY_UP_TO_DATE_REGEX = "Already[\\s-]+up[\\s-]+to[\\s-]+date.?\\s*";

    @Inject
    private AdminDeveloperBean developerBean;

    @Override
    public MessageStatus call(ViewerMessage ticket) {
        updateProgress(0.1f);
        if (DataManager.getInstance().getConfiguration().getThemeRootPath() != null) {
            Path themeRootPath = Path.of(DataManager.getInstance().getConfiguration().getThemeRootPath()).toAbsolutePath();
            themeRootPath = Path.of("/").resolve(themeRootPath.subpath(0, themeRootPath.getNameCount() - 4));
            if (Files.exists(themeRootPath) && Files.exists(themeRootPath.resolve(".git"))) {
                try {
                    if (pullThemeRepository(themeRootPath)) {
                        updateProgress(1f);
                        return MessageStatus.FINISH;
                    }
                    updateProgress(1f);
                    return MessageStatus.IGNORE;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Message handler thread interrupted while waiting for bash call to finish");
                    ticket.getProperties()
                            .put(ViewerMessage.MESSAGE_PROPERTY_ERROR, "Message handler thread interrupted while waiting for bash call to finish");
                    sendProgressError("Message handler thread interrupted while waiting for bash call to finish");
                    return MessageStatus.ERROR;
                } catch (IOException e) {
                    logger.error("Error pulling theme: {}", e.toString());
                    ticket.getProperties().put(ViewerMessage.MESSAGE_PROPERTY_ERROR, "Error pulling theme: " + e.toString());
                    sendProgressError("Error pulling theme: " + e.toString());
                    return MessageStatus.ERROR;
                }
            }
            ticket.getProperties().put(ViewerMessage.MESSAGE_PROPERTY_ERROR, "Theme root path is not accessible or is not a git repository");
            sendProgressError("Theme root path is not accessible or is not a git repository");
            return MessageStatus.ERROR;
        }
        ticket.getProperties().put(ViewerMessage.MESSAGE_PROPERTY_ERROR, "No theme root path configured");
        sendProgressError("No theme root path configured");
        return MessageStatus.ERROR;
    }

    /**
     * 
     * @param message
     */
    private void sendProgressError(String message) {
        developerBean = (AdminDeveloperBean) BeanUtils.getBeanByName("adminDeveloperBean", AdminDeveloperBean.class);
        if (developerBean != null) {
            developerBean.sendPullThemeError(message);
        }
    }

    /**
     * 
     * @param f
     */
    private void updateProgress(float f) {
        developerBean = (AdminDeveloperBean) BeanUtils.getBeanByName("adminDeveloperBean", AdminDeveloperBean.class);
        if (developerBean != null) {
            if (f < 1) {
                developerBean.sendPullThemeUpdate(f);
            } else {
                developerBean.sendPullThemeFinished();
            }
        }
    }

    private static boolean pullThemeRepository(Path themePath) throws IOException, InterruptedException {
        
        String scriptTemplate = DataManager.getInstance().getConfiguration().getThemePullScriptPath();
        String commandString =  new VariableReplacer(DataManager.getInstance().getConfiguration()).replace(scriptTemplate);
        ShellCommand command = new ShellCommand(commandString.split("\\s+"));
        int ret = command.exec();
        String output = command.getOutput();
        String error = command.getErrorOutput();
        if (ret > 0) {
            throw new IOException("Error executing command '" + commandString + "': " + command.getErrorOutput());
        } else if (StringUtils.isNotBlank(error)) {
            throw new IOException("Error calling git pull: " + error);
        }  else {
            return !(output != null && output.matches(ALREADY_UP_TO_DATE_REGEX));
        }
    }

    @Override
    public String getMessageHandlerName() {
        return TaskType.PULL_THEME.name();
    }
}
