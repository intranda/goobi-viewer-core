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

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.controller.mq.MessageHandler;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.controller.shell.ShellCommand;
import io.goobi.viewer.controller.variablereplacer.VariableReplacer;
import io.goobi.viewer.managedbeans.AdminDeveloperBean;
import io.goobi.viewer.managedbeans.AdminDeveloperBean.VersionInfo;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.job.TaskType;

public class PullThemeHandler implements MessageHandler<MessageStatus> {

    private static final Logger logger = LogManager.getLogger(PullThemeHandler.class);

    @Inject
    private AdminDeveloperBean developerBean;

    @Override
    public MessageStatus call(ViewerMessage ticket, MessageQueueManager queueManager) {
        updateProgress(0.1f);
        if (DataManager.getInstance().getConfiguration().getThemeRootPath() != null) {
            String result = "";
            try {
                result = pullThemeRepository();
                ticket.getProperties().put(ViewerMessage.MESSAGE_PROPERTY_INFO, result);
                sendProgressFinished(PullThemeHandler.getMessage(result));
                return MessageStatus.FINISH;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Message handler thread interrupted while waiting for bash call to finish");
                ticket.getProperties()
                        .put(ViewerMessage.MESSAGE_PROPERTY_ERROR, "Message handler thread interrupted while waiting for bash call to finish");
                sendProgressError("Message handler thread interrupted while waiting for bash call to finish");
                return MessageStatus.ERROR;
            } catch (IOException e) {
                logger.error("Error pulling theme: {}", e.toString());
                String errorMessage = "Error pulling theme: " + e.toString();
                errorMessage = errorMessage.substring(0, 255);
                ticket.getProperties().put(ViewerMessage.MESSAGE_PROPERTY_ERROR, errorMessage);
                sendProgressError("Error pulling theme: " + e.toString());
                return MessageStatus.ERROR;
            } catch (JDOMException e) {
                logger.error("Error Reading pull result '{}'; {}", result, e.toString());
                String errorMessage = "Error Reading pull result: " + e.toString();
                errorMessage = errorMessage.substring(0, 255);
                ticket.getProperties().put(ViewerMessage.MESSAGE_PROPERTY_ERROR, errorMessage);
                sendProgressError("Error Reading pull result: " + e.toString());
                return MessageStatus.ERROR;
            }
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

    private void sendProgressFinished(String message) {
        developerBean = (AdminDeveloperBean) BeanUtils.getBeanByName("adminDeveloperBean", AdminDeveloperBean.class);
        if (developerBean != null) {
            developerBean.sendPullThemeFinished(message);
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
                developerBean.sendPullThemeFinished("");
            }
        }
    }

    private String pullThemeRepository() throws IOException, InterruptedException {

        String scriptTemplate = DataManager.getInstance().getConfiguration().getThemePullScriptPath();
        String commandString = new VariableReplacer(DataManager.getInstance().getConfiguration()).replace(scriptTemplate);
        ShellCommand command = new ShellCommand(commandString.split("\\s+"));
        int ret = command.exec();
        String output = command.getOutput();
        String error = command.getErrorOutput();
        if (ret > 0) {
            throw new IOException("Error executing command '" + commandString + "': " + error);
        } else if (StringUtils.isNotBlank(error)) {
            throw new IOException("Error calling git pull: " + error);
        } else {
            return output;
        }
    }

    @Override
    public String getMessageHandlerName() {
        return TaskType.PULL_THEME.name();
    }

    public static VersionInfo getVersionInfo(String resultString, String buildDate) throws JDOMException {
        try {
            Document doc = XmlTools.getDocumentFromString(resultString, "utf-8");
            String branch = doc.getRootElement().getChildText("branch");
            String revision = doc.getRootElement().getChildText("revision");
            return new VersionInfo(DataManager.getInstance().getConfiguration().getName(), buildDate, revision, branch);
        } catch (IOException e) {
            throw new JDOMException(e.toString(), e);
        }
    }

    public static String getMessage(String resultString) throws JDOMException {
        try {
            Document doc = XmlTools.getDocumentFromString(resultString, "utf-8");
            return doc.getRootElement().getChildText("message");
        } catch (IOException e) {
            throw new JDOMException(e.toString(), e);
        }

    }

}
