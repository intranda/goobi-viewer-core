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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.messages.ViewerResourceBundle;

/**
 * <p>
 * SimpleBrowseElementInfo class.
 * </p>
 */
public class SimpleBrowseElementInfo implements BrowseElementInfo {

    private static final Logger logger = LogManager.getLogger(SimpleBrowseElementInfo.class);

    private String description = null;
    private URI linkURI = null;
    private URI iconURI = null;
    private String collectionName;

    /**
     * <p>
     * Constructor for SimpleBrowseElementInfo.
     * </p>
     *
     * @param collectionName a {@link java.lang.String} object.
     */
    public SimpleBrowseElementInfo(String collectionName) {
        this.collectionName = collectionName;
    }

    /**
     * <p>
     * Constructor for SimpleBrowseElementInfo.
     * </p>
     *
     * @param collectionName a {@link java.lang.String} object.
     * @param linkURI a {@link java.net.URI} object.
     * @param iconURI a {@link java.net.URI} object.
     */
    public SimpleBrowseElementInfo(String collectionName, URI linkURI, URI iconURI) {
        this.collectionName = collectionName;
        this.linkURI = linkURI;
        this.iconURI = iconURI;
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return description;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.viewer.BrowseElementInfo#getDescription(java.lang.String)
     */
    @Override
    public String getDescription(String language) {
        return description;
    }

    /**
     * <p>
     * Setter for the field <code>description</code>.
     * </p>
     *
     * @param description a {@link java.lang.String} object.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /** {@inheritDoc} */
    @Override
    public URI getLinkURI(HttpServletRequest request) {
        return linkURI;
    }

    /** {@inheritDoc} */
    @Override
    public URI getLinkURI() {
        return linkURI;
    }

    /**
     * <p>
     * Setter for the field <code>linkURI</code>.
     * </p>
     *
     * @param linkURI a {@link java.net.URI} object.
     */
    public void setLinkURI(URI linkURI) {
        this.linkURI = linkURI;
    }

    /** {@inheritDoc} */
    @Override
    public URI getIconURI() {
        return this.iconURI;
    }

    /**
     * <p>
     * Setter for the field <code>iconURI</code>.
     * </p>
     *
     * @param iconURI a {@link java.net.URI} object.
     */
    public void setIconURI(URI iconURI) {
        this.iconURI = iconURI;
    }

    /** {@inheritDoc} */
    @Override
    public URI getIconURI(int width, int height) {
        return getIconURI();
    }

    /** {@inheritDoc} */
    @Override
    public URI getIconURI(int size) {
        return getIconURI();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.viewer.BrowseElementInfo#getName()
     */
    /** {@inheritDoc} */
    @Override
    public String getName() {
        return collectionName;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.viewer.BrowseElementInfo#getTranslationsForName()
     */
    /** {@inheritDoc} */
    @Override
    public IMetadataValue getTranslationsForName() {
        return ViewerResourceBundle.getTranslations(getName(), false);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.viewer.BrowseElementInfo#getTranslationsForDescription()
     */
    @Override
    public IMetadataValue getTranslationsForDescription() {
        return null;
    }
}
