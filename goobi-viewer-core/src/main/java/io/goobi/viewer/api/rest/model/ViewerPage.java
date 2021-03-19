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
package io.goobi.viewer.api.rest.model;

import java.net.URI;

import de.intranda.api.iiif.presentation.content.IContent;
import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.model.cms.CMSContentItem.CMSContentItemType;
import io.goobi.viewer.model.cms.CMSPage;

/**
 * Simple template to create a json-representation of a viewer page, typically a CMS-Page.
 * Used to provide slides to sliders
 * 
 * @author florian
 *
 */
public class ViewerPage {

    public final URI link;
    public final IContent image;
    public final IMetadataValue header;
    public final IMetadataValue description;
    /**
     * @param link
     * @param image
     * @param header
     * @param description
     */
    public ViewerPage(URI link, IContent image, IMetadataValue header, IMetadataValue description) {
        super();
        this.link = link;
        this.image = image;
        this.header = header;
        this.description = description;
    }
    
    public ViewerPage(CMSPage page) {
        this.header = page.getTitleTranslations();
        this.description = page.getPreviewTranslations();
        this.link = URI.create(page.getUrl());
        this.image = page.getGlobalContentItems().stream()
                .filter(item -> CMSContentItemType.MEDIA.equals(item.getType()))
                .sorted()
                .map(item -> MediaItem.getMediaResource(item.getMediaItem()))
                .findFirst().orElse(null);
    }
}
