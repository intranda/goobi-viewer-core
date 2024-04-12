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
package io.goobi.viewer.modules.interfaces;

import io.goobi.viewer.model.search.BrowseElement;
import io.goobi.viewer.model.viewer.PageType;

/**
 * Interface for creating module-specific urls for viewer pages
 *
 * @author Florian Alpers
 */
public interface IURLBuilder {

    /**
     * <p>
     * generateURL.
     * </p>
     *
     * @param browseElement The browseElement for which we want to build a url
     * @return The url the the given BrowseElement should link to
     */
    public String generateURL(BrowseElement browseElement);

    /**
     * <p>
     * buildPageUrl.
     * </p>
     *
     * @param pi The record persistent identifier
     * @param imageNo the page number (1-based)
     * @param logId The METS identifier of the logical struct element
     * @param pageType the type of viewer page the url should open
     * @param topStruct if false, the url should point to a page or struct element within the record, so imageNo and logId must be considered
     * @return A URL to the object in the view given by pageType
     */
    public String buildPageUrl(String pi, int imageNo, String logId, PageType pageType, boolean topStruct);

    /**
     * 
     * @param pi
     * @param imageNo
     * @param logId
     * @param pageType
     * @return Generated URL
     */
    public default String buildPageUrl(String pi, int imageNo, String logId, PageType pageType) {
        return buildPageUrl(pi, imageNo, logId, pageType, false);
    }
}
