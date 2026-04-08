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
package io.goobi.viewer.model.viewer.collections;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import de.intranda.metadata.multilanguage.IMetadataValue;

/**
 * Interface for metadata and navigation information attached to a collection browse element.
 * Provides multilingual name and description, navigation and icon URIs (optionally scaled to a requested size),
 * and a flag indicating whether a description is available.
 */
public interface BrowseElementInfo {

    /**
     * getDescription.
     *
     * @return the description of this browse element in the default language
     */
    public String getDescription();

    /**
     *
     * @param language language code for the requested description
     * @return the description of this browse element in the given language
     */
    public String getDescription(String language);

    /**
     * getName.
     *
     * @return the name of this browse element
     */
    public String getName();

    /**
     * getLinkURI.
     *
     * @param request a {@link jakarta.servlet.http.HttpServletRequest} object.
     * @return the link URI for this browse element, resolved relative to the given request context
     */
    public URI getLinkURI(HttpServletRequest request);

    /**
     * getLinkURI.
     *
     * @return the link URI for this browse element
     */
    public URI getLinkURI();

    /**
     * getIconURI.
     *
     * @return the icon URI for this browse element, or null if no icon is set
     */
    public URI getIconURI();

    /**
     * getIconURI.
     *
     * @param width a int.
     * @param height a int.
     * @return the icon URI scaled to the given width and height, or null if no icon is set
     */
    public URI getIconURI(int width, int height);

    /**
     * getIconURI.
     *
     * @param size a int.
     * @return the icon URI scaled to the given square size, or null if no icon is set
     */
    public URI getIconURI(int size);

    /**
     * hasDescription.
     *
     * @return true if this browse element has a non-blank description, false otherwise
     */
    default boolean hasDescription() {
        return StringUtils.isNotBlank(getDescription());
    }

    /**
     * getTranslationsForName.
     *
     * @return the multilingual name translations for this browse element
     */
    public IMetadataValue getTranslationsForName();

    /**
     * @return {@link IMetadataValue}
     */
    IMetadataValue getTranslationsForDescription();

}
