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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

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

import org.apache.commons.lang.StringUtils;
import org.eclipse.persistence.annotations.PrivateOwned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.managedbeans.UserBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;

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

    @Transient
    private Long availableItemId;

    /** Empty constructor. */
    public CMSNavigationItem() {
        // the emptiness inside
    }

    /**
     * Created a copy of the passed item ignoring all data concerning the item hierarchy (order, child and parent items)
     *
     * @param original
     */
    public CMSNavigationItem(CMSNavigationItem original) {
        setItemLabel(original.getItemLabel());
        setPageUrl(original.getPageUrl());
        setCmsPage(original.getCmsPage());
        setAbsoluteLink(original.isAbsoluteLink());
        setDisplayRule(original.getDisplayRule());
    }

    public CMSNavigationItem(String targetUrl, String label) {
        setPageUrl(targetUrl);
        setItemLabel(label);
    }

    public CMSNavigationItem(CMSPage cmsPage) {
        setCmsPage(cmsPage);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(CMSNavigationItem o) {
        if (this == o) {
            return 0;
        }
        return (order - o.getOrder());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItemLabel() {
        if (cmsPage != null) {
            return cmsPage.getMenuTitle();
        }
        return itemLabel;
    }

    public void setItemLabel(String itemLabel) {
        this.itemLabel = itemLabel;
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
     * @return the parentItem
     */
    public CMSNavigationItem getParentItem() {
        return parentItem;
    }

    /**
     * @param parentItem the parentItem to set
     */
    public void setParentItem(CMSNavigationItem parentItem) {
        this.parentItem = parentItem;
        parentItem.addChildItem(this);
    }

    public List<CMSNavigationItem> getChildItems() {
        return childItems;
    }

    public void setChildItems(List<CMSNavigationItem> childItems) {
        this.childItems = childItems;
    }

    public void addChildItem(CMSNavigationItem child) {
        if (!childItems.contains(child)) {
            childItems.add(child);
        }
    }

    public void removeChildItem(CMSNavigationItem child) {
        if (childItems.contains(child)) {
            childItems.remove(child);
        }
    }

    public CMSPage getCmsPage() {
        return cmsPage;
    }

    public void setCmsPage(CMSPage cmsPage) {
        this.cmsPage = cmsPage;
    }

    public String getNavigationUrl() {
        String url = (isAbsolute(getPageUrl()) || isOnSameRessource(getPageUrl()) ? "" : BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/")
                + getPageUrl();
        return url;
    }

    /**
     * @param pageUrl2
     * @return
     */
    private boolean isOnSameRessource(String url) {
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

    public String getPageUrl() {
        if (cmsPage != null) {
            return cmsPage.getRelativeUrlPath(true);
        }
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    /**
     * @return true if this item has an associated cmsPage and this page's status is unpublished
     */
    public boolean hasUnpublishedCmsPage() {
        return getCmsPage() != null && !getCmsPage().isPublished();
    }

    public boolean isValid() {
        return !hasUnpublishedCmsPage() && !hasDeletedCmsPage();
    }

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
     * @return true if this item has no associated cmsPage, but the url is that of a cms page or is empty
     */
    public boolean hasDeletedCmsPage() {
        return getCmsPage() == null && (getPageUrl().matches("/?cms/.+/?") || StringUtils.isEmpty(getPageUrl()));
    }

    public boolean hasCmsPage() {
        return getCmsPage() != null;
    }

    /**
     * Check to highlight this item as 'currentPage'. Checks both the items label and all child labels whether they equal currentPage
     *
     * @param currentPage
     * @return
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
     * @return
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

    public Long getAvailableItemId() {
        return availableItemId;
    }

    public void setAvailableItemId(Long availableItemId) {
        this.availableItemId = availableItemId;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof CMSNavigationItem && ((getParentItem() == null && ((CMSNavigationItem) other).getParentItem() == null)
                || (getParentItem() != null && getParentItem().equals(((CMSNavigationItem) other).getParentItem())))) {
            if (getCmsPage() != null && getCmsPage().equals(((CMSNavigationItem) other).getCmsPage())) {
                return true;
            } else if (getPageUrl().equals(((CMSNavigationItem) other).getPageUrl()) && getItemLabel().equals(((CMSNavigationItem) other)
                    .getItemLabel())) {
                return true;
            }
        }

        return false;
    }

    public boolean isAbsoluteLink() {
        return absoluteLink;
    }

    public void setAbsoluteLink(boolean absoluteLink) {
        this.absoluteLink = absoluteLink;
    }

    public boolean isVisible() {
        return true;
    }

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

    public void setDisplayRule(DisplayRule rule) {
        this.displayRule = rule;
    }

    public DisplayRule getDisplayRule() {
        if (this.displayRule == null) {
            this.displayRule = DisplayRule.ALWAYS;
        }
        return this.displayRule;
    }

    public void setDisplayForUsersOnly(boolean display) {
        this.displayRule = display ? DisplayRule.LOGGED_IN : DisplayRule.ALWAYS;
    }

    public boolean isDisplayForUsersOnly() {
        return displayRule.equals(DisplayRule.LOGGED_IN) || displayRule.equals(DisplayRule.ADMIN);
    }

    public void setDisplayForAdminsOnly(boolean display) {
        this.displayRule = display ? DisplayRule.ADMIN : DisplayRule.ALWAYS;
    }

    public boolean isDisplayForAdminsOnly() {
        return displayRule.equals(DisplayRule.ADMIN);
    }

    public static enum DisplayRule {
        ALWAYS,
        NOT_LOGGED_IN,
        LOGGED_IN,
        ADMIN;
    }

}
