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
package io.goobi.viewer.model.bookmark;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.metadata.MetadataElement;
import io.goobi.viewer.model.search.BrowseElement;
import io.goobi.viewer.model.search.SearchHit;
import io.goobi.viewer.model.search.SearchHitFactory;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * <p>
 * Bookmark class.
 * </p>
 */
@Entity
@Table(name = "bookshelf_items")
@JsonInclude(Include.NON_NULL)
public class Bookmark implements Serializable {

    private static final long serialVersionUID = 9047168382986927374L;

    private static final Logger logger = LogManager.getLogger(Bookmark.class);

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

    @Column(name = "date_added")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime dateAdded;

    @Transient
    private String url;

    @Transient
    private BrowseElement browseElement = null;

    @Transient
    private Boolean hasImages = null;

    /**
     * Empty constructor.
     */
    public Bookmark() {
        // the emptiness inside
    }

    /**
     * <p>
     * Constructor for Bookmark.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param mainTitle a {@link java.lang.String} object.
     * @param name a {@link java.lang.String} object.
     */
    public Bookmark(String pi, String mainTitle, String name) {
        this.pi = pi;
        this.name = name;
        this.dateAdded = LocalDateTime.now();
    }

    /**
     * Creates a new bookmark based in book pi, section logId and page order logId and order my be empty or null, if only the book itself is
     * references. PI must be non-empty, otherwise a NullPointerException is thrown The item name will be inferred from the book/section title from
     * Solr. If that fails, an IndexUnreachableException or PresentationException is thrown
     *
     * @param pi a {@link java.lang.String} object.
     * @param logId a {@link java.lang.String} object.
     * @param order a {@link java.lang.Integer} object.
     * @throws java.lang.NullPointerException if pi is null or blank
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public Bookmark(String pi, String logId, Integer order) throws IndexUnreachableException, PresentationException {
        this.pi = pi;
        this.logId = logId;
        this.order = order;
        this.name = getDocumentTitle();
        this.dateAdded = LocalDateTime.now();
    }

    /**
     * Creates a new bookmark based in book pi, section logId and page order logId and order my be empty or null, if only the book itself is
     * references. PI must be non-empty, otherwise a NullPointerException is thrown The item name will be inferred from the book/section title from
     * Solr. If that fails, an IndexUnreachableException or PresentationException is thrown
     *
     * @param pi a {@link java.lang.String} object.
     * @param logId a {@link java.lang.String} object.
     * @param order a {@link java.lang.Integer} object.
     * @param ignoreMissingSolrDoc should be false, unless arbitrary pi/logid values should be allowed (e.g. for testing)
     * @throws java.lang.NullPointerException if pi is null or blank
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
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
        this.dateAdded = LocalDateTime.now();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    /** {@inheritDoc} */
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
    /** {@inheritDoc} */
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
     * <p>
     * bothEqualOrNull.
     * </p>
     *
     * @param o1 a {@link java.lang.Object} object.
     * @param o2 a {@link java.lang.Object} object.
     * @return a boolean.
     */
    public boolean bothEqualOrNull(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        }
        return o1.equals(o2);
    }

    /**
     * <p>
     * bothEqualOrBlank.
     * </p>
     *
     * @param o1 a {@link java.lang.String} object.
     * @param o2 a {@link java.lang.String} object.
     * @return a boolean.
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
     * Returns the image view URL for this bookmark. If this is the first call, the url is constructed first.
     *
     * @return The URL as string.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getUrl() throws PresentationException, IndexUnreachableException {
        if (url != null) {
            return url;
        }

        StringBuilder sb = new StringBuilder();
        //        sb.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());

        if (StringUtils.isNotEmpty(urn)) {
            sb.append("/resolver?identifier=").append(urn);
        } else {
            sb.append("/piresolver?id=").append(pi);
            if (order != null) {
                sb.append("&page=").append(order);
            }
        }
        url = sb.toString();

        // logger.debug("URL: {}", url);
        return url;
    }

    /**
     * Returns the URL to the representative image thumbnail for the record represented by this item.
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getRepresentativeImageUrl() throws PresentationException, IndexUnreachableException, ViewerConfigurationException, DAOException {
        int width = 90;
        int height = 120;
        return getRepresentativeImageUrl(width, height);
    }

    /**
     * Returns the URL to the representative image thumbnail for the record represented by this item.
     *
     * @param width a int.
     * @param height a int.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getRepresentativeImageUrl(int width, int height)
            throws PresentationException, IndexUnreachableException, ViewerConfigurationException, DAOException {
        ThumbnailHandler thumbs = BeanUtils.getImageDeliveryBean().getThumbs();
        if (order != null) {
            return thumbs.getThumbnailUrl(order, pi, width, height);
        }

        return thumbs.getThumbnailUrl(pi, width, height);
    }

    /**
     * Retrieves the documents title from the Solr index using the stored pi and - if nonempty - the logId
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
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
     * <p>
     * Getter for the field <code>id</code>.
     * </p>
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * <p>
     * Setter for the field <code>id</code>.
     * </p>
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * <p>
     * Getter for the field <code>bookmarkList</code>.
     * </p>
     *
     * @return the bookmarkList
     */
    public BookmarkList getBookmarkList() {
        return bookmarkList;
    }

    /**
     * <p>
     * Setter for the field <code>bookmarkList</code>.
     * </p>
     *
     * @param bookmarkList the bookmarkList to set
     */
    public void setBookmarkList(BookmarkList bookmarkList) {
        this.bookmarkList = bookmarkList;
    }

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
     * Getter for the field <code>logId</code>.
     * </p>
     *
     * @return the logId
     */
    public String getLogId() {
        return logId;
    }

    /**
     * <p>
     * Setter for the field <code>logId</code>.
     * </p>
     *
     * @param logId the logId to set
     */
    public void setLogId(String logId) {
        this.logId = logId;
    }

    /**
     * <p>
     * Getter for the field <code>urn</code>.
     * </p>
     *
     * @return the urn
     */
    public String getUrn() {
        return urn;
    }

    /**
     * <p>
     * Setter for the field <code>urn</code>.
     * </p>
     *
     * @param urn the urn to set
     */
    public void setUrn(String urn) {
        this.urn = urn;
    }

    /**
     * <p>
     * getMainTitleUnescaped.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    @JsonIgnore
    public String getMainTitleUnescaped() {
        return StringEscapeUtils.unescapeHtml4(mainTitle);
    }

    /**
     * <p>
     * Getter for the field <code>name</code>.
     * </p>
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Setter for the field <code>name</code>.
     * </p>
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
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
     * Getter for the field <code>dateAdded</code>.
     * </p>
     *
     * @return the dateAdded
     */
    public LocalDateTime getDateAdded() {
        return dateAdded;
    }

    /**
     * <p>
     * Setter for the field <code>dateAdded</code>.
     * </p>
     *
     * @param dateAdded the dateAdded to set
     */
    public void setDateAdded(LocalDateTime dateAdded) {
        this.dateAdded = dateAdded;
    }

    /**
     * <p>
     * Getter for the field <code>order</code>.
     * </p>
     *
     * @return the order
     */
    public Integer getOrder() {
        return order;
    }

    /**
     * <p>
     * Setter for the field <code>order</code>.
     * </p>
     *
     * @param order the order to set
     */
    public void setOrder(Integer order) {
        this.order = order;
    }

    /**
     * <p>
     * Getter for the field <code>mainTitle</code>.
     * </p>
     *
     * @return the mainTitle
     */
    @Deprecated
    public String getMainTitle() {
        return mainTitle;
    }

    /**
     * <p>
     * Setter for the field <code>mainTitle</code>.
     * </p>
     *
     * @param mainTitle the mainTitle to set
     */
    @Deprecated
    public void setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
    }

    @JsonIgnore
    public MetadataElement getMetadataElement() throws IndexUnreachableException {
        SolrDocument doc = retrieveSolrDocument();
        Long iddoc = Long.parseLong((String) doc.getFirstValue(SolrConstants.IDDOC));
        StructElement se = new StructElement(iddoc, doc);
        Locale sessionLocale = BeanUtils.getLocale();
        String selectedRecordLanguage = sessionLocale.getLanguage();
        try {
            return new MetadataElement().init(se, 0, sessionLocale).setSelectedRecordLanguage(selectedRecordLanguage);
        } catch (PresentationException e) {
            throw new IndexUnreachableException(e.getMessage());
        }
    }

    /**
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private SolrDocument retrieveSolrDocument() throws IndexUnreachableException {
        try {
            String query = getSolrQueryForDocument();
            SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, null);
            return doc;
        } catch (PresentationException e) {
            throw new IndexUnreachableException(e.toString());
        }
    }

    /**
     * @return
     */
    @JsonIgnore
    public String getSolrQueryForDocument() {
        String query = "+PI_TOPSTRUCT:%s";
        if (StringUtils.isNotBlank(logId)) {
            query += " +LOGID:%s";
            query = String.format(query, this.pi, this.logId);
        } else {
            query += " +(ISWORK:* OR ISANCHOR:*)";
            query = String.format(query, this.pi);
        }
        return query;
    }

    @JsonIgnore
    public BrowseElement getBrowseElement() throws IndexUnreachableException {
        if (this.browseElement == null) {
            try {
                SolrDocument doc = retrieveSolrDocument();
                if (this.getOrder() != null) {
                    doc.setField(SolrConstants.ORDER, this.getOrder());
                } else if (StringUtils.isNotBlank(this.getLogId())) {
                    doc.setField(SolrConstants.LOGID, this.getLogId());
                }
                if (doc != null) {
                    Locale locale = BeanUtils.getLocale();
                    SearchHitFactory factory = new SearchHitFactory(null, null, null, 0, BeanUtils.getImageDeliveryBean().getThumbs(), locale);
                    SearchHit sh = factory.createSearchHit(doc, null, null, null);
                    this.browseElement = sh.getBrowseElement();
                    try {
                        this.browseElement
                                .setThumbnailUrl(this.getRepresentativeImageUrl(DataManager.getInstance().getConfiguration().getThumbnailsWidth(),
                                        DataManager.getInstance().getConfiguration().getThumbnailsHeight()));
                    } catch (IndexUnreachableException | ViewerConfigurationException | DAOException e) {
                        logger.error("Unable to set thumbnail url of browseElement to bookmark thumbnail url: {}", e.toString());
                    }

                }
            } catch (PresentationException e) {
                throw new IndexUnreachableException(e.toString());
            }
        }

        return this.browseElement;
    }

    @JsonIgnore
    public boolean isHasImages() {
        try {
            if (this.hasImages != null) {
                //no action required
            } else if (this.browseElement != null) {
                this.hasImages = this.browseElement.isHasImages();
            } else {
                this.hasImages = isHasImagesFromSolr();
            }
        } catch (IndexUnreachableException | PresentationException e) {
            logger.error("Unable to get browse element for bookmark", e);
            return false;
        }
        return this.hasImages;
    }

    private boolean isHasImagesFromSolr() throws IndexUnreachableException, PresentationException {
        SolrDocument doc = DataManager.getInstance()
                .getSearchIndex()
                .getFirstDoc(getSolrQueryForDocument(), Arrays.asList(SolrConstants.THUMBNAIL, SolrConstants.FILENAME));
        return SolrTools.isHasImages(doc);
    }

}
