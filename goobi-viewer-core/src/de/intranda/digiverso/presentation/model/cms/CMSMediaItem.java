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
package de.intranda.digiverso.presentation.model.cms;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.faces.context.FacesContext;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.annotations.PrivateOwned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.managedbeans.CmsMediaBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.cms.tilegrid.ImageGalleryTile;
import de.intranda.digiverso.presentation.model.viewer.BrowseElementInfo;
import de.intranda.digiverso.presentation.model.viewer.PageType;

@Entity
@Table(name = "cms_media_items")
public class CMSMediaItem implements BrowseElementInfo, ImageGalleryTile {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(CMSMediaItem.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cms_media_item_id")
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "link_url", nullable = true)
    private URI link;

    @Column(name = "priority", nullable = true)
    private Priority priority = Priority.DEFAULT;

    @Column(name = "collection", nullable = true)
    private Boolean collection = false;

    @Column(name = "collection_field", nullable = true)
    private String collectionField = "DC";
    
    @Column(name = "collection_name", nullable = true)
    private String collectionName = null;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cms_media_item_metadata", joinColumns = @JoinColumn(name = "owner_media_item_id"))
    @PrivateOwned
    private List<CMSMediaItemMetadata> metadata = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cms_media_item_tags", joinColumns = @JoinColumn(name = "owner_media_item_id"))
    @Column(name = "tag_name")
    private List<String> tags = new ArrayList<>();

    @Column(name = "display_size", nullable = true)
    private DisplaySize displaySize = DisplaySize.DEFAULT;

    @Column(name = "display_order", nullable = true)
    private int displayOrder = 0;

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
        CMSMediaItem other = (CMSMediaItem) obj;
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
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public CMSMediaItemMetadata getMetadataForLocale(Locale locale) {
        if (locale != null) {
            return getMetadataForLanguage(locale.getLanguage());
        }

        return null;
    }

    public CMSMediaItemMetadata getMetadataForLanguage(String language) {
        for (CMSMediaItemMetadata md : metadata) {
            if (md.getLanguage().equals(language)) {
                return md;
            }
        }

        return null;
    }

    /**
     * @return the metadata
     */
    public List<CMSMediaItemMetadata> getMetadata() {
        return metadata;
    }

    /**
     * @param metadata the metadata to set
     */
    public void setMetadata(List<CMSMediaItemMetadata> metadata) {
        this.metadata = metadata;
    }

    /**
     * Adds a metadata item to the list of image metadata. If a metadata item with the same language string exists, it is replaced
     *
     * @param metadata_de
     */
    public void addMetadata(CMSMediaItemMetadata metadata) {
        String language = metadata.getLanguage();
        if (getMetadataForLanguage(language) != null) {
            getMetadata().remove(getMetadataForLanguage(language));
        }
        getMetadata().add(metadata);
    }

    public CMSMediaItemMetadata getCurrentLanguageMetadata() {
        CMSMediaItemMetadata version = null;
        if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getViewRoot() != null) {
            version = getMetadataForLocale(FacesContext.getCurrentInstance().getViewRoot().getLocale());
        }
        if (version == null && getMetadata().size() > 0) {
            version = getMetadata().get(0);
        }
        if (version == null) {
            return null;
        }

        return version;
    }

    public boolean hasTags() {
        return !tags.isEmpty();
    }

    @Override
    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public boolean removeTag(String tag) {
        return this.tags.remove(tag);
    }

    public boolean addTag(String tag) {
        if (this.tags.contains(tag)) {
            return false;
        }
        return this.tags.add(tag);
    }

    @Override
    public boolean isImportant() {
        return Priority.IMPORTANT.equals(this.priority);
    }

    public void setImportant(boolean important) {
        this.priority = important ? Priority.IMPORTANT : Priority.DEFAULT;
    }

    /**
     * @return the priority
     */
    @Override
    public Priority getPriority() {
        if (priority == null) {
            priority = Priority.DEFAULT;
        }
        return priority;
    }

    /**
     * @param priority the priority to set
     */
    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public URI getLinkURI() {
        return getLinkURI(BeanUtils.getRequest());
    }
    
    /**
     * @return the URI to this media item
     */
    @Override
    public URI getLinkURI(HttpServletRequest request) {
        
        if (StringUtils.isNotBlank(getLink())) {
            try {
                URI uri = new URI(URLDecoder.decode(getLink(), "utf-8"));            
                if(!uri.isAbsolute()) {                    
                    String viewerURL = "/";
                    if(request != null) {
                        viewerURL = request.getContextPath();
                    }
                    String urlString = (viewerURL + URLDecoder.decode(getLink(), "utf-8")).replace("//", "/");
                    uri = new URI(urlString);
                }
                return uri;
            } catch (URISyntaxException | UnsupportedEncodingException e) {
                logger.error("Unable to create uri from " + getLink());
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * 
     * @return the entered link url
     */
    public String getLink() {
        if (this.link != null) {
            return this.link.toString();
        }
        return null;
    }

    /**
     * set the link for this media item
     * 
     * @param linkUrl
     * @throws URISyntaxException
     */
    public void setLink(String linkUrl) throws URISyntaxException {
        this.link = new URI(linkUrl);
    }

    @Override
    public boolean isCollection() {
        if (collection == null) {
            collection = false;
        }
        return collection;
    }

    public void setCollection(boolean collection) {
        this.collection = collection;
    }

    @Override
    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) throws URISyntaxException, UnsupportedEncodingException {
        this.collectionName = collectionName;
        if (StringUtils.isNotBlank(this.collectionName) && StringUtils.isBlank(getLink())) {
            this.link = new URI(URLEncoder.encode(getCollectionSearchUri(), "utf-8"));
        }
    }

    @Deprecated
    public String getCollectionViewUri() {
        String baseUri = BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.browse.getName();
        return baseUri + "/" + PageType.expandCollection + "/" + getCollectionField() + ':' + getCollectionName() + "/";
    }

    public String getCollectionSearchUri() throws UnsupportedEncodingException {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.browse.getName() + "/" + getCollectionField() + ':'
                + URLEncoder.encode(getCollectionName(), "utf-8") + "/-/1/-/-/";
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.viewer.BrowseElementInfo#getDescription()
     */
    @Override
    public String getDescription() {
        return getCurrentLanguageMetadata().getDescription();
    }

    /**
     * @return the size
     */
    @Override
    public DisplaySize getSize() {
        return displaySize;
    }

    /**
     * @param size the size to set
     */
    public void setSize(DisplaySize size) {
        this.displaySize = size;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.viewer.BrowseElementInfo#getIconURI()
     */
    @Override
    public URI getIconURI() {
        if (getFileName() != null) {

            int height = DataManager.getInstance().getConfiguration().getCmsMediaDisplayHeight();
            int width = DataManager.getInstance().getConfiguration().getCmsMediaDisplayWidth();

            String uriString = CmsMediaBean.getMediaUrl(this, Integer.toString(width), Integer.toString(height));
            try {
                return new URI(uriString);
            } catch (URISyntaxException e) {
                logger.error("Failed to create resource uri for " + getFileName() + ": " + e.getMessage());
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ID=" + id + " (FILE=" + fileName + ")";
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.cms.tilegrid.ImageGalleryTile#getName(java.lang.String)
     */
    @Override
    public String getName(String language) {
        if (getMetadataForLanguage(language) != null) {
            return getMetadataForLanguage(language).getName();
        }
        return null;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.cms.tilegrid.ImageGalleryTile#getDescription(java.lang.String)
     */
    @Override
    public String getDescription(String language) {
        if (getMetadataForLanguage(language) != null) {
            return getMetadataForLanguage(language).getDescription();
        }
        return null;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.cms.tilegrid.ImageGalleryTile#getDisplayOrder()
     */
    @Override
    public int getDisplayOrder() {
        return this.displayOrder;
    }

    /**
     * @param displayOrder the displayOrder to set
     */
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
    
    /**
     * @return the collectionNField
     */
    public String getCollectionField() {
        if(StringUtils.isBlank(collectionField)) {
            return SolrConstants.DC;
        }
        return collectionField;
    }
    
    /**
     * @param collectionField the collectionField to set
     */
    public void setCollectionField(String collectionField) {
        this.collectionField = collectionField;
    }
    
    public String getImageURI() {
        StringBuilder imageUrlBuilder = new StringBuilder("file:/");

        // Add an extra slash if not on Windows
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("win") == -1) {
            imageUrlBuilder.append('/');
        }
        imageUrlBuilder.append(DataManager.getInstance().getConfiguration().getViewerHome());
        imageUrlBuilder.append(DataManager.getInstance().getConfiguration().getCmsMediaFolder()).append('/');
        imageUrlBuilder.append(getFileName());
        return imageUrlBuilder.toString();
    }

}
