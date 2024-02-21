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
package io.goobi.viewer.model.iiif.presentation.v2.builder;

import java.net.URI;

import de.intranda.api.iiif.presentation.content.LinkingContent;
import de.intranda.api.iiif.presentation.enums.DcType;
import de.intranda.api.iiif.presentation.enums.Format;
import de.intranda.metadata.multilanguage.IMetadataValue;

/**
 * @author florian
 *
 */
public class LinkingProperty {

    public enum LinkingType {
        RENDERING,
        SEE_ALSO,
        RELATED,
        SERVICE;
    }

    public enum LinkingTarget {
        PLAINTEXT(Format.TEXT_PLAIN, DcType.TEXT),
        ALTO(Format.TEXT_XML, DcType.TEXT),
        PDF(Format.APPLICATION_PDF, DcType.IMAGE),
        VIEWER(Format.TEXT_HTML, DcType.INTERACTIVE_RESOURCE),
        METS(Format.TEXT_XML, DcType.TEXT),
        LIDO(Format.TEXT_XML, DcType.TEXT);

        public final Format mimeType;
        public final DcType type;

        private LinkingTarget(Format mimeType, DcType type) {
            this.mimeType = mimeType;
            this.type = type;
        }
    }

    private final LinkingType type;
    private final LinkingTarget target;
    private final IMetadataValue label;

    /**
     * @param type
     * @param target
     * @param label
     */
    public LinkingProperty(LinkingType type, LinkingTarget target, IMetadataValue label) {
        super();
        this.type = type;
        this.target = target;
        this.label = label;
    }

    public LinkingContent getLinkingContent(URI id) {
        LinkingContent link = new LinkingContent(id);
        link.setFormat(target.mimeType);
        link.setType(target.type.getLabel());
        if (label != null && !label.isEmpty()) {
            link.setLabel(label);
        }
        return link;
    }

    /**
     * @return the target
     */
    public LinkingTarget getTarget() {
        return target;
    }

}
