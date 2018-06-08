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
package de.intranda.digiverso.presentation.model.viewer;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;

public class SimpleBrowseElementInfo implements BrowseElementInfo {

    private static final Logger logger = LoggerFactory.getLogger(SimpleBrowseElementInfo.class);

    private String description = null;
    private URI linkURI = null;
    private URI iconURI = null;
    private String collectionName;

    public SimpleBrowseElementInfo(String collectionName) {
        this.collectionName = collectionName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public URI getLinkURI(HttpServletRequest request) {
        return linkURI;
    }

    public void setLinkURI(URI linkURI) {
        this.linkURI = linkURI;
    }

    @Override
    public URI getIconURI() {
        if (this.iconURI == null) {
            this.iconURI = createIconURI(this.collectionName);
        }
        return this.iconURI;
    }

    public void setIconURI(URI iconURI) {
        this.iconURI = iconURI;
    }

    protected static URI createIconURI(String collectionName) {
        String icon = DataManager.getInstance()
                .getConfiguration()
                .getDefaultBrowseIcon(collectionName);
        if (StringUtils.isBlank(icon)) {
            return null;
        }
        try {
            URI iconURI = new URI(icon);
            if (!iconURI.isAbsolute()) {
                String iconPath = BeanUtils.getServletPathWithHostAsUrlFromJsfContext() +  "/resources/themes/" + BeanUtils.getNavigationHelper()
                        .getTheme() + "/" + icon;
                iconURI = new URI(iconPath);
            }
            return iconURI;
        } catch (URISyntaxException e) {
            logger.error("Unable to parse " + icon + " as URI");
            return null;
        } catch (NullPointerException e) {
            logger.error("Unable to create icon path. Probably due to missing jsf context");
            return null;
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.viewer.BrowseElementInfo#getName()
     */
    @Override
    public String getName() {
        return collectionName;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.viewer.BrowseElementInfo#getTranslationsForName()
     */
    @Override
    public IMetadataValue getTranslationsForName() {
        return IMetadataValue.getTranslations(getName());
    }

}
