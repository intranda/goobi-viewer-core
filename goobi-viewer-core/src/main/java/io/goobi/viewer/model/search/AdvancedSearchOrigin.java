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

/**
 * Holds information about the record from which an advanced search was triggered (e.g. from the TOC view).
 *
 * <p>Used to provide a back-link to the originating record on the search results page.
 */
public class AdvancedSearchOrigin {

    private final String pi;
    private final String label;
    private final String docstrct;

    /**
     * @param pi Persistent identifier of the record
     * @param label Display label of the record
     * @param docstrct Document structure type (e.g. "Newspaper", "Periodical")
     */
    public AdvancedSearchOrigin(String pi, String label, String docstrct) {
        this.pi = pi;
        this.label = label;
        this.docstrct = docstrct;
    }

    /**
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return the docstrct
     */
    public String getDocstrct() {
        return docstrct;
    }
}
