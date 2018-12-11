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

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.eclipse.persistence.annotations.PrivateOwned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.exceptions.CmsElementNotFoundException;

/**
 * Content instance of a CMS page for a particular language.
 */
@Entity
@Table(name = "cms_page_language_versions")
public class CMSPageLanguageVersion {

	public enum CMSPageStatus {
		WIP, REVIEW_PENDING, FINISHED;
	}

	/** Logger for this class. */
	private static final Logger logger = LoggerFactory.getLogger(CMSPageLanguageVersion.class);

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "cms_page_language_version_id")
	private Long id;

	/** Reference to the owning <code>CMSPage</code>. */
	@ManyToOne
	@JoinColumn(name = "owner_page_id")
	private CMSPage ownerPage;

	@Column(name = "language", nullable = false)
	private String language;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private CMSPageStatus status = CMSPageStatus.WIP;

	@Column(name = "title", nullable = false)
	private String title = "";

	@Column(name = "menu_title", nullable = false)
	private String menuTitle = "";

	@OneToMany(mappedBy = "ownerPageLanguageVersion", fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
	@PrivateOwned
	private List<CMSContentItem> contentItems = new ArrayList<>();

	@Transient
	private List<CMSContentItem> completeContentItemList = null;

	public CMSPageLanguageVersion() {
	    
	}
	
	   public CMSPageLanguageVersion(String language) {
	        this.language = language;
	    }
	
	/**
     * @param language2
     * @param cmsPage
     */
    public CMSPageLanguageVersion(CMSPageLanguageVersion original, CMSPage ownerPage) {
        if(original.id != null) {            
            this.id = new Long(original.id);
        }
        this.ownerPage = ownerPage;
        this.language = original.language;
        this.status = original.status;
        this.title = original.title;
        this.menuTitle = original.menuTitle;
        
        if(original.contentItems != null) {     
            this.contentItems = new ArrayList<>();
            for (CMSContentItem item : original.contentItems) {
                CMSContentItem copy = new CMSContentItem(item, this);
                this.contentItems.add(copy);
            }
        }
    }

    /**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the ownerPage
	 */
	public CMSPage getOwnerPage() {
		return ownerPage;
	}

	/**
	 * @param ownerPage
	 *            the ownerPage to set
	 */
	public void setOwnerPage(CMSPage ownerPage) {
		this.ownerPage = ownerPage;
	}

	/**
	 * @return the language
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * @param language
	 *            the language to set
	 */
	public void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * @return the status
	 */
	public CMSPageStatus getStatus() {
		return status;
	}

	/**
	 * @param status
	 *            the status to set
	 */
	public void setStatus(CMSPageStatus status) {
		this.status = status;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title
	 *            the title to set
	 */
	public void setTitle(String title) {
		this.title = title != null ? Normalizer.normalize(title, Form.NFC) : title;
	}

	/**
	 * @return the menuTitle
	 */
	public String getMenuTitle() {
		return menuTitle;
	}

	/**
	 * @param menuTitle
	 *            the menuTitle to set
	 */
	public void setMenuTitle(String menuTitle) {
		this.menuTitle = menuTitle != null ? Normalizer.normalize(menuTitle, Form.NFC) : "";
	}

	/**
	 * @return the contentItems
	 */
	public List<CMSContentItem> getContentItems() {
		// Collections.sort(contentItems);
		return contentItems;
	}

	/**
	 * @param contentItems
	 *            the contentItems to set
	 */
	public void setContentItems(List<CMSContentItem> contentItems) {
		this.contentItems = contentItems;
	}

	/**
	 * @param itemId
	 * @return
	 * @throws CmsElementNotFoundException 
	 */
	public CMSContentItem getContentItem(String itemId) throws CmsElementNotFoundException {
	    if(getCompleteContentItemList() != null) {	        
	        for (CMSContentItem item : getCompleteContentItemList()) {
	            if (item.getItemId().equals(itemId)) {
	                return item;
	            }
	        }
	    }
		throw new CmsElementNotFoundException("No element of id " + itemId + " found in " + this);
	}

	public List<CMSContentItem> getCompleteContentItemList() {
		if (completeContentItemList == null) {
			generateCompleteContentItemList();
		}
		return completeContentItemList;
	}
	

	/**
	 *
	 */
	protected void generateCompleteContentItemList() {
		CMSPageLanguageVersion global;
        try {
            global = getOwnerPage().getLanguageVersion(CMSPage.GLOBAL_LANGUAGE);
        } catch (CmsElementNotFoundException e) {
            global = null;
        }
		completeContentItemList = new ArrayList<>();
		if (CMSPage.GLOBAL_LANGUAGE.equals(this.getLanguage())) {
			completeContentItemList.addAll(getContentItems());
		} else if (global != null) {
			completeContentItemList.addAll(getContentItems());
			completeContentItemList.addAll(global.getContentItems());
		} else {
			completeContentItemList = getContentItems();
		}
		sortItems(completeContentItemList);
	}

	/**
	 * @param completeContentItemList2
	 */
	private void sortItems(List<CMSContentItem> items) {
		if (getOwnerPage().getTemplate() != null) {
			for (CMSContentItem cmsContentItem : items) {
				for (CMSContentItem templateItem : getOwnerPage().getTemplate().getContentItems()) {
					if (templateItem.getItemId().equals(cmsContentItem.getItemId())) {
						cmsContentItem.setOrder(templateItem.getOrder());
					}
				}
			}
		}
		Collections.sort(items);
	}

	@Override
	public String toString() {
		return CMSPageLanguageVersion.class.getSimpleName() + ": " + getLanguage();
	}

	/**
	 * @param item
	 */
	public void addContentItem(CMSContentItem item) {
		for (CMSContentItem existingItem : contentItems) {
			if (item.getItemId().equals(existingItem.getItemId())) {
				logger.warn("Cannot add content item {}. An item with this id already exists", item.getItemId());
				return;
			}
		}
		item.setOwnerPageLanguageVersion(this);
		contentItems.add(item);
		generateCompleteContentItemList();
	}
}
