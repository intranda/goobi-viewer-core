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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * <p>
 * Channel class.
 * </p>
 *
 * @author Florian Alpers
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Channel {

    String title;
    String link;
    String description;
    String language;
    String copyright;
    Date pubDate;

    List<RssItem> items = new ArrayList<>();

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
    public String getDescription() {
        return description;
    }

    /**
     * <p>
     * Setter for the field <code>description</code>.
     * </p>
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * <p>
     * Getter for the field <code>language</code>.
     * </p>
     *
     * @return the language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * <p>
     * Setter for the field <code>language</code>.
     * </p>
     *
     * @param language the language to set
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * <p>
     * Getter for the field <code>copyright</code>.
     * </p>
     *
     * @return the copyright
     */
    public String getCopyright() {
        return copyright;
    }

    /**
     * <p>
     * Setter for the field <code>copyright</code>.
     * </p>
     *
     * @param copyright the copyright to set
     */
    public void setCopyright(String copyright) {
        this.copyright = copyright;
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
     * Getter for the field <code>items</code>.
     * </p>
     *
     * @return the items
     */
    public List<RssItem> getItems() {
        return items;
    }

    /**
     * <p>
     * addItem.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.rss.RssItem} object.
     */
    public void addItem(RssItem item) {
        this.items.add(item);
    }

}
