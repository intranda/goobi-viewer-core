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
package io.goobi.viewer.model.job.download;

import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.model.files.external.Progress;

public class ExternalFilesDownloadJob {

    private final Progress progress;
    private final String identifier;
    private final Path path;
    private final String messageId;
    private final String errorMessage;

    public ExternalFilesDownloadJob(String identifier, String messageId, String errorMessage) {
        super();
        this.progress = new Progress(0, 1);
        this.identifier = identifier;
        this.path = Path.of("");
        this.messageId = messageId;
        this.errorMessage = errorMessage;
    }

    public ExternalFilesDownloadJob(Progress progress, String identifier, Path path, String messageId) {
        super();
        this.progress = progress;
        this.identifier = identifier;
        this.path = path;
        this.messageId = messageId;
        this.errorMessage = "";
    }

    public Progress getProgress() {
        return progress;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Path getPath() {
        return path;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isError() {
        return StringUtils.isNotBlank(errorMessage);
    }

}
