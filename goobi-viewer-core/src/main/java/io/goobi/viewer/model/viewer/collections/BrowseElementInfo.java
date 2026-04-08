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
 * BrowseElementInfo interface.
 */
public interface BrowseElementInfo {

    /**
     * getDescription.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDescription();

    /**
     *
     * @param language language code for the requested description
     * @return a {@link java.lang.String} object.
     */
    public String getDescription(String language);

    /**
     * getName.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName();

    /**
     * getLinkURI.
     *
     * @param request a {@link jakarta.servlet.http.HttpServletRequest} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getLinkURI(HttpServletRequest request);

    /**
     * getLinkURI.
     *
     * @return a {@link java.net.URI} object.
     */
    public URI getLinkURI();

    /**
     * getIconURI.
     *
     * @return a {@link java.net.URI} object.
     */
    public URI getIconURI();

    /**
     * getIconURI.
     *
     * @param width a int.
     * @param height a int.
     * @return a {@link java.net.URI} object.
     */
    public URI getIconURI(int width, int height);

    /**
     * getIconURI.
     *
     * @param size a int.
     * @return a {@link java.net.URI} object.
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
     * @return a {@link de.intranda.metadata.multilanguage.IMetadataValue} object.
     */
    public IMetadataValue getTranslationsForName();

    /**
     * @return {@link IMetadataValue}
     */
    IMetadataValue getTranslationsForDescription();

}
