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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.MessageHandler;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.controller.shell.ShellCommand;
import io.goobi.viewer.model.job.TaskType;

public class PullThemeHandler implements MessageHandler<MessageStatus> {

    private static final Logger logger = LogManager.getLogger(PullThemeHandler.class);
    
    private static final String BASH_STATEMENT_PULL_THEME_REPOSITORY = "cd $VIEWERTHEMEPATH; git pull | grep -v -e \"Already up-to-date.\" -e \"Bereits aktuell.\"";

    @Override
    public MessageStatus call(ViewerMessage ticket) {
        
        Path themeRootPath = Path.of(DataManager.getInstance().getConfiguration().getThemeRootPath());
        if(Files.exists(themeRootPath) && Files.exists(themeRootPath.resolve(".git"))) {
            try {
                pullThemeRepository(themeRootPath);
                return MessageStatus.FINISH;
            } catch (IOException | InterruptedException e) {
                logger.error("Error pulling theme: {}", e.toString());
                ticket.getProperties().put("error", "Error pulling theme: " + e.toString());
                return MessageStatus.ERROR;
            }
        } else {
            ticket.getProperties().put("error", "Theme root path is not accessible or is not a git repository");
            return MessageStatus.ERROR;
        }
        
    }
    
    private void pullThemeRepository(Path themePath) throws IOException, InterruptedException {
        String commandString = BASH_STATEMENT_PULL_THEME_REPOSITORY.replace("$VIEWERTHEMEPATH", themePath.toAbsolutePath().toString());
        ShellCommand command = new ShellCommand(commandString.split("\\s+"));
        int ret = command.exec();
        if(ret > 0) {
            throw new IOException("Error executing command '" + commandString + "': " + command.getErrorOutput());
        }
    }

    @Override
    public String getMessageHandlerName() {
        return TaskType.PRERENDER_PDF.name();
    }



}
