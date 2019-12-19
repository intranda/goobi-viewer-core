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
package io.goobi.viewer.model.bookmark;

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
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StructElement;

@Entity
@Table(name = "bookshelf_items")
public class Bookmark implements Serializable {

    private static final long serialVersionUID = 9047168382986927374L;

    private static final Logger logger = LoggerFactory.getLogger(Bookmark.class);

    private static final String[] FIELDS =
            { SolrConstants.THUMBNAIL, SolrConstants.DATAREPOSITORY, SolrConstants.MIMETYPE, SolrConstants.IDDOC, SolrConstants.PI };

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookshelf_item_id")
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "bookshelf_id", nullable = false)
    private BookmarkList bookmarkList;

    @Column(name = "name", columnDefinition = "LONGTEXT")
    private String name;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "pi")
    private String pi;

    @Column(name = "logid")
    private String logId;

    @Column(name = "page_order")
    private Integer order;

    @Column(name = "urn")
    private String urn;

    @Deprecated
    @Column(name = "main_title")
    private String mainTitle = null;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_added")
    private Date dateAdded;

    /** Empty constructor. */
    public Bookmark() {
        // the emptiness inside
    }

    public Bookmark(String pi, String mainTitle, String name) {
        this.pi = pi;
        this.name = name;
        this.dateAdded = new Date();
    }

    /**
     * Creates a new bookmark based in book pi, section logId and page order logId and order my be empty or null, if only the book itself is
     * references. PI must be non-empty, otherwise a NullPointerException is thrown The item name will be inferred from the book/section title from
     * Solr. If that fails, an IndexUnreachableException or PresentationException is thrown
     * 
     * @param pi
     * @param logId
     * @param order
     * @throws IndexUnreachableException if the Solr index could not be reached
     * @throws PresentationException if the pi/logId could not be resolved
     * @throws NullPointerException if pi is null or blank
     */
    public Bookmark(String pi, String logId, Integer order) throws IndexUnreachableException, PresentationException {
        this.pi = pi;
        this.logId = logId;
        this.order = order;
        this.name = getDocumentTitle();
        this.dateAdded = new Date();
    }

    /**
     * Creates a new bookmark based in book pi, section logId and page order logId and order my be empty or null, if only the book itself is
     * references. PI must be non-empty, otherwise a NullPointerException is thrown The item name will be inferred from the book/section title from
     * Solr. If that fails, an IndexUnreachableException or PresentationException is thrown
     * 
     * @param pi
     * @param logId
     * @param order
     * @param ignoreMissingSolrDoc should be false, unless arbitrary pi/logid values should be allowed (e.g. for testing)
     * @throws IndexUnreachableException if the Solr index could not be reached
     * @throws PresentationException if the pi/logId could not be resolved
     * @throws NullPointerException if pi is null or blank
     */
    public Bookmark(String pi, String logId, Integer order, boolean ignoreMissingSolrDoc) throws IndexUnreachableException, PresentationException {
        this.pi = pi;
        this.logId = logId;
        this.order = order;
        try {
            this.name = getDocumentTitle();
        } catch (SolrException | IndexUnreachableException | PresentationException e) {
            if (ignoreMissingSolrDoc) {
                this.name = "";
            } else {
                throw e;
            }
        }
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
        Bookmark other = (Bookmark) obj;
        return bothEqualOrBlank(this.pi, other.pi) && bothEqualOrBlank(this.logId, other.logId) && bothEqualOrNull(this.order, other.order);
    }

    /**
     * @param other
     * @return
     */
    public boolean bothEqualOrNull(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        }
        return o1.equals(o2);
    }

    /**
     * @param other
     * @return
     */
    public boolean bothEqualOrBlank(String o1, String o2) {
        if (StringUtils.isBlank(o1)) {
            return StringUtils.isBlank(o2);
        } else if (StringUtils.isBlank(o2)) {
            return false;
        } else {
            return o1.trim().equals(o2.trim());
        }
    }

    /**
     * Constructs the image view URL for this bookmark.
     *
     * @return The URL as string.
     */
    public String getUrl() {
        StringBuilder url = new StringBuilder();
        url.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());

        if (StringUtils.isNotEmpty(urn)) {
            url.append("/resolver?identifier=").append(urn);
        } else {
            url.append('/')
                    .append(DataManager.getInstance()
                            .getUrlBuilder()
                            .buildPageUrl(pi, order != null ? Integer.valueOf(order) : 1, logId,
                                    order != null ? PageType.viewObject : PageType.viewMetadata));
        }

        // logger.debug("URL: {}", url.toString());
        return url.toString();
    }

    /**
     * Returns the URL to the representative image thumbnail for the record represented by this item.
     *
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ViewerConfigurationException
     * @throws DAOException
     */
    public String getRepresentativeImageUrl() throws PresentationException, IndexUnreachableException, ViewerConfigurationException, DAOException {
        int width = 90;
        int height = 120;
        return getRepresentativeImageUrl(width, height);
    }

    /**
     * Returns the URL to the representative image thumbnail for the record represented by this item.
     *
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ViewerConfigurationException
     * @throws DAOException
     */
    public String getRepresentativeImageUrl(int width, int height)
            throws PresentationException, IndexUnreachableException, ViewerConfigurationException, DAOException {
        String query;
        if (order != null) {
            // Exactly the bookmarked page
            query = new StringBuilder("+").append(SolrConstants.PI_TOPSTRUCT)
                    .append(':')
                    .append(pi)
                    .append(" +")
                    .append(SolrConstants.ORDER)
                    .append(':')
                    .append(order)
                    .append(" +")
                    .append(SolrConstants.DOCTYPE)
                    .append(":PAGE")
                    .toString();
        } else {
            // Representative image
            query = new StringBuilder(SolrConstants.PI).append(':').append(pi).toString();
        }

        SolrDocumentList docs = DataManager.getInstance().getSearchIndex().search(query, 1, null, Arrays.asList(FIELDS));
        if (!docs.isEmpty()) {
            String luceneId = (String) docs.get(0).getFieldValue(SolrConstants.IDDOC);
            ThumbnailHandler thumbs = BeanUtils.getImageDeliveryBean().getThumbs();
            if (order != null) {
                logger.trace("url: " + thumbs.getThumbnailUrl(docs.get(0), width, height));
                return thumbs.getThumbnailUrl(order, pi, width, height);
            }
            return thumbs.getThumbnailUrl(docs.get(0), width, height);
        }

        return "";
    }

    /**
     * Retrieves the documents title from the Solr index using the stored pi and - if nonempty - the logId
     * 
     * @return
     * @throws IndexUnreachableException if the Solr index could not be reached
     * @throws PresentationException if the pi/logId could not be resolved
     */
    @JsonIgnore
    public String getDocumentTitle() throws IndexUnreachableException, PresentationException {
        logger.trace("getDocumentTitle: {}/{}", pi, logId);
        Long iddoc = null;
        if (StringUtils.isNotBlank(logId)) {
            iddoc = DataManager.getInstance().getSearchIndex().getIddocByLogid(pi, logId);
        } else if (StringUtils.isNotBlank(pi)) {
            iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier(pi);
        }

        String title = "";
        if (iddoc != null) {
            SolrDocument doc = DataManager.getInstance().getSearchIndex().getDocumentByIddoc(iddoc.toString());
            if (doc != null) {
                StructElement se = new StructElement(iddoc, doc);
                title = se.getDisplayLabel();
                //                title = SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.TITLE);
                return title;
            }
            throw new PresentationException("No document found with iddoc = " + iddoc);
        }
        throw new PresentationException("No iddoc found for pi = " + pi + " and logId = " + logId);
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
     * @return the bookmarkList
     */
    public BookmarkList getBookmarkList() {
        return bookmarkList;
    }

    /**
     * @param bookmarkList the bookmarkList to set
     */
    public void setBookmarkList(BookmarkList bookmarkList) {
        this.bookmarkList = bookmarkList;
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

    @JsonIgnore
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

    /**
     * @return the order
     */
    public Integer getOrder() {
        return order;
    }

    /**
     * @param order the order to set
     */
    public void setOrder(Integer order) {
        this.order = order;
    }

    /**
     * @return the mainTitle
     */
    @Deprecated
    public String getMainTitle() {
        return mainTitle;
    }

    /**
     * @param mainTitle the mainTitle to set
     */
    @Deprecated
    public void setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
    }

}
