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
package io.goobi.viewer.model.files.upload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import javax.servlet.http.Part;

/**
 * This file capsules file uploading via jsf h:inputFile
 */
public class FileUploader {

    private Part file = null;
    private Path downloadPath = null;
    private byte[] fileContents = null;
    private Exception error = null;

    public void upload() {
        if (file != null) {
            try (InputStream input = file.getInputStream()) {
                fileContents = input.readAllBytes();
            } catch (IOException e) {
                this.error = e;
            }
        }
    }

    public Part getFile() {
        return file;
    }

    public void setFile(Part uploadedFile) {
        this.file = uploadedFile;
    }

    public byte[] getFileContents() {
        return fileContents;
    }

    public boolean isError() {
        return this.error != null;
    }

    public boolean isUploaded() {
        return fileContents != null;
    }

    public Exception getError() {
        return this.error;
    }

    public Path getDownloadPath() {
        return downloadPath;
    }

    public void setDownloadPath(Path downloadPath) {
        this.downloadPath = downloadPath;
    }

    public boolean isReadoForUpload() {
        return file != null;
    }

}
