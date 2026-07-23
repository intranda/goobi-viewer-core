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
package io.goobi.viewer.model.search;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.translations.IPolyglott;

/**
 * Holds information about the record from which an advanced search was triggered (e.g. from the TOC view).
 *
 * <p>
 * Used to provide a back-link to the originating record on the search results page.
 */
public class AdvancedSearchOrigin {

    private final String pi;
    private final Long cmsPageId;
    private final String label;
    private final String docstrct;

    /**
     * @param pi Persistent identifier of the record
     * @param label Display label of the record
     * @param docstrct Document structure type (e.g. "Newspaper", "Periodical")
     * @should set pi label and docstrct from constructor arguments
     */
    public AdvancedSearchOrigin(String pi, String label, String docstrct) {
        this.pi = pi;
        this.cmsPageId = null;
        this.label = label;
        this.docstrct = docstrct;
    }

    /**
     * @param cmsPage the cms page originating the search
     * @should set cms page id and title from given page
     */
    public AdvancedSearchOrigin(CMSPage cmsPage) {
        this.pi = null;
        this.cmsPageId = cmsPage.getId();
        this.label = cmsPage.getTitle(IPolyglott.getCurrentLocale());
        this.docstrct = null;
    }

    public String getPi() {
        return pi;
    }

    public String getLabel() {
        return label;
    }

    public String getDocstrct() {
        return docstrct;
    }

    public Long getCmsPageId() {
        return cmsPageId;
    }

    /**
     * @should return true when pi is not null
     * @should return false when pi is blank
     * @should return false when cms page id is set
     */
    public boolean isRecordOrigin() {
        // Treat a blank pi as invalid: a blank pi cannot build a valid TOC back-link and would
        // otherwise pass a bare non-null check while producing a broken URL
        return StringUtils.isNotBlank(this.pi);
    }

    /**
     * @should return false when pi is not null
     * @should return true when cms page id is set
     */
    public boolean isCmsPageOrigin() {
        return this.cmsPageId != null;
    }

    /**
     * Returns whether this origin can produce a back-link URL, i.e. it points to either a record or a CMS page.
     *
     * @return true if either a record pi or a cms page id is set; false otherwise
     * @should return true for record origin
     * @should return true for cms page origin
     * @should return false when neither pi nor cms page id is set
     */
    public boolean isValid() {
        // Guards callers/views against an origin that has neither a record nor a CMS page target,
        // which would make getOriginUrl() throw during rendering
        return this.isRecordOrigin() || this.isCmsPageOrigin();
    }

    /**
     * @should return toc url for record origin
     * @should return cms page url for cms page origin
     * @should throw IllegalStateException when pi is null and no cms page id set
     */
    public String getOriginUrl() {
        if (this.isRecordOrigin()) {
            return PrettyUrlTools.getAbsolutePageUrl("toc2", this.getPi(), 1);
        } else if (this.isCmsPageOrigin()) {
            return PrettyUrlTools.getAbsolutePageUrl("cmsOpenPage1", this.getCmsPageId());
        } else {
            throw new IllegalStateException("Not valid search origin defined");
        }
    }

}
