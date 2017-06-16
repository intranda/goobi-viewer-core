/**
 * This file is part of the Goobi Viewer - a content presentation and management application for digitized objects.
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
package de.intranda.digiverso.presentation.model.bookshelf;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.viewer.PageType;

@Entity
@Table(name = "bookshelf_items")
public class BookshelfItem implements Serializable {

    private static final long serialVersionUID = 9047168382986927374L;

    private static final Logger logger = LoggerFactory.getLogger(BookshelfItem.class);

    private static final String[] FIELDS = { SolrConstants.THUMBNAIL, SolrConstants.DATAREPOSITORY };

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookshelf_item_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bookshelf_id", nullable = false)
    private Bookshelf bookshelf;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "pi")
    private String pi;

    @Column(name = "logid")
    private String logId;

    @Column(name = "urn")
    private String urn;

    @Column(name = "main_title")
    private String mainTitle;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_added")
    private Date dateAdded;

    /** Empty constructor. */
    public BookshelfItem() {
        // the emptiness inside
    }

    public BookshelfItem(String pi, String mainTitle, String name) {
        this.pi = pi;
        this.mainTitle = mainTitle;
        this.name = name;
        this.dateAdded = new Date();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        BookshelfItem other = (BookshelfItem) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    /**
     * Constructs the image URL for this bookshelf item.
     *
     * @return The URL as string.
     */
    public String getUrl() {
        StringBuilder url = new StringBuilder();
        url.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());

        if (StringUtils.isNotEmpty(urn)) {
            url.append("/resolver?identifier=").append(urn);
        } else {
            url.append('/').append(PageType.viewMetadata.getName()).append('/').append(pi).append("/1/");
            if (StringUtils.isNotEmpty(logId)) {
                url.append(logId).append('/');
            }
        }

        // logger.debug("URL: " + url.toString());
        return url.toString();
    }

    /**
     * Returns the URL to the representative image thumbnail for the record represented by this item.
     *
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public String getRepresentativeImageUrl() throws PresentationException, IndexUnreachableException {
        int width = 90;
        int height = 120;

        SolrDocumentList docs = DataManager.getInstance().getSearchIndex().search(new StringBuilder(SolrConstants.PI).append(':').append(pi).toString(), 1, null,
                Arrays.asList(FIELDS));
        if (!docs.isEmpty()) {
            String fileName = (String) docs.get(0).getFieldValue(SolrConstants.THUMBNAIL);
            String dataRepository = (String) docs.get(0).getFieldValue(SolrConstants.DATAREPOSITORY);
            String url = Helper.getImageUrl(pi, fileName, dataRepository, width, height, 0, true, true);
            logger.trace("Bookshelf item thumbnail URL: {}", url);
            return url;
        }

        return null;
    }

    /*********************************** Getter and Setter ***************************************/

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the bookshelf
     */
    public Bookshelf getBookshelf() {
        return bookshelf;
    }

    /**
     * @param bookshelf the bookshelf to set
     */
    public void setBookshelf(Bookshelf bookshelf) {
        this.bookshelf = bookshelf;
    }

    /**
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * @param pi the pi to set
     */
    public void setPi(String pi) {
        this.pi = pi;
    }

    /**
     * @return the logId
     */
    public String getLogId() {
        return logId;
    }

    /**
     * @param logId the logId to set
     */
    public void setLogId(String logId) {
        this.logId = logId;
    }

    /**
     * @return the urn
     */
    public String getUrn() {
        return urn;
    }

    /**
     * @param urn the urn to set
     */
    public void setUrn(String urn) {
        this.urn = urn;
    }

    /**
     * @return the mainTitle
     */
    public String getMainTitle() {
        return mainTitle;
    }

    /**
     * @param mainTitle the mainTitle to set
     */
    public void setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
    }

    public String getMainTitleUnescaped() {
        return StringEscapeUtils.unescapeHtml(mainTitle);
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the dateAdded
     */
    public Date getDateAdded() {
        return dateAdded;
    }

    /**
     * @param dateAdded the dateAdded to set
     */
    public void setDateAdded(Date dateAdded) {
        this.dateAdded = dateAdded;
    }
}
