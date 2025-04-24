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
package io.goobi.viewer.model.citation;

import java.util.List;
import java.util.Objects;

import io.goobi.viewer.model.citation.CitationLink.CitationLinkLevel;
import io.goobi.viewer.model.viewer.ViewManager;

/**
 * Contains a list of {@link CitationLink}s along with a viewManager state wich tracks current page number and logId The method
 * {@link #isCurrent(ViewManager)} checks whether this list reflects to same state as the given {@link ViewManager} regarding the
 * {@link CitationLinkLevel level} of the included citation links
 */
public class CitationList {

    private final CitationLinkLevel level;
    private final List<CitationLink> list;
    private final String logId;
    private final int page;

    /**
     * Constructor creating state from a {@link ViewManager}
     * 
     * @param list the list of citation links
     * @param level the level this list pertains to
     * @param viewManager the viewmanager providing the state
     */
    public CitationList(List<CitationLink> list, CitationLinkLevel level, ViewManager viewManager) {
        this(list, level, viewManager.getLogId(), viewManager.getCurrentImageOrder());
    }

    /**
     * Constructor creating state from individual properties
     * 
     * @param list the list of citation links
     * @param level the level this list pertains to
     * @param logId the logId for which this is was created
     * @param page the page for which this list was created
     */
    public CitationList(List<CitationLink> list, CitationLinkLevel level, String logId, int page) {
        this.list = list;
        this.level = level;
        this.logId = logId;
        this.page = page;
    }

    /**
     * get the actual list of citation links
     * 
     * @return a list of {@link CitationLink}s
     */
    public List<CitationLink> getList() {
        return list;
    }

    /**
     * Check whether the state of this list reflects the same state as the given ViewManager regarding the level for which the list was created
     * 
     * @param viewManager
     * @return true if the state of this list reflects the same state as the given ViewManager regarding the level for which the list was created;
     *         false otherwise
     */
    public boolean isCurrent(ViewManager viewManager) {
        switch (level) {
            case RECORD:
                return true;
            case DOCSTRUCT:
                return Objects.equals(viewManager.getLogId(), this.logId);
            case IMAGE:
                return Objects.equals(viewManager.getCurrentImageOrder(), this.page);
        }
        return true;
    }

}
