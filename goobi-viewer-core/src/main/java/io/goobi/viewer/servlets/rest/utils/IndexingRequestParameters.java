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
package io.goobi.viewer.servlets.rest.utils;

/**
 * POST request parameters for IndexingResource.
 */
public class IndexingRequestParameters {

    private String pi;
    /** If true, a trace document will be added to the index. */
    private boolean createTraceDocument = false;

    /**
     * <p>
     * Getter for the field <code>pi</code>.
     * </p>
     *
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * <p>
     * Setter for the field <code>pi</code>.
     * </p>
     *
     * @param pi the pi to set
     */
    public void setPi(String pi) {
        this.pi = pi;
    }

    /**
     * <p>
     * isCreateTraceDocument.
     * </p>
     *
     * @return the createTraceDocument
     */
    public boolean isCreateTraceDocument() {
        return createTraceDocument;
    }

    /**
     * <p>
     * Setter for the field <code>createTraceDocument</code>.
     * </p>
     *
     * @param createTraceDocument the createTraceDocument to set
     */
    public void setCreateTraceDocument(boolean createTraceDocument) {
        this.createTraceDocument = createTraceDocument;
    }
}
