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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.annotations.PrivateOwned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.exceptions.CmsElementNotFoundException;
import io.goobi.viewer.model.cms.CMSContentItem.CMSContentItemType;

/**
 * Content instance of a CMS page for a particular language.
 */
@Entity
@Table(name = "cms_page_language_versions")
public class CMSPageLanguageVersion {

    public enum CMSPageStatus {
        WIP,
        REVIEW_PENDING,
        FINISHED;
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

    /**
     * <p>
     * Constructor for CMSPageLanguageVersion.
     * </p>
     */
    public CMSPageLanguageVersion() {

    }

    /**
     * <p>
     * Constructor for CMSPageLanguageVersion.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     */
    public CMSPageLanguageVersion(String language) {
        this.language = language;
    }

    /**
     * <p>
     * Constructor for CMSPageLanguageVersion.
     * </p>
     *
     * @param original a {@link io.goobi.viewer.model.cms.CMSPageLanguageVersion} object.
     * @param ownerPage a {@link io.goobi.viewer.model.cms.CMSPage} object.
     */
    public CMSPageLanguageVersion(CMSPageLanguageVersion original, CMSPage ownerPage) {
        if (original.id != null) {
            this.id = new Long(original.id);
        }
        this.ownerPage = ownerPage;
        this.language = original.language;
        this.status = original.status;
        this.title = original.title;
        this.menuTitle = original.menuTitle;

        if (original.contentItems != null) {
            this.contentItems = new ArrayList<>();
            for (CMSContentItem item : original.contentItems) {
                CMSContentItem copy = new CMSContentItem(item, this);
                this.contentItems.add(copy);
            }
        }
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
     * Getter for the field <code>ownerPage</code>.
     * </p>
     *
     * @return the ownerPage
     */
    public CMSPage getOwnerPage() {
        return ownerPage;
    }

    /**
     * <p>
     * Setter for the field <code>ownerPage</code>.
     * </p>
     *
     * @param ownerPage the ownerPage to set
     */
    public void setOwnerPage(CMSPage ownerPage) {
        this.ownerPage = ownerPage;
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
     * Getter for the field <code>status</code>.
     * </p>
     *
     * @return the status
     */
    public CMSPageStatus getStatus() {
        return status;
    }

    /**
     * <p>
     * Setter for the field <code>status</code>.
     * </p>
     *
     * @param status the status to set
     */
    public void setStatus(CMSPageStatus status) {
        this.status = status;
    }

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
        this.title = title != null ? Normalizer.normalize(title, Form.NFC) : title;
    }

    /**
     * <p>
     * Getter for the field <code>menuTitle</code>.
     * </p>
     *
     * @return the menuTitle
     */
    public String getMenuTitle() {
        return menuTitle;
    }

    /**
     * <p>
     * getMenuTitleOrTitle.
     * </p>
     *
     * @return the menuTitle or the title if no menu title exists
     */
    public String getMenuTitleOrTitle() {
        return StringUtils.isBlank(menuTitle) ? title : menuTitle;
    }

    /**
     * <p>
     * Setter for the field <code>menuTitle</code>.
     * </p>
     *
     * @param menuTitle the menuTitle to set
     */
    public void setMenuTitle(String menuTitle) {
        this.menuTitle = menuTitle != null ? Normalizer.normalize(menuTitle, Form.NFC) : "";
    }

    /**
     * <p>
     * Getter for the field <code>contentItems</code>.
     * </p>
     *
     * @return the contentItems
     */
    public List<CMSContentItem> getContentItems() {
        // Collections.sort(contentItems);
        return contentItems;
    }

    /**
     * <p>
     * Setter for the field <code>contentItems</code>.
     * </p>
     *
     * @param contentItems the contentItems to set
     */
    public void setContentItems(List<CMSContentItem> contentItems) {
        this.contentItems = contentItems;
    }

    /**
     * <p>
     * getContentItem.
     * </p>
     *
     * @param itemId a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.cms.CMSContentItem} object.
     * @throws io.goobi.viewer.exceptions.CmsElementNotFoundException if any.
     */
    public CMSContentItem getContentItem(String itemId) throws CmsElementNotFoundException {
        if (getCompleteContentItemList() != null) {
            for (CMSContentItem item : getCompleteContentItemList()) {
                if (item.getItemId().equals(itemId)) {
                    return item;
                }
            }
        }
        throw new CmsElementNotFoundException("No element of id " + itemId + " found in " + this);
    }

    /**
     * <p>
     * Getter for the field <code>completeContentItemList</code>.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<CMSContentItem> getCompleteContentItemList() {
        if (completeContentItemList == null) {
            generateCompleteContentItemList();
        }
        return completeContentItemList;
    }

    /**
     * <p>
     * hasContentItem.
     * </p>
     *
     * @param itemId a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean hasContentItem(final String itemId) {
        //        logger.trace("template item id: {}", itemId);
        //        for(CMSContentItem item : getContentItems()) {
        //            logger.trace(item.getItemId());
        //            if(item.getItemId().equals(itemId)) {
        //                return true;
        //            }
        //        }
        //        return false;
        return getContentItems().stream().filter(item -> item.getItemId().equals(itemId)).findAny().isPresent();
    }

    /**
     * Generates complete content item list for this page language version. The language version must be added to a CMS page before calling this
     * method!
     */
    public void generateCompleteContentItemList() {
        if (getOwnerPage() == null) {
            throw new IllegalArgumentException("Cannot generate content item list unless this language version is already added to a CMS page");
        }
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
        Collections.sort(completeContentItemList);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return CMSPageLanguageVersion.class.getSimpleName() + ": " + getLanguage();
    }

    /**
     * Adds a new content item from a template item.
     *
     * @param templateItem a {@link io.goobi.viewer.model.cms.CMSContentItem} object.
     * @should return false if content item already in list
     * @return a boolean.
     */
    public boolean addContentItemFromTemplateItem(CMSContentItem templateItem) {
        if (templateItem == null) {
            throw new IllegalArgumentException("templateItem may not be null");
        }
        if (hasContentItem(templateItem.getItemId())) {
            return false;
        }

        CMSContentItem item = new CMSContentItem(templateItem, null);
        if (item.getType().equals(CMSContentItemType.HTML) || item.getType().equals(CMSContentItemType.TEXT)) {
            if (!getLanguage().equals(CMSPage.GLOBAL_LANGUAGE)) {
                addContentItem(item);
                logger.trace("Added new template item '{}' to language version: {}", templateItem.getId(), getLanguage());
                return true;
            }
        } else {
            if (getLanguage().equals(CMSPage.GLOBAL_LANGUAGE)) {
                addContentItem(item);
                logger.trace("Added new template item '{}' to language version: {}", templateItem.getId(), getLanguage());
                return true;
            }
        }

        return false;
    }

    /**
     * <p>
     * addContentItem.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.CMSContentItem} object.
     */
    public void addContentItem(CMSContentItem item) {
        if (item == null) {
            throw new IllegalArgumentException("item may not be null");
        }
        if (item.getItemId() == null) {
            throw new IllegalArgumentException("item.itemId may not be null");
        }

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

    /**
     * <p>
     * removeContentItem.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.CMSContentItem} object.
     */
    public void removeContentItem(CMSContentItem item) {
        contentItems.remove(item);
    }
}
