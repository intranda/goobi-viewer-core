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
package io.goobi.viewer.model.glossary;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * <p>
 * Glossary class.
 * </p>
 *
 * @author Florian Alpers
 */
@JsonInclude(Include.NON_NULL)
public class Glossary {

    private final String title;
    private final String filename;
    private final String description;
    private List<GlossaryRecord> records;

    /**
     * <p>
     * Constructor for Glossary.
     * </p>
     *
     * @param title a {@link java.lang.String} object.
     * @param filename a {@link java.lang.String} object.
     * @param description a {@link java.lang.String} object.
     */
    public Glossary(String title, String filename, String description) {
        super();
        this.title = title;
        this.filename = filename;
        this.description = description;
    }

    /**
     * <p>
     * Getter for the field <code>title</code>.
     * </p>
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * <p>
     * Getter for the field <code>filename</code>.
     * </p>
     *
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * <p>
     * Getter for the field <code>description</code>.
     * </p>
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * <p>
     * Getter for the field <code>records</code>.
     * </p>
     *
     * @return the records
     */
    public List<GlossaryRecord> getRecords() {
        return records;
    }

    /**
     * <p>
     * Setter for the field <code>records</code>.
     * </p>
     *
     * @param records the records to set
     */
    public void setRecords(List<GlossaryRecord> records) {
        this.records = records;
    }
}
