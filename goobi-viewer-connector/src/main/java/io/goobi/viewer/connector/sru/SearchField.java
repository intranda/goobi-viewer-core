/**
 * This file is part of the Goobi viewer Connector - OAI-PMH and SRU interfaces for digital objects.
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
package io.goobi.viewer.connector.sru;

import io.goobi.viewer.solr.SolrConstants;

/**
 * <p>SearchField class.</p>
 *
 */
public enum SearchField {

    ANYWHERE("any", null, "anywhere", "*", true, false, false),
    TITLE("title", "dc.title", "title", SolrConstants.TITLE, true, false, true),
    CREATOR("creator", "dc.creator", "per", SolrConstants.PERSON_ONEFIELD, true, false, false),
    COLLECTION("collection", "dc.subject", "collection", SolrConstants.DC, true, false, false),
    PUBLISHER("publisher", "dc.publisher", "publisher", SolrConstants.PUBLISHER, true, false, true),
    YEAR("year", "dc.date", "year", SolrConstants.YEARPUBLISH, true, false, true),
    DOCUMENTTYPE("documentType", "dc.type", "type", SolrConstants.DOCSTRCT, true, false, false),
    FORMAT("format", "dc.format", null, null, false, false, false),
    URN("urn", "dc.identifier", "urn", SolrConstants.URN, true, false, true),
    IDENTIFIER("identifier", "dc.identifier", "identifier", SolrConstants.PI, true, false, true);

    private String internalName;
    private String dcName;
    private String cqlName;
    private String solrName;
    private boolean seachable;
    private boolean scanable;
    private boolean sortable;

    /**
     * 
     * @param internalName
     * @param dcName
     * @param cqlName
     * @param solrName
     * @param seachable
     * @param scanable
     * @param sortable
     */
    private SearchField(String internalName, String dcName, String cqlName, String solrName, boolean seachable, boolean scanable, boolean sortable) {
        this.internalName = internalName;
        this.dcName = dcName;
        this.cqlName = cqlName;
        this.solrName = solrName;
        this.seachable = seachable;
        this.scanable = scanable;
        this.sortable = sortable;
    }

    /**
     * <p>Getter for the field <code>internalName</code>.</p>
     *
     * @return the internalName
     */
    public String getInternalName() {
        return internalName;
    }

    /**
     * <p>Getter for the field <code>dcName</code>.</p>
     *
     * @return the dcName
     */
    public String getDcName() {
        return dcName;
    }

    /**
     * <p>Getter for the field <code>cqlName</code>.</p>
     *
     * @return the cqlName
     */
    public String getCqlName() {
        return cqlName;
    }

    /**
     * <p>Getter for the field <code>solrName</code>.</p>
     *
     * @return the solrName
     */
    public String getSolrName() {
        return solrName;
    }

    /**
     * <p>isSeachable.</p>
     *
     * @return the seachable
     */
    public boolean isSeachable() {
        return seachable;
    }

    /**
     * <p>isScanable.</p>
     *
     * @return the scanable
     */
    public boolean isScanable() {
        return scanable;
    }

    /**
     * <p>isSortable.</p>
     *
     * @return the sortable
     */
    public boolean isSortable() {
        return sortable;
    }

    /**
     * <p>getFieldByDcName.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.connector.sru.SearchField} object.
     */
    public static SearchField getFieldByDcName(String name) {
        for (SearchField field : SearchField.values()) {
            if (field.getDcName() != null && field.getDcName().equalsIgnoreCase(name)) {
                return field;
            }
        }
        return null;
    }

    /**
     * <p>getFieldByCqlName.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.connector.sru.SearchField} object.
     */
    public static SearchField getFieldByCqlName(String name) {
        for (SearchField field : SearchField.values()) {
            if (field.getCqlName() != null && field.getCqlName().equalsIgnoreCase(name)) {
                return field;
            }
        }
        return null;
    }

    /**
     * <p>getFieldBySolrName.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.connector.sru.SearchField} object.
     */
    public static SearchField getFieldBySolrName(String name) {
        for (SearchField field : SearchField.values()) {
            if (field.getSolrName() != null && field.getSolrName().equalsIgnoreCase(name)) {
                return field;
            }
        }
        return null;
    }

    /**
     * <p>getFieldByInternalName.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.connector.sru.SearchField} object.
     */
    public static SearchField getFieldByInternalName(String name) {
        for (SearchField field : SearchField.values()) {
            if (field.getInternalName().equalsIgnoreCase(name)) {
                return field;
            }
        }
        return null;
    }

}
