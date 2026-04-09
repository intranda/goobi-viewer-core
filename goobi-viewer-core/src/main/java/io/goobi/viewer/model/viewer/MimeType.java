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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.solr.common.SolrDocument;

import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Class to determine uses of various media mimetypes.
 */
public class MimeType {

    private final String name;

    public MimeType(String name) {
        this.name = Optional.ofNullable(name).orElse("").toLowerCase();
    }

    public MimeType(SolrDocument doc) {
        this(Optional.ofNullable(doc.getFieldValue(SolrConstants.MIMETYPE)).map(Object::toString).orElse(""));
    }

    public MimeType() {
        this("");
    }

    public static MimeType of(Path path) throws IOException {
        return new MimeType(FileTools.getMimeTypeFromFile(path));
    }

    public String getType() {
        if (name.contains("/")) {
            return name.substring(0, name.indexOf("/"));
        }
        return name;
    }

    public String getSubType() {
        if (name.contains("/")) {
            return name.substring(name.indexOf("/") + 1);
        }
        return "";
    }

    public String[] getSubTypes() {
        return getSubType().split(Pattern.quote("+"));
    }

    public boolean isImage() {
        return getType().equals("image");
    }

    public boolean isAudio() {
        return getType().equals("audio");
    }

    public boolean isVideo() {
        return getType().equals("video");
    }

    public boolean is3DModel() {
        return getType().equals("model") || getType().equals("object");
    }

    public boolean isMEI() {
        return this.name.equals("application/mei") || this.name.equals("application/mei+xml");
    }

    public boolean isPdf() {
        return this.name.equals("application/pdf");
    }

    public boolean isEpub() {
        //return getType().equals("application") && Arrays.contains(getSubTypes(), "epub"); //alt implementation
        return this.name.equals("application/epub+zip") || this.name.equals("application/epub");
    }

    public boolean isAllowsImageView() {
        return this.isImage() || isPdf();
    }

    public boolean isSandboxedHtml() {
        return this.name.equals("text/html-sandboxed");
    }

    public boolean isMediaType() {
        return isAllowsImageView() || isAudio() || isVideo() || isSandboxedHtml() || is3DModel();
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public int hashCode() {
        return this.getType().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass().equals(this.getClass())) {
            return this.name.equals(((MimeType) obj).name);
        }
        return false;
    }
}
