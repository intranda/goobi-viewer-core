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
package io.goobi.viewer.connector.oai.enums;

/**
 * <p>
 * Verb class.
 * </p>
 *
 */
public enum Verb {

    GETRECORD("GetRecord"),
    IDENTIFY("Identify"),
    LISTIDENTIFIERS("ListIdentifiers"),
    LISTMETADATAFORMATS("ListMetadataFormats"),
    LISTRECORDS("ListRecords"),
    LISTSETS("ListSets");

    private String title;

    private Verb(String title) {
        this.title = title;
    }

    /**
     * <p>
     * Getter for the field <code>title</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getTitle() {
        return title;
    }

    /**
     * <p>
     * getByTitle.
     * </p>
     *
     * @param title a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.connector.oai.enums.Verb} object.
     */
    public static Verb getByTitle(String title) {
        for (Verb v : Verb.values()) {
            if (v.getTitle().equals(title)) {
                return v;
            }
        }
        return null;
    }

}
