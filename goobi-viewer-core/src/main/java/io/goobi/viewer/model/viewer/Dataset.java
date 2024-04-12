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

package io.goobi.viewer.model.viewer;

import java.nio.file.Path;
import java.util.List;

public class Dataset {

    /** document identifier */
    private String pi;

    /** path to metadata file */
    private Path metadataFilePath;

    /** path to image folder */
    private Path mediaFolderPath;

    /** path to pdf folder */
    private Path pdfFolderPath;

    /** path to alto folder */
    private Path altoFolderPath;

    /** ordered image file list */
    private List<Path> mediaFiles;

    /** ordered pdf file list */
    private List<Path> pdfFiles;

    /** ordered alto file list */
    private List<Path> altoFiles;

    public String getPi() {
        return pi;
    }

    public void setPi(String pi) {
        this.pi = pi;
    }

    public Path getMetadataFilePath() {
        return metadataFilePath;
    }

    public void setMetadataFilePath(Path metadataFilePath) {
        this.metadataFilePath = metadataFilePath;
    }

    public Path getMediaFolderPath() {
        return mediaFolderPath;
    }

    public void setMediaFolderPath(Path mediaFolderPath) {
        this.mediaFolderPath = mediaFolderPath;
    }

    public Path getPdfFolderPath() {
        return pdfFolderPath;
    }

    public void setPdfFolderPath(Path pdfFolderPath) {
        this.pdfFolderPath = pdfFolderPath;
    }

    public Path getAltoFolderPath() {
        return altoFolderPath;
    }

    public void setAltoFolderPath(Path altoFolderPath) {
        this.altoFolderPath = altoFolderPath;
    }

    public List<Path> getMediaFiles() {
        return mediaFiles;
    }

    public void setMediaFiles(List<Path> mediaFiles) {
        this.mediaFiles = mediaFiles;
    }

    public List<Path> getPdfFiles() {
        return pdfFiles;
    }

    public void setPdfFiles(List<Path> pdfFiles) {
        this.pdfFiles = pdfFiles;
    }

    public List<Path> getAltoFiles() {
        return altoFiles;
    }

    public void setAltoFiles(List<Path> altoFiles) {
        this.altoFiles = altoFiles;
    }

}
