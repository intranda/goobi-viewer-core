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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.MessageHandler;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.controller.shell.ShellCommand;
import io.goobi.viewer.managedbeans.AdminDeveloperBean;
import io.goobi.viewer.managedbeans.PersistentStorageBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.job.TaskType;

public class PullThemeHandler implements MessageHandler<MessageStatus> {


    private static final Logger logger = LogManager.getLogger(PullThemeHandler.class);

    private static final String BASH_STATEMENT_PULL_THEME_REPOSITORY =
            "git -C $VIEWERTHEMEPATH pull | grep -v -e \"Already up-to-date.\" -e \"Bereits aktuell.\"";

    @Inject
    AdminDeveloperBean developerBean;
    
    @Override
    public MessageStatus call(ViewerMessage ticket) {
        updateProgress(0.1f);
        if (DataManager.getInstance().getConfiguration().getThemeRootPath() != null) {
            Path themeRootPath = Path.of(DataManager.getInstance().getConfiguration().getThemeRootPath()).toAbsolutePath();
            themeRootPath = Path.of("/").resolve(themeRootPath.subpath(0, themeRootPath.getNameCount()-4));
            if (Files.exists(themeRootPath) && Files.exists(themeRootPath.resolve(".git"))) {
                try {
                    pullThemeRepository(themeRootPath);
                    updateProgress(1f);
                    return MessageStatus.FINISH;
                }catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Message handler thread interrupted while waiting for bash call to finish");
                    ticket.getProperties().put(ViewerMessage.MESSAGE_PROPERTY_ERROR, "Message handler thread interrupted while waiting for bash call to finish");
                    sendProgressError("Message handler thread interrupted while waiting for bash call to finish");
                    return MessageStatus.ERROR;
                } catch (IOException e) {
                    logger.error("Error pulling theme: {}", e.toString());
                    ticket.getProperties().put(ViewerMessage.MESSAGE_PROPERTY_ERROR, "Error pulling theme: " + e.toString());
                    sendProgressError("Error pulling theme: " + e.toString());
                    return MessageStatus.ERROR;
                }
            } else {
                ticket.getProperties().put(ViewerMessage.MESSAGE_PROPERTY_ERROR, "Theme root path is not accessible or is not a git repository");
                sendProgressError("Theme root path is not accessible or is not a git repository");
                return MessageStatus.ERROR;
            }
        } else {
            ticket.getProperties().put(ViewerMessage.MESSAGE_PROPERTY_ERROR, "No theme root path configured");
            sendProgressError("No theme root path configured");
            return MessageStatus.ERROR;
        }
    }

    private void sendProgressError(String message) {
       developerBean = (AdminDeveloperBean) BeanUtils.getBeanByName("adminDeveloperBean", AdminDeveloperBean.class);
       if(developerBean != null) {
           developerBean.sendPullThemeError(message);
       }
    }
    
    private void updateProgress(float f) {
       developerBean = (AdminDeveloperBean) BeanUtils.getBeanByName("adminDeveloperBean", AdminDeveloperBean.class);
       if(developerBean != null) {
           if(f < 1) {               
               developerBean.sendPullThemeUpdate(f);
           } else {
               developerBean.sendPullThemeFinished();
           }
       }
    }

    private void pullThemeRepository(Path themePath) throws IOException, InterruptedException {
        String commandString = BASH_STATEMENT_PULL_THEME_REPOSITORY.replace("$VIEWERTHEMEPATH", themePath.toAbsolutePath().toString());
        ShellCommand command = new ShellCommand(commandString.split("\\s+"));
        int ret = command.exec();
        if (ret > 0) {
            throw new IOException("Error executing command '" + commandString + "': " + command.getErrorOutput());
        }
    }

    @Override
    public String getMessageHandlerName() {
        return TaskType.PULL_THEME.name();
    }

}
