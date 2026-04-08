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
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.goobi.viewer.model.security.AccessDeniedInfoConfig;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.security.IAccessDeniedThumbnailOutput;

/**
 * Represents a single object within an RSS feed.
 *
 * @author Florian Alpers
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RssItem implements Comparable<RssItem>, IAccessDeniedThumbnailOutput {

    private String title;
    private String link;
    private Description description;
    private Date pubDate;
    private String creator;
    private String docType;

    @JsonIgnore
    private AccessPermission accessPermissionThumbnail = null;

    @Override
    public String getAccessDeniedThumbnailUrl(Locale locale) {
        if (accessPermissionThumbnail != null && accessPermissionThumbnail.getAccessDeniedPlaceholderInfo() != null) {
            AccessDeniedInfoConfig placeholderInfo = accessPermissionThumbnail.getAccessDeniedPlaceholderInfo().get(locale.getLanguage());
            if (placeholderInfo != null && StringUtils.isNotEmpty(placeholderInfo.getImageUri())) {
                return placeholderInfo.getImageUri();
            }
        }

        return null;
    }

    /**
     * Getter for the field <code>title</code>.
     *

     */
    public String getTitle() {
        return title;
    }

    /**
     * Setter for the field <code>title</code>.
     *
     * @param title the title of this RSS item
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Getter for the field <code>link</code>.
     *

     */
    public String getLink() {
        return link;
    }

    /**
     * Setter for the field <code>link</code>.
     *
     * @param link the URL of the record or resource represented by this RSS item
     */
    public void setLink(String link) {
        this.link = link;
    }

    /**
     * Getter for the field <code>description</code>.
     *

     */
    public Description getDescription() {
        return description;
    }

    /**
     * Setter for the field <code>description</code>.
     *
     * @param description the structured description object containing image and text for this RSS item
     */
    public void setDescription(Description description) {
        this.description = description;
    }

    /**
     * Getter for the field <code>pubDate</code>.
     *

     */
    public Date getPubDate() {
        return pubDate;
    }

    /**
     * Setter for the field <code>pubDate</code>.
     *
     * @param pubDate the publication date of this RSS item, used for sorting
     */
    public void setPubDate(Date pubDate) {
        this.pubDate = pubDate;
    }

    /**
     * Getter for the field <code>creator</code>.
     *

     */
    public String getCreator() {
        return creator;
    }

    /**
     * Setter for the field <code>creator</code>.
     *
     * @param creator the Dublin Core creator (author or responsible party) of this RSS item
     */
    public void setCreator(String creator) {
        this.creator = creator;
    }

    /**
     * Setter for the field <code>docType</code>.
     *
     * @param docType the Solr document type of the record represented by this RSS item (e.g. "monograph", "periodical")
     */
    public void setDocType(String docType) {
        this.docType = docType;
    }

    /**
     * Getter for the field <code>docType</code>.
     *

     */
    public String getDocType() {
        return docType;
    }

    
    public void setAccessPermissionThumbnail(AccessPermission accessPermissionThumbnail) {
        this.accessPermissionThumbnail = accessPermissionThumbnail;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sorts the items accoring to their publication date
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
