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
package io.goobi.viewer.model.iiif.presentation.v3.builder;

import java.net.URI;

import de.intranda.api.iiif.presentation.enums.Format;
import de.intranda.api.iiif.presentation.v3.LabeledResource;
import de.intranda.metadata.multilanguage.IMetadataValue;

/**
 * @author florian
 *
 */
public class LinkingProperty {

    public enum LinkingTarget {
        PLAINTEXT("Text", Format.TEXT_PLAIN, null),
        ALTO("Dataset", Format.TEXT_XML, "https://www.loc.gov/alto/"),
        METS("Dataset", Format.TEXT_XML, "https://www.loc.gov/mets/"),
        LIDO("Dataset", Format.TEXT_XML, "http://www.lido-schema.org"),
        TEI("Text", Format.TEXT_XML, "http://www.tei-c.org"),
        PDF("Text", Format.APPLICATION_PDF, null),
        VIEWER("Text", Format.TEXT_HTML, null);

        private final Format mimeType;
        private final String type;
        private final String profile;

        private LinkingTarget(String type, Format mimeType, String profile) {
            this.type = type;
            this.mimeType = mimeType;
            this.profile = profile;
        }
    }

    private final LinkingTarget target;
    private final IMetadataValue label;

    /**
     * @param target
     * @param label
     */
    public LinkingProperty(LinkingTarget target, IMetadataValue label) {
        super();
        this.target = target;
        this.label = label;
    }

    /**
     * 
     * @param id
     * @return {@link LabeledResource}
     */
    public LabeledResource getResource(URI id) {
        return new LabeledResource(id, target.type, target.mimeType.getLabel(), target.profile, label);
    }

    /**
     * @return the target
     */
    public LinkingTarget getTarget() {
        return target;
    }

    /**
     * @return the label
     */
    public IMetadataValue getLabel() {
        return label;
    }
}
