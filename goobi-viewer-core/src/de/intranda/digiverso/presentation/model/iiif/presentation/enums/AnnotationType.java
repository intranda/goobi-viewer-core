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

/**
 * @author Florian Alpers
 *
 */
public enum AnnotationType {

    FULLTEXT,
    ALTO,
    AUDIO,
    VIDEO,
    HTML,
    PDF,
    COMMENT,
    TEI,
    CMDI;

    public Format getFormat() {
        switch (this) {
            case ALTO:
            case TEI:
            case CMDI:
                return Format.TEXT_XML;
            case HTML:
            case COMMENT:
                return Format.TEXT_HTML;
            case AUDIO:
                return Format.AUDI_MP3;
            case VIDEO:
                return Format.VIDEO_WEBM;
            case PDF:
                return Format.APPLICATION_PDF;
            case FULLTEXT:
            default:
                return Format.TEXT_PLAIN;
        }
    }
    
    public DcType getDcType() {
        switch (this) {
            case AUDIO:
                return DcType.SOUND;
            case VIDEO:
                return DcType.MOVING_IMAGE;
            case PDF:
                return DcType.SOFTWARE;
            case ALTO:
            case TEI:
            case CMDI:
            case HTML:
            case COMMENT:
            case FULLTEXT:
            default:
                return DcType.TEXT;
        }
    }
}
