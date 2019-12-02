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
package io.goobi.viewer.model.cms;

import java.io.IOException;
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
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.annotations.PrivateOwned;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.TEITools;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.CmsMediaBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.tilegrid.ImageGalleryTile;
import io.goobi.viewer.model.viewer.BrowseElementInfo;

@Entity
@Table(name = "cms_media_items")
public class CMSMediaItem implements BrowseElementInfo, ImageGalleryTile, Comparable<CMSMediaItem> {

	/** Logger for this class. */
	private static final Logger logger = LoggerFactory.getLogger(CMSMediaItem.class);

	public static final String CONTENT_TYPE_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	public static final String CONTENT_TYPE_DOC = "application/msword";
	public static final String CONTENT_TYPE_RTF = "application/rtf";
	public static final String CONTENT_TYPE_RTF2 = "application/x-rtf";
	public static final String CONTENT_TYPE_RTF3 = "text/rtf";
	public static final String CONTENT_TYPE_RTF4 = "text/richtext";
	public static final String CONTENT_TYPE_XML = "text/xml";
	public static final String CONTENT_TYPE_HTML = "text/html";
	public static final String CONTENT_TYPE_SVG = "image/svg+xml";
    public static final String CONTENT_TYPE_PDF = "application/pdf";

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

	@Column(name = "image_alt_text", nullable = true)
	private String alternativeText = "";

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "cms_media_item_metadata", joinColumns = @JoinColumn(name = "owner_media_item_id"))
	@PrivateOwned
	private List<CMSMediaItemMetadata> metadata = new ArrayList<>();

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "cms_media_item_cms_categories", joinColumns = @JoinColumn(name = "media_item_id"), inverseJoinColumns = @JoinColumn(name = "category_id"))
	private List<CMSCategory> categories = new ArrayList<>();

	@Column(name = "display_order", nullable = true)
	private int displayOrder = 0;

	@Transient
	private FileTime lastModifiedTime = null;


	/**
	 * default constructor
	 */
	public CMSMediaItem() {
	}

	/**
	 * copy constructor
	 * 
	 * @param currentMediaItem
	 */
	public CMSMediaItem(CMSMediaItem orig) {
		if (orig.id != null) {
			this.id = new Long(orig.id);
		}
		this.fileName = orig.fileName;
		this.link = orig.link;
		this.priority = orig.priority;
		this.displayOrder = orig.displayOrder;
		this.categories = new ArrayList<>(orig.getCategories());

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
	 * Perform any necessary post-upload processing (e.g. format conversions).
	 * 
	 * @throws IOException
	 * @throws JDOMException
	 */
	public void processMediaFile(Path mediaFile) {
		if (mediaFile == null) {
			return;
		}

		if (CONTENT_TYPE_DOCX.equals(getContentType())) {
			try {
				// TODO convert to TEI
				String tei = TEITools.convertDocxToTei(mediaFile);
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
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
		case "doc":
			return CONTENT_TYPE_DOC;
		case "docx":
			return CONTENT_TYPE_DOCX;
		case "htm":
		case "html":
		case "xhtml":
			return CONTENT_TYPE_HTML;
		case "xml":
			return CONTENT_TYPE_XML;
		case "rtf":
			return CONTENT_TYPE_RTF;
		case "jpg":
		case "jpeg":
		case "png":
		case "tif":
		case "tiff":
		case "jp2":
			return ImageFileFormat.getImageFileFormatFromFileExtension(extension).getMimeType();
		case "svg":
			return CONTENT_TYPE_SVG;
		case "pdf":
		    return CONTENT_TYPE_PDF;
		default:
			return "";
		}
	}

	/**
	 * Checks whether this media item contains a text file that can be exported for
	 * indexing.
	 * 
	 * @return true if item content types allows for text export; false otherwise
	 */
	public boolean isHasExportableText() {
		switch (getContentType()) {
		case CMSMediaItem.CONTENT_TYPE_DOC:
		case CMSMediaItem.CONTENT_TYPE_DOCX:
		case CMSMediaItem.CONTENT_TYPE_RTF:
		case CMSMediaItem.CONTENT_TYPE_HTML:
			return true;
		default:
			return false;
		}
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
		this.lastModifiedTime = null;
	}

	/**
	 * @return the alternativeText
	 */
	public String getAlternativeText() {
		return alternativeText;
	}

	/**
	 * @param alternativeText the alternativeText to set
	 */
	public void setAlternativeText(String alternativeText) {
		this.alternativeText = alternativeText;
	}

	/**
	 * 
	 * @param locale
	 * @return media item metadata for the given locale; null if no locale given
	 */
	public CMSMediaItemMetadata getMetadataForLocale(Locale locale) {
		if (locale != null) {
			return getMetadataForLanguage(locale.getLanguage());
		}

		return null;
	}

	/**
	 * 
	 * @param language
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
	 * Adds a metadata item to the list of image metadata. If a metadata item with
	 * the same language string exists, it is replaced
	 *
	 * @param metadata
	 */
	public void addMetadata(CMSMediaItemMetadata metadata) {
		String language = metadata.getLanguage();
		if (getMetadataForLanguage(language) != null) {
			getMetadata().remove(getMetadataForLanguage(language));
		}
		getMetadata().add(metadata);
	}

	/**
	 * 
	 * @return metadata list for the current language
	 */
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

	public boolean hasCateories() {
		return !this.categories.isEmpty();
	}

	@Override
	public List<CMSCategory> getCategories() {
		return this.categories;
	}

	public void setCategories(List<CMSCategory> categories) {
		this.categories = categories;
	}

	public boolean removeCategory(CMSCategory cat) {
		return this.categories.remove(cat);
	}

	public boolean addCategory(CMSCategory cat) {
		if (this.categories.contains(cat)) {
			return false;
		}
		return this.categories.add(cat);
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
		String link = getLink();
		if (StringUtils.isNotBlank(link)) {
			try {
				URI uri = new URI(link);
				if (!uri.isAbsolute()) {
					String viewerURL = "/";
					if (request != null) {
						viewerURL = request.getContextPath();
					}
					link = StringTools.decodeUrl(link);
					String urlString = (viewerURL + link).replace("//", "/");
					uri = new URI(urlString);
				}
				return uri;
			} catch (URISyntaxException e) {
				logger.error("Unable to create uri from " + getLink());
				return null;
			}
		}

		return null;
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
		if (StringUtils.isBlank(linkUrl)) {
			this.link = null;
		} else {
			this.link = new URI(linkUrl);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.goobi.viewer.model.viewer.BrowseElementInfo#
	 * getDescription()
	 */
	@Override
	public String getDescription() {
		return getCurrentLanguageMetadata().getDescription();
	}

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
	@Override
	public URI getIconURI(int width, int height) {
		if (getFileName() != null) {
			try {
				String uriString = CmsMediaBean.getMediaUrl(this, Integer.toString(width), Integer.toString(height));
				return new URI(uriString);
			} catch (URISyntaxException e) {
				logger.error("Failed to create resource uri for " + getFileName() + ": " + e.getMessage());
			} catch (ViewerConfigurationException e) {
				logger.error(e.getMessage());
			}
		}

		return null;

	}

	@Override
	public URI getIconURI(int size) {
		if (getFileName() != null) {
			try {
				String uriString = BeanUtils.getImageDeliveryBean().getThumbs().getSquareThumbnailUrl(this, size);
				return new URI(uriString);
			} catch (URISyntaxException e) {
				logger.error("Failed to create resource uri for " + getFileName() + ": " + e.getMessage());
			}
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
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
	@Override
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
	@Override
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

	public String getImageURI() {

		Path path = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome(),
				DataManager.getInstance().getConfiguration().getCmsMediaFolder(), getFileName());
		return path.toUri().toString();

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

	@Override
	public IMetadataValue getTranslationsForName() {
		Map<String, String> names = getMetadata().stream().filter(md -> StringUtils.isNotBlank(md.getName()))
				.collect(Collectors.toMap(CMSMediaItemMetadata::getLanguage, CMSMediaItemMetadata::getName));
		return new MultiLanguageMetadataValue(names);
		// return IMetadataValue.getTranslations(getName());
	}

	public IMetadataValue getTranslationsForDescription() {
		Map<String, String> names = getMetadata().stream().filter(md -> StringUtils.isNotBlank(md.getDescription()))
				.collect(Collectors.toMap(CMSMediaItemMetadata::getLanguage, CMSMediaItemMetadata::getDescription));
		return new MultiLanguageMetadataValue(names);
	}

	public boolean isFinished(Locale locale) {
		return StringUtils.isNotBlank(getMetadataForLocale(locale).getName());
	}

	public List<Locale> getFinishedLocales() {
		return this.getMetadata().stream().filter(md -> StringUtils.isNotBlank(md.getName()))
				.map(md -> Locale.forLanguageTag(md.getLanguage())).collect(Collectors.toList());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
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
			return 0;
		}
	}

	/**
	 * @return the lastModifiedTime. May be null only if no file exists or last
	 *         modified time cannot be read
	 */
	public synchronized FileTime getLastModifiedTime() {
		if (lastModifiedTime == null) {
			lastModifiedTime = FileTime.fromMillis(0); // fallback
			Path filePath = getFilePath();
			if (Files.exists(filePath)) {
				try {
					lastModifiedTime = Files.getLastModifiedTime(filePath);
				} catch (IOException e) {
					logger.error("Error reading last modified time from " + filePath, e);
				}
			}
		}
		return lastModifiedTime;
	}

	/**
	 * @return
	 */
	public Path getFilePath() {
		Path folder = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome(),
				DataManager.getInstance().getConfiguration().getCmsMediaFolder());
		Path file = folder.resolve(getFileName());
		return file;
	}

	/**
	 * @return the categoryMap. Never null. If it isn't defined yet, create a map
	 *         from all categories
	 * @throws DAOException TODO: check for licenses
	 */
	public synchronized List<Selectable<CMSCategory>> wrapCategories(List<CMSCategory> categories){
		List<Selectable<CMSCategory>> wrappedCategories = categories.stream()
					.map(cat -> new Selectable<>(cat, this.getCategories().contains(cat))).collect(Collectors.toList());
		return wrappedCategories;
	}


}
