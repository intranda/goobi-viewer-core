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
package de.intranda.digiverso.presentation.managedbeans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.StringTools;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.crowdsourcing.DisplayUserGeneratedContent;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;

/**
 * Supplies additional content for records (such contents produced by the crowdsourcing module).
 */
@Named
@SessionScoped
public class ContentBean implements Serializable {

    private static final long serialVersionUID = -2656584301309913161L;

    private static final Logger logger = LoggerFactory.getLogger(ContentBean.class);

    private String pi;
    /** Currently open page number. Used to make sure contents are reloaded if a new page is opened. */
    private int currentPage = -1;
    /** User generated contents to display on this page. */
    private List<DisplayUserGeneratedContent> userGeneratedContentsForDisplay;

    /** Empty Constructor. */
    public ContentBean() {
        // the emptiness inside
    }

    @PostConstruct
    public void init() {
    }

    /**
     * @return User-generated contents for the given page
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    public List<DisplayUserGeneratedContent> getUserGeneratedContentsForDisplay(PhysicalElement page)
            throws PresentationException, IndexUnreachableException {
        logger.trace("getUserGeneratedContentsForDisplay");
        if (page != null && (userGeneratedContentsForDisplay == null || page.getPi().equals(pi) || page.getOrder() != currentPage)) {
            loadUserGeneratedContentsForDisplay(page);
        }
        if (userGeneratedContentsForDisplay != null && userGeneratedContentsForDisplay.size() > 0) {
            return userGeneratedContentsForDisplay;
        }

        return null;
    }

    /**
     * 
     * @param page
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public void loadUserGeneratedContentsForDisplay(PhysicalElement page) throws PresentationException, IndexUnreachableException {
        logger.trace("loadUserGeneratedContentsForDisplay");
        if (page == null) {
            logger.debug("page is null, cannot load");
            return;
        }
        currentPage = page.getOrder();
        userGeneratedContentsForDisplay = new ArrayList<>();
        for (DisplayUserGeneratedContent ugcContent : DataManager.getInstance().getSearchIndex().getDisplayUserGeneratedContentsForPage(page.getPi(),
                page.getOrder())) {
            // Do not add empty comments
            if (ugcContent.isEmpty()) {
                userGeneratedContentsForDisplay.add(ugcContent);
                // getPageUserGeneratedContent(page).generateDisplayCoordinates(ugcContent);
            }
        }
        logger.debug("Loaded {} user generated contents for page {}", userGeneratedContentsForDisplay.size(), currentPage);
    }

    public List<List<String>> getCurrentUGCCoords(PhysicalElement page) throws IndexUnreachableException, PresentationException {
        List<DisplayUserGeneratedContent> currentContent;
        currentContent = getUserGeneratedContentsForDisplay(page);
        if (currentContent != null) {
            List<List<String>> coords = new ArrayList<>(currentContent.size());
            for (DisplayUserGeneratedContent content : currentContent) {
                if (content.hasArea()) {
                    String rect = content.getAreaString();
                    rect += (",'" + content.getLabel() + "'");
                    rect += (",'" + content.getId() + "'");
                    coords.add(Arrays.asList(rect.split(",")));
                }
            }
            logger.debug("getting ugc coordinates {}", coords);
            return coords;
        }

        return Collections.emptyList();
    }

    /**
     * Removes script tags from the given string.
     * 
     * @param value
     * @return value sans any script tags
     */
    public String cleanUpValue(String value) {
        return StringTools.stripJS(value);
    }
}