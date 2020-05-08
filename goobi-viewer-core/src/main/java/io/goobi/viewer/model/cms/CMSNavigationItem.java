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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.annotations.PrivateOwned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;

/**
 * <p>
 * CMSNavigationItem class.
 * </p>
 */
@Entity
@Table(name = "cms_navigation_items")
public class CMSNavigationItem implements Comparable<CMSNavigationItem> {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(CMSNavigationItem.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cms_navigation_item_id")
    private Long id;

    /**
     * Relative url in the viewer
     */
    @Column(name = "page_url", nullable = false)
    private String pageUrl;

    /**
     * Display name and identifier for NavigationHelper.currentPage Handed to messages for normal items, and called from currentLanguage for CMS-pages
     */
    @Column(name = "item_label", nullable = false)
    private String itemLabel;

    @Column(name = "absolute_link")
    private boolean absoluteLink = false;

    /** Sorting order. */
    @Column(name = "item_order", nullable = false)
    private Integer order;

    /** CMS-generated page this link may refer to */
    @JoinColumn(name = "cms_page_id")
    private CMSPage cmsPage;

    /** Reference to the parent <code>CMSNavigationItem</code>. */
    @ManyToOne
    @JoinColumn(name = "parent_item_id")
    private CMSNavigationItem parentItem;

    @OneToMany(mappedBy = "parentItem", fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
    @OrderBy("order")
    @PrivateOwned
    private List<CMSNavigationItem> childItems = new ArrayList<>();

    @Column(name = "display_rule")
    private DisplayRule displayRule = DisplayRule.ALWAYS;

    /**
     * If not blank, this item is only displayed in the theme/subtheme of this name Only used if {@link #cmsPage} is null. Otherwise
     * {@link CMSPage#getSubThemeDiscriminatorValue()} is used instead
     */
    @Column(name = "associated_theme")
    private String associatedTheme = null;

    @Column(name = "open_in_new_window")
    private boolean openInNewWindow = false;

    /**
     * A temporary id given to an item when adding it to the menu. Used to identify the item in the navigation menu's hierarchy
     */
    @Transient
    private Integer sortingListId = null;

    /**
     * Empty constructor.
     */
    public CMSNavigationItem() {
        // the emptiness inside
    }

    /**
     * Created a copy of the passed item ignoring all data concerning the item hierarchy (order, child and parent items)
     *
     * @param original a {@link io.goobi.viewer.model.cms.CMSNavigationItem} object.
     */
    public CMSNavigationItem(CMSNavigationItem original) {
        setItemLabel(original.getItemLabel());
        setPageUrl(original.getPageUrl());
        setCmsPage(original.getCmsPage());
        setAbsoluteLink(original.isAbsoluteLink());
        setDisplayRule(original.getDisplayRule());
        setOpenInNewWindow(original.isOpenInNewWindow());
        setOrder(original.getOrder());
        setAssociatedTheme(original.getAssociatedTheme());
    }

    /**
     * <p>
     * Constructor for CMSNavigationItem.
     * </p>
     *
     * @param targetUrl a {@link java.lang.String} object.
     * @param label a {@link java.lang.String} object.
     */
    public CMSNavigationItem(String targetUrl, String label) {
        setPageUrl(targetUrl);
        setItemLabel(label);
    }

    /**
     * <p>
     * Constructor for CMSNavigationItem.
     * </p>
     *
     * @param cmsPage a {@link io.goobi.viewer.model.cms.CMSPage} object.
     */
    public CMSNavigationItem(CMSPage cmsPage) {
        setCmsPage(cmsPage);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public int compareTo(CMSNavigationItem o) {
        if (this == o) {
            return 0;
        } else if (getOrder() != null && o.getOrder() != null) {
            return (getOrder() - o.getOrder());
        } else if (getOrder() != null) {
            return -1;
        } else {
            return 1;
        }
    }

    /**
     * <p>
     * Getter for the field <code>id</code>.
     * </p>
     *
     * @return a {@link java.lang.Long} object.
     */
    public Long getId() {
        return id;
    }

    /**
     * <p>
     * Setter for the field <code>id</code>.
     * </p>
     *
     * @param id a {@link java.lang.Long} object.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * <p>
     * Getter for the field <code>itemLabel</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getItemLabel() {
        if (cmsPage != null) {
            return cmsPage.getMenuTitle();
        }
        return itemLabel;
    }

    /**
     * <p>
     * Setter for the field <code>itemLabel</code>.
     * </p>
     *
     * @param itemLabel a {@link java.lang.String} object.
     */
    public void setItemLabel(String itemLabel) {
        this.itemLabel = itemLabel;
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
     * Getter for the field <code>parentItem</code>.
     * </p>
     *
     * @return the parentItem
     */
    public CMSNavigationItem getParentItem() {
        return parentItem;
    }

    /**
     * <p>
     * Setter for the field <code>parentItem</code>.
     * </p>
     *
     * @param parentItem the parentItem to set
     */
    public void setParentItem(CMSNavigationItem parentItem) {
        this.parentItem = parentItem;
        if (parentItem != null) {
            parentItem.addChildItem(this);
        }
    }

    /**
     * <p>
     * Getter for the field <code>childItems</code>.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<CMSNavigationItem> getChildItems() {
        return childItems;
    }

    /**
     * <p>
     * Setter for the field <code>childItems</code>.
     * </p>
     *
     * @param childItems a {@link java.util.List} object.
     */
    public void setChildItems(List<CMSNavigationItem> childItems) {
        this.childItems = childItems;
    }

    /**
     * <p>
     * addChildItem.
     * </p>
     *
     * @param child a {@link io.goobi.viewer.model.cms.CMSNavigationItem} object.
     */
    public void addChildItem(CMSNavigationItem child) {
        if (!childItems.contains(child)) {
            childItems.add(child);
        }
    }

    /**
     * <p>
     * removeChildItem.
     * </p>
     *
     * @param child a {@link io.goobi.viewer.model.cms.CMSNavigationItem} object.
     */
    public void removeChildItem(CMSNavigationItem child) {
        if (childItems.contains(child)) {
            childItems.remove(child);
        }
    }

    /**
     * <p>
     * Getter for the field <code>cmsPage</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.cms.CMSPage} object.
     */
    public CMSPage getCmsPage() {
        return cmsPage;
    }

    /**
     * <p>
     * Setter for the field <code>cmsPage</code>.
     * </p>
     *
     * @param cmsPage a {@link io.goobi.viewer.model.cms.CMSPage} object.
     */
    public void setCmsPage(CMSPage cmsPage) {
        this.cmsPage = cmsPage;
    }

    /**
     * <p>
     * getNavigationUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getNavigationUrl() {
        String url = (isAbsolute(getPageUrl()) || isOnSameRessource(getPageUrl()) ? "" : BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/")
                + getPageUrl();
        //Handle cases where #getPageUrl == '/'
        if(url.endsWith("//")) {
            return url.substring(0, url.length()-1);
        }
        return url;
    }

    /**
     * @param pageUrl2
     * @return
     */
    private static boolean isOnSameRessource(String url) {
        return url.startsWith("#");
    }

    /**
     * @param pageUrl2
     * @return
     */
    private boolean isAbsolute(String url) {
        try {
            URI uri = new URI(url);
            return uri.isAbsolute();
        } catch (URISyntaxException e) {
            logger.warn("Failed to validate url " + pageUrl + ". Assuming it to be an absolute url");
            return true;
        }
    }

    /**
     * <p>
     * Getter for the field <code>pageUrl</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPageUrl() {
        if (cmsPage != null) {
            return cmsPage.getRelativeUrlPath(true);
        }
        return pageUrl;
    }

    /**
     * <p>
     * Setter for the field <code>pageUrl</code>.
     * </p>
     *
     * @param pageUrl a {@link java.lang.String} object.
     */
    public void setPageUrl(String pageUrl) {
        if (StringUtils.isNotBlank(pageUrl) && pageUrl.toLowerCase().startsWith("www.")) {
            pageUrl = "http://" + pageUrl;
        }
        this.pageUrl = pageUrl;
    }

    /**
     * <p>
     * hasUnpublishedCmsPage.
     * </p>
     *
     * @return true if this item has an associated cmsPage and this page's status is unpublished
     */
    public boolean hasUnpublishedCmsPage() {
        return getCmsPage() != null && !getCmsPage().isPublished();
    }

    /**
     * <p>
     * isValid.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isValid() {
        return !hasUnpublishedCmsPage() && !hasDeletedCmsPage();
    }

    /**
     * <p>
     * isShouldDisplay.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isShouldDisplay() {
        UserBean userBean = BeanUtils.getUserBean();
        switch (getDisplayRule()) {
            case ADMIN:
                if (userBean != null) {
                    return userBean.isLoggedIn() && userBean.getUser().isSuperuser();
                }
            case LOGGED_IN:
                if (userBean != null) {
                    return userBean.isLoggedIn();
                }
            case NOT_LOGGED_IN:
                return userBean == null || !userBean.isLoggedIn();
            case ALWAYS:
                return true;
            default:
                return false;
        }
    }

    /**
     * <p>
     * hasDeletedCmsPage.
     * </p>
     *
     * @return true if this item has no associated cmsPage, but the url is that of a cms page or is empty
     */
    public boolean hasDeletedCmsPage() {
        return getCmsPage() == null && (getPageUrl().matches("/?cms/.+/?") || StringUtils.isEmpty(getPageUrl()));
    }

    /**
     * <p>
     * hasCmsPage.
     * </p>
     *
     * @return a boolean.
     */
    public boolean hasCmsPage() {
        return getCmsPage() != null;
    }

    /**
     * Check to highlight this item as 'currentPage'. Checks both the items label and all child labels whether they equal currentPage
     *
     * @param currentPage a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean matchesLabel(String currentPage) {
        if (getItemLabel().equals(currentPage)) {
            return true;
        } else if (childItems != null && !childItems.isEmpty()) {
            for (CMSNavigationItem child : childItems) {
                if (child.matchesLabel(currentPage)) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    /**
     * Returns the hierarchy level of this item, i.e. the number of ancestor items
     *
     * @return a int.
     */
    public int getLevel() {
        int level = 0;
        CMSNavigationItem parent = getParentItem();
        while (parent != null) {
            parent = parent.getParentItem();
            level++;
        }
        return level;
    }

    /**
     * <p>
     * Getter for the field <code>sortingListId</code>.
     * </p>
     *
     * @return the sortingListId
     */
    public Integer getSortingListId() {
        return sortingListId;
    }

    /**
     * <p>
     * Setter for the field <code>sortingListId</code>.
     * </p>
     *
     * @param sortingListId the sortingListId to set
     */
    public void setSortingListId(Integer sortingListId) {
        this.sortingListId = sortingListId;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (other instanceof CMSNavigationItem && ((getParentItem() == null && ((CMSNavigationItem) other).getParentItem() == null)
                || (getParentItem() != null && getParentItem().equals(((CMSNavigationItem) other).getParentItem())))) {
            if (getCmsPage() != null && getCmsPage().equals(((CMSNavigationItem) other).getCmsPage())) {
                return true;
            } else if (getPageUrl().equals(((CMSNavigationItem) other).getPageUrl())
                    && getItemLabel().equals(((CMSNavigationItem) other).getItemLabel())) {
                return true;
            }
        }

        return false;
    }

    /**
     * <p>
     * isAbsoluteLink.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isAbsoluteLink() {
        return absoluteLink;
    }

    /**
     * <p>
     * Setter for the field <code>absoluteLink</code>.
     * </p>
     *
     * @param absoluteLink a boolean.
     */
    public void setAbsoluteLink(boolean absoluteLink) {
        this.absoluteLink = absoluteLink;
    }

    /**
     * <p>
     * isVisible.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isVisible() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append('\n');
        sb.append(getItemLabel());
        sb.append(" (");
        sb.append(getPageUrl());
        sb.append(") order:");
        sb.append(getOrder());
        return sb.toString();
    }

    /**
     * <p>
     * Setter for the field <code>displayRule</code>.
     * </p>
     *
     * @param rule a {@link io.goobi.viewer.model.cms.CMSNavigationItem.DisplayRule} object.
     */
    public void setDisplayRule(DisplayRule rule) {
        this.displayRule = rule;
    }

    /**
     * <p>
     * Getter for the field <code>displayRule</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.cms.CMSNavigationItem.DisplayRule} object.
     */
    public DisplayRule getDisplayRule() {
        if (this.displayRule == null) {
            this.displayRule = DisplayRule.ALWAYS;
        }
        return this.displayRule;
    }

    /**
     * <p>
     * setDisplayForUsersOnly.
     * </p>
     *
     * @param display a boolean.
     */
    public void setDisplayForUsersOnly(boolean display) {
        this.displayRule = display ? DisplayRule.LOGGED_IN : DisplayRule.ALWAYS;
    }

    /**
     * <p>
     * isDisplayForUsersOnly.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayForUsersOnly() {
        return displayRule.equals(DisplayRule.LOGGED_IN) || displayRule.equals(DisplayRule.ADMIN);
    }

    /**
     * <p>
     * setDisplayForAdminsOnly.
     * </p>
     *
     * @param display a boolean.
     */
    public void setDisplayForAdminsOnly(boolean display) {
        this.displayRule = display ? DisplayRule.ADMIN : DisplayRule.ALWAYS;
    }

    /**
     * <p>
     * isDisplayForAdminsOnly.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayForAdminsOnly() {
        return displayRule.equals(DisplayRule.ADMIN);
    }

    /**
     * Sets the {@link #associatedTheme} to the given theme, or to null if the given theme is empty or blank
     *
     * @param associatedTheme the associatedTheme to set
     */
    public void setAssociatedTheme(String associatedTheme) {
        this.associatedTheme = StringUtils.isBlank(associatedTheme) ? null : associatedTheme;
    }

    /**
     * <p>
     * Getter for the field <code>associatedTheme</code>.
     * </p>
     *
     * @return the associatedTheme; null if no associated theme exists
     */
    public String getAssociatedTheme() {
        return Optional.ofNullable(cmsPage)
                .map(page -> page.getSubThemeDiscriminatorValue())
                .map(value -> StringUtils.isBlank(value) ? null : value)
                .orElse(this.associatedTheme);
    }

    /**
     * <p>
     * Setter for the field <code>openInNewWindow</code>.
     * </p>
     *
     * @param openInNewWindow if the link should open in a new tab/window
     */
    public void setOpenInNewWindow(boolean openInNewWindow) {
        this.openInNewWindow = openInNewWindow;
    }

    /**
     * <p>
     * isOpenInNewWindow.
     * </p>
     *
     * @return if the link should open in a new tab/window
     */
    public boolean isOpenInNewWindow() {
        if (StringUtils.isBlank(getPageUrl()) || "#".equals(getPageUrl())) {
            return false;
        } else {
            return openInNewWindow;
        }
    }

    public static enum DisplayRule {
        ALWAYS,
        NOT_LOGGED_IN,
        LOGGED_IN,
        ADMIN;
    }

    /**
     * <p>
     * getMeWithDescendants.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<CMSNavigationItem> getMeWithDescendants() {
        List<CMSNavigationItem> items = new ArrayList<>();
        items.add(this);
        if (getChildItems() != null && !getChildItems().isEmpty()) {
            items.addAll(getChildItems().stream().flatMap(child -> child.getMeWithDescendants().stream()).collect(Collectors.toList()));
        }
        return items;
    }
    
    /**
     * @return true if the item links to a cmsPage and that page has a subtheme associated wih it.
     */
    public boolean isAssociatedWithSubtheme() {
        return Optional.ofNullable(cmsPage).map(CMSPage::getSubThemeDiscriminatorValue).filter(StringUtils::isNotBlank).isPresent();
    }
    
    public String getAssociatedSubtheme() {
        if(cmsPage != null && StringUtils.isNotBlank(cmsPage.getSubThemeDiscriminatorValue())) {
            return ViewerResourceBundle.getTranslation(cmsPage.getSubThemeDiscriminatorValue(), null);
        }
        
        return "";
    }

}
