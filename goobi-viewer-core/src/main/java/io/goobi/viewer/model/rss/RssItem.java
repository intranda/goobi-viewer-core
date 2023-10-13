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
package io.goobi.viewer.model.rss;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents a single object within an RSS feed
 *
 * @author Florian Alpers
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RssItem implements Comparable<RssItem> {

    String title;
    String link;
    Description description;
    Date pubDate;
    String creator;
    String docType;

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
     * Setter for the field <code>title</code>.
     * </p>
     *
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * <p>
     * Getter for the field <code>link</code>.
     * </p>
     *
     * @return the link
     */
    public String getLink() {
        return link;
    }

    /**
     * <p>
     * Setter for the field <code>link</code>.
     * </p>
     *
     * @param link the link to set
     */
    public void setLink(String link) {
        this.link = link;
    }

    /**
     * <p>
     * Getter for the field <code>description</code>.
     * </p>
     *
     * @return the description
     */
    public Description getDescription() {
        return description;
    }

    /**
     * <p>
     * Setter for the field <code>description</code>.
     * </p>
     *
     * @param description the description to set
     */
    public void setDescription(Description description) {
        this.description = description;
    }

    /**
     * <p>
     * Getter for the field <code>pubDate</code>.
     * </p>
     *
     * @return the pubDate
     */
    public Date getPubDate() {
        return pubDate;
    }

    /**
     * <p>
     * Setter for the field <code>pubDate</code>.
     * </p>
     *
     * @param pubDate the pubDate to set
     */
    public void setPubDate(Date pubDate) {
        this.pubDate = pubDate;
    }

    /**
     * <p>
     * Getter for the field <code>creator</code>.
     * </p>
     *
     * @return the creator
     */
    public String getCreator() {
        return creator;
    }

    /**
     * <p>
     * Setter for the field <code>creator</code>.
     * </p>
     *
     * @param creator the creator to set
     */
    public void setCreator(String creator) {
        this.creator = creator;
    }

    /**
     * <p>
     * Setter for the field <code>docType</code>.
     * </p>
     *
     * @param docType the docType to set
     */
    public void setDocType(String docType) {
        this.docType = docType;
    }

    /**
     * <p>
     * Getter for the field <code>docType</code>.
     * </p>
     *
     * @return the docType
     */
    public String getDocType() {
        return docType;
    }

    /**
     * {@inheritDoc}
     *
     * Sorts the items accoring to their publication date
     */
    @Override
    public int compareTo(RssItem other) {
        if (getPubDate() != null && other.getPubDate() != null) {
            return other.getPubDate().compareTo(getPubDate());
        } else if (getPubDate() != null) {
            return -1;
        } else {
            return 1;
        }
    }

}
