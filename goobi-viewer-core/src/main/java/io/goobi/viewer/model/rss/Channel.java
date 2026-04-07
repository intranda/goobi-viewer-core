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
 * Channel class.
 *
 * @author Florian Alpers
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Channel {

    private String title;
    private String link;
    private String description;
    private String language;
    private String copyright;
    private Date pubDate;

    private List<RssItem> items = new ArrayList<>();

    /**
     * Getter for the field <code>title</code>.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Setter for the field <code>title</code>.
     *
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Getter for the field <code>link</code>.
     *
     * @return the link
     */
    public String getLink() {
        return link;
    }

    /**
     * Setter for the field <code>link</code>.
     *
     * @param link the link to set
     */
    public void setLink(String link) {
        this.link = link;
    }

    /**
     * Getter for the field <code>description</code>.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Setter for the field <code>description</code>.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Getter for the field <code>language</code>.
     *
     * @return the language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Setter for the field <code>language</code>.
     *
     * @param language the language to set
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Getter for the field <code>copyright</code>.
     *
     * @return the copyright
     */
    public String getCopyright() {
        return copyright;
    }

    /**
     * Setter for the field <code>copyright</code>.
     *
     * @param copyright the copyright to set
     */
    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    /**
     * Getter for the field <code>pubDate</code>.
     *
     * @return the pubDate
     */
    public Date getPubDate() {
        return pubDate;
    }

    /**
     * Setter for the field <code>pubDate</code>.
     *
     * @param pubDate the pubDate to set
     */
    public void setPubDate(Date pubDate) {
        this.pubDate = pubDate;
    }

    /**
     * Getter for the field <code>items</code>.
     *
     * @return the items
     */
    public List<RssItem> getItems() {
        return items;
    }

    /**
     * addItem.
     *
     * @param item a {@link io.goobi.viewer.model.rss.RssItem} object.
     */
    public void addItem(RssItem item) {
        this.items.add(item);
    }

}
