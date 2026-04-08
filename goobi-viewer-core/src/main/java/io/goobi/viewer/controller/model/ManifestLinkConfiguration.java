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
package io.goobi.viewer.controller.model;

import io.goobi.viewer.model.metadata.Metadata;

/**
 * Configuration for a manifest link entry displayed in the viewer, combining a display label,
 * a format identifier, and the metadata object that provides the target URL.
 */
public class ManifestLinkConfiguration {

    private final String label;
    private final String format;
    private final Metadata metadata;

    /**
     * @param label the display label for the manifest link
     * @param format the format identifier for the manifest
     * @param metadata the metadata object providing the link URL
     */
    public ManifestLinkConfiguration(String label, String format, Metadata metadata) {
        super();
        this.label = label;
        this.format = format;
        this.metadata = metadata;
    }

    
    public String getLabel() {
        return label;
    }

    
    public String getFormat() {
        return format;
    }

    public Metadata getMetadata() {
        return metadata;
    }
}
