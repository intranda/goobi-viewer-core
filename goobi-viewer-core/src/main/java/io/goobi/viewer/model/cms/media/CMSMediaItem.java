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
package io.goobi.viewer.model.cms.media;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.persistence.annotations.PrivateOwned;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.managedbeans.CmsMediaBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.Selectable;
import io.goobi.viewer.model.viewer.collections.BrowseElementInfo;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * <p>
 * CMSMediaItem class.
 * </p>
 */
@Entity
@Table(name = "cms_media_items")
public class CMSMediaItem implements BrowseElementInfo, Comparable<CMSMediaItem>, Serializable {

    private static final long serialVersionUID = 31745843830948125L;

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(CMSMediaItem.class);

    /** Constant <code>CONTENT_TYPE_XML="text/xml"</code> */
    public static final String CONTENT_TYPE_XML = "text/xml";
    /** Constant <code>CONTENT_TYPE_SVG="image/svg+xml"</code> */
    public static final String CONTENT_TYPE_SVG = "image/svg+xml";
    /** Constant <code>CONTENT_TYPE_SVG="image/svg+xml"</code> */
    public static final String CONTENT_TYPE_ICO = "image/x-icon";
    /** Constant <code>CONTENT_TYPE_PDF="application/pdf"</code> */
    public static final String CONTENT_TYPE_PDF = "application/pdf";
    /** Constant <code>CONTENT_TYPE_GIF="application/gif"</code> */
    public static final String CONTENT_TYPE_GIF = "image/gif";
    /** Constant <code>CONTENT_TYPE_VIDEO="video"</code> */
    public static final String CONTENT_TYPE_VIDEO = "video";
    /** Constant <code>CONTENT_TYPE_AUDIO="audio"</code> */
    public static final String CONTENT_TYPE_AUDIO = "audio";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cms_media_item_id")
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "link_url", nullable = true)
    private String link;

    @Column(name = "priority", nullable = true)
    private Priority priority = Priority.DEFAULT;

    @Column(name = "image_alt_text", nullable = true)
    private String alternativeText = "";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cms_media_item_metadata", joinColumns = @JoinColumn(name = "owner_media_item_id"))
    @PrivateOwned
    private List<CMSMediaItemMetadata> metadata = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "cms_media_item_cms_categories", joinColumns = @JoinColumn(name = "media_item_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private List<CMSCategory> categories = new ArrayList<>();

    @Column(name = "display_order", nullable = true)
    private int displayOrder = 0;

    @Transient
    private transient FileTime lastModifiedTime = null;

    /**
     * default constructor
     */
    public CMSMediaItem() {
    }

    /**
     * copy constructor
     *
     * @param orig a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     */
    public CMSMediaItem(CMSMediaItem orig) {
        if (orig.id != null) {
            this.id = orig.id;
        }
        this.fileName = orig.fileName;
        this.link = orig.link;
        this.priority = orig.priority;
        this.displayOrder = orig.displayOrder;
        this.categories = new ArrayList<>(orig.getCategories());
        this.lastModifiedTime = orig.lastModifiedTime;

        for (CMSMediaItemMetadata origMetadata : orig.metadata) {
            CMSMediaItemMetadata copy = new CMSMediaItemMetadata(origMetadata);
            this.metadata.add(copy);
        }
    }

    /*
     * (non-Javadoc)
     *
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

    /*
     * (non-Javadoc)
     *
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
     * Determines this media item's file's content type via the extension.
     *
     * @return Content type string
     */
    public String getContentType() {
        if (fileName == null) {
            return "";
        }

        String extension = FilenameUtils.getExtension(fileName).toLowerCase();
        switch (extension) {
            case "xml":
                return CONTENT_TYPE_XML;
            case "jpg":
            case "jpeg":
            case "png":
            case "tif":
            case "tiff":
            case "jp2":
                return ImageFileFormat.getImageFileFormatFromFileExtension(extension).getMimeType();
            case "svg":
                return CONTENT_TYPE_SVG;
            case "ico":
                return CONTENT_TYPE_ICO;
            case "pdf":
                return CONTENT_TYPE_PDF;
            case "gif":
                return CONTENT_TYPE_GIF;
            case "mp4":
            case "mpeg4":
            case "avi":
            case "mov":
            case "wmv":
                return CONTENT_TYPE_VIDEO;
            case "mp3":
            case "mpeg":
            case "wav":
            case "ogg":
            case "wma":
                return CONTENT_TYPE_AUDIO;
            default:
                return "";
        }
    }

    /**
     * Checks whether this media item contains a text file that can be exported for indexing.
     *
     * @return true if item content types allows for text export; false otherwise
     */
    public boolean isHasExportableText() {
        return false;
    }

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
     * Getter for the field <code>fileName</code>.
     * </p>
     *
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * <p>
     * Setter for the field <code>fileName</code>.
     * </p>
     *
     * @param fileName the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
        this.lastModifiedTime = null;
    }

    /**
     * <p>
     * Getter for the field <code>alternativeText</code>.
     * </p>
     *
     * @return the alternativeText
     */
    public String getAlternativeText() {
        return alternativeText;
    }

    /**
     * <p>
     * Setter for the field <code>alternativeText</code>.
     * </p>
     *
     * @param alternativeText the alternativeText to set
     */
    public void setAlternativeText(String alternativeText) {
        this.alternativeText = alternativeText;
    }

    /**
     * <p>
     * getMetadataForLocale.
     * </p>
     *
     * @param locale a {@link java.util.Locale} object.
     * @return media item metadata for the given locale; null if no locale given
     */
    public CMSMediaItemMetadata getMetadataForLocale(Locale locale) {
        if (locale != null) {
            return getMetadataForLanguage(locale.getLanguage());
        }

        return null;
    }

    /**
     * <p>
     * getMetadataForLanguage.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     * @return media item metadata for the given locale
     */
    public CMSMediaItemMetadata getMetadataForLanguage(String language) {
        for (CMSMediaItemMetadata md : metadata) {
            if (md.getLanguage().equals(language)) {
                return md;
            }
        }

        CMSMediaItemMetadata md = new CMSMediaItemMetadata();
        md.setLanguage(language);
        this.metadata.add(md);
        return md;
    }

    /**
     * <p>
     * Getter for the field <code>metadata</code>.
     * </p>
     *
     * @return the metadata
     */
    public List<CMSMediaItemMetadata> getMetadata() {
        return metadata;
    }

    /**
     * <p>
     * Setter for the field <code>metadata</code>.
     * </p>
     *
     * @param metadata the metadata to set
     */
    public void setMetadata(List<CMSMediaItemMetadata> metadata) {
        this.metadata = metadata;
    }

    /**
     * Adds a metadata item to the list of image metadata. If a metadata item with the same language string exists, it is replaced
     *
     * @param metadata a {@link io.goobi.viewer.model.cms.media.CMSMediaItemMetadata} object.
     */
    public void addMetadata(CMSMediaItemMetadata metadata) {
        String language = metadata.getLanguage();
        if (getMetadataForLanguage(language) != null) {
            getMetadata().remove(getMetadataForLanguage(language));
        }
        getMetadata().add(metadata);
    }

    /**
     * <p>
     * getCurrentLanguageMetadata.
     * </p>
     *
     * @return metadata list for the current language
     */
    public CMSMediaItemMetadata getCurrentLanguageMetadata() {
        CMSMediaItemMetadata version = null;
        if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getViewRoot() != null) {
            version = getMetadataForLocale(FacesContext.getCurrentInstance().getViewRoot().getLocale());
        }
        if (version == null && !getMetadata().isEmpty()) {
            version = getMetadata().get(0);
        }
        if (version == null) {
            return null;
        }

        return version;
    }

    /**
     * <p>
     * hasCateories.
     * </p>
     *
     * @return a boolean.
     */
    public boolean hasCateories() {
        return !this.categories.isEmpty();
    }

    /** {@inheritDoc} */
    public List<CMSCategory> getCategories() {
        return this.categories;
    }

    /**
     * <p>
     * Setter for the field <code>categories</code>.
     * </p>
     *
     * @param categories a {@link java.util.List} object.
     */
    public void setCategories(List<CMSCategory> categories) {
        this.categories = categories;
    }

    /**
     * <p>
     * removeCategory.
     * </p>
     *
     * @param cat a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @return a boolean.
     */
    public boolean removeCategory(CMSCategory cat) {
        return this.categories.remove(cat);
    }

    /**
     * <p>
     * addCategory.
     * </p>
     *
     * @param cat a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @return a boolean.
     */
    public boolean addCategory(CMSCategory cat) {
        if (this.categories.contains(cat)) {
            return false;
        }
        return this.categories.add(cat);
    }

    /** {@inheritDoc} */
    public boolean isImportant() {
        return Priority.IMPORTANT.equals(this.priority);
    }

    /**
     * <p>
     * setImportant.
     * </p>
     *
     * @param important a boolean.
     */
    public void setImportant(boolean important) {
        this.priority = important ? Priority.IMPORTANT : Priority.DEFAULT;
    }

    /** {@inheritDoc} */
    public Priority getPriority() {
        if (priority == null) {
            priority = Priority.DEFAULT;
        }
        return priority;
    }

    /**
     * <p>
     * Setter for the field <code>priority</code>.
     * </p>
     *
     * @param priority the priority to set
     */
    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    /**
     * <p>
     * getLinkURI.
     * </p>
     *
     * @return a {@link java.net.URI} object.
     */
    @Override
    public URI getLinkURI() {
        return getLinkURI(BeanUtils.getRequest());
    }

    /** {@inheritDoc} */
    @Override
    public URI getLinkURI(HttpServletRequest request) {
        String lnk = getLink();
        if (StringUtils.isNotBlank(link)) {
            try {
                URI uri = new URI(link);
                if (!uri.isAbsolute()) {
                    String viewerURL = "/";
                    if (request != null) {
                        viewerURL = request.getContextPath();
                    }
                    lnk = StringTools.decodeUrl(lnk);
                    String urlString = (viewerURL + lnk).replace("//", "/");
                    uri = new URI(urlString);
                }
                return uri;
            } catch (URISyntaxException e) {
                logger.error("Unable to create uri from {}", getLink());
                return null;
            }
        }

        return null;
    }

    /**
     * <p>
     * Getter for the field <code>link</code>.
     * </p>
     *
     * @return the entered link url
     */
    public String getLink() {
        return this.link;
    }

    /**
     * set the link for this media item
     *
     * @param linkUrl a {@link java.lang.String} object.
     * @throws java.net.URISyntaxException if any.
     */
    public void setLink(String linkUrl) {
        this.link = linkUrl;
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.model.viewer.BrowseElementInfo#
     * getDescription()
     */
    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return getCurrentLanguageMetadata().getDescription();
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return getCurrentLanguageMetadata().getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * io.goobi.viewer.model.viewer.BrowseElementInfo#getIconURI(
     * )
     */
    /** {@inheritDoc} */
    @Override
    public URI getIconURI() {

        int height = DataManager.getInstance().getConfiguration().getCmsMediaDisplayHeight();
        int width = DataManager.getInstance().getConfiguration().getCmsMediaDisplayWidth();
        return getIconURI(width, height);
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.model.cms.tilegrid.ImageGalleryTile#
     * getIconURI(int, int)
     */
    /** {@inheritDoc} */
    @Override
    public URI getIconURI(int width, int height) {
        if (getFileName() != null) {
            try {
                String uriString = CmsMediaBean.getMediaUrl(this, Integer.toString(width), Integer.toString(height));
                return new URI(uriString);
            } catch (URISyntaxException e) {
                logger.error("Failed to create resource uri for {}: {}", getFileName(), e.getMessage());
            }
        }

        return null;

    }

    /** {@inheritDoc} */
    @Override
    public URI getIconURI(int size) {
        if (getFileName() != null) {
            try {
                String uriString = BeanUtils.getImageDeliveryBean().getThumbs().getSquareThumbnailUrl(this, size);
                return new URI(uriString);
            } catch (URISyntaxException e) {
                logger.error("Failed to create resource uri for {}: {}", getFileName(), e.getMessage());
            }
        }

        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "ID=" + id + " (FILE=" + fileName + ")";
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.model.cms.tilegrid.ImageGalleryTile#
     * getName(java.lang.String)
     */
    /** {@inheritDoc} */
    public String getName(String language) {
        if (getMetadataForLanguage(language) != null) {
            return getMetadataForLanguage(language).getName();
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.model.cms.tilegrid.ImageGalleryTile#
     * getDescription(java.lang.String)
     */
    /** {@inheritDoc} */
    public String getDescription(String language) {
        if (getMetadataForLanguage(language) != null) {
            return getMetadataForLanguage(language).getDescription();
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see io.goobi.viewer.model.cms.tilegrid.ImageGalleryTile#
     * getDisplayOrder()
     */
    /** {@inheritDoc} */
    public int getDisplayOrder() {
        return this.displayOrder;
    }

    /**
     * <p>
     * Setter for the field <code>displayOrder</code>.
     * </p>
     *
     * @param displayOrder the displayOrder to set
     */
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    /**
     * <p>
     * getImageURI.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getImageURI() {
        // Path.get() adds a "C:" to Unix paths. This must be prevented when using an external API in a Windows dev environment.
        Path path = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome(),
                DataManager.getInstance().getConfiguration().getCmsMediaFolder(), getFileName());
        return FileTools.adaptPathForWindows(path.toUri().toString());

        // StringBuilder imageUrlBuilder = new StringBuilder("file:/");
        //
        // // Add an extra slash if not on Windows
        // String os = System.getProperty("os.name").toLowerCase();
        // if (os.indexOf("win") == -1) {
        // imageUrlBuilder.append('/');
        // }
        // imageUrlBuilder.append(DataManager.getInstance().getConfiguration().getViewerHome());
        // imageUrlBuilder.append(DataManager.getInstance().getConfiguration().getCmsMediaFolder()).append('/');
        // imageUrlBuilder.append(getFileName());
        // return imageUrlBuilder.toString();
    }

    /** {@inheritDoc} */
    @Override
    public IMetadataValue getTranslationsForName() {
        Map<String, String> names = getMetadata().stream()
                .filter(md -> StringUtils.isNotBlank(md.getName()))
                .collect(Collectors.toMap(CMSMediaItemMetadata::getLanguage, CMSMediaItemMetadata::getName));
        return new MultiLanguageMetadataValue(names);
    }

    /**
     * <p>
     * getTranslationsForDescription.
     * </p>
     *
     * @return a {@link de.intranda.metadata.multilanguage.IMetadataValue} object.
     */
    public IMetadataValue getTranslationsForDescription() {
        Map<String, String> names = getMetadata().stream()
                .filter(md -> StringUtils.isNotBlank(md.getDescription()))
                .collect(Collectors.toMap(CMSMediaItemMetadata::getLanguage, CMSMediaItemMetadata::getDescription));
        return new MultiLanguageMetadataValue(names);
    }

    /**
     * <p>
     * isFinished.
     * </p>
     *
     * @param locale a {@link java.util.Locale} object.
     * @return a boolean.
     */
    public boolean isFinished(Locale locale) {
        return StringUtils.isNotBlank(getMetadataForLocale(locale).getName());
    }

    /**
     * <p>
     * getFinishedLocales.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<Locale> getFinishedLocales() {
        return this.getMetadata()
                .stream()
                .filter(md -> StringUtils.isNotBlank(md.getName()))
                .map(md -> Locale.forLanguageTag(md.getLanguage()))
                .collect(Collectors.toList());
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public int compareTo(CMSMediaItem o) {
        FileTime myTime = getLastModifiedTime();
        FileTime oTime = o.getLastModifiedTime();
        if (myTime != null && oTime != null) {
            return oTime.compareTo(myTime);
        } else if (myTime != null) {
            return -1;
        } else if (oTime != null) {
            return 1;
        } else {
            return this.getId().compareTo(o.getId());
        }
    }

    /**
     * <p>
     * Getter for the field <code>lastModifiedTime</code>.
     * </p>
     *
     * @return the lastModifiedTime. May be null only if no file exists or last modified time cannot be read
     */
    public synchronized FileTime getLastModifiedTime() {
        if (lastModifiedTime == null) {
            lastModifiedTime = FileTime.fromMillis(0); // fallback
            Path filePath = getFilePath();
            if (Files.exists(filePath)) {
                try {
                    lastModifiedTime = Files.getLastModifiedTime(filePath);
                } catch (IOException e) {
                    logger.error("Error reading last modified time from {}", filePath, e);
                }
            }
        }
        return lastModifiedTime;
    }

    /**
     * <p>
     * getFilePath.
     * </p>
     *
     * @return a {@link java.nio.file.Path} object.
     */
    public Path getFilePath() {
        Path folder = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome(),
                DataManager.getInstance().getConfiguration().getCmsMediaFolder());
        return folder.resolve(getFileName());
    }

    /**
     * <p>
     * wrapCategories.
     * </p>
     *
     * @return the categoryMap. Never null. If it isn't defined yet, create a map from all categories
     * @param categories a {@link java.util.List} object.
     */
    public synchronized List<Selectable<CMSCategory>> wrapCategories(List<CMSCategory> categories) {
        return categories.stream().map(cat -> new Selectable<>(cat, this.getCategories().contains(cat))).collect(Collectors.toList());
    }

    public enum Priority {
        IMPORTANT,
        DEFAULT;
    }
}
