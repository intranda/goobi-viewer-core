/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.intranda.digiverso.presentation.model.iiif.presentation.enums;

import org.apache.commons.compress.compressors.FileNameUtil;
import org.apache.commons.io.FilenameUtils;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author florian
 *
 */
public enum Format {

    IMAGE_JPEG("image/jpeg"),
    IMAGE_PNG("image/png"),
    VIDEO_MP4("video/mp4"),
    VIDEO_WEBM("video/webm"),
    AUDIO_OGG("audio/ogg"),
    AUDI_MP3("audio/mp3"),
    TEXT_PLAIN("text/plain"),
    TEXT_XML("text/xml"),
    TEXT_HTML("text/html"),
    APPLICATION_PDF("applocation/pdf");

    private String label;

    private Format(String label) {
        this.label = label;
    }

    /**
     * @return the label
     */
    @JsonValue
    public String getLabel() {
        return label;
    }

    /**
     * @param displayMimeType
     * @return
     */
    public static Format fromMimeType(String mimeType) {
        for (Format format : Format.values()) {
            if (mimeType.toLowerCase().equals(format.getLabel())) {
                return format;
            }
        }
        return null;
    }

    /**
     * @param displayMimeType
     * @return
     */
    public static Format fromFilename(String filename) {
        String suffix = FilenameUtils.getExtension(filename).toLowerCase();
        switch (suffix) {
            case "txt":
                return Format.TEXT_PLAIN;
            case "html":
            case "xhtml":
                return Format.TEXT_HTML;
            case "xml":
                return Format.TEXT_XML;
            case "ogg":
                return Format.AUDIO_OGG;
            case "mp3":
            case "mpeg3":
                return Format.AUDI_MP3;
            case "mp4":
            case "mpeg4":
                return Format.VIDEO_MP4;
            case "webm":
                return Format.VIDEO_WEBM;
            case "pdf":
                return Format.APPLICATION_PDF;
            case "png":
                return Format.IMAGE_PNG;
            default:
                return Format.IMAGE_JPEG;

        }
    }

}
