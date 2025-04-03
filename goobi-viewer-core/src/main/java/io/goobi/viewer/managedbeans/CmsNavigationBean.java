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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.cms.CMSNavigationItem;
import io.goobi.viewer.model.cms.CMSNavigationManager;
import io.goobi.viewer.model.cms.SelectableNavigationItem;
import io.goobi.viewer.solr.SolrTools;

/**
 * <p>
 * CmsNavigationBean class.
 * </p>
 */
@Named
@SessionScoped
public class CmsNavigationBean implements Serializable {

    private static final long serialVersionUID = -8958990090110696045L;

    private static final Logger logger = LogManager.getLogger(CmsNavigationBean.class);

    @Inject
    private CmsBean cmsBean;
    @Inject
    private UserBean userBean;

    private String menuItemList = null;
    private CMSNavigationItem selectedNavigationItem = null;
    private CMSNavigationManager itemManager;
    private boolean editMode = false;
    private List<String> selectableThemes = null;

    /**
     * <p>
     * init.
     * </p>
     */
    @PostConstruct
    public void init() {
        try {
            List<String> allowedThemes = getSelectableThemes();
            itemManager = new CMSNavigationManager(allowedThemes.get(0));
        } catch (PresentationException | IndexUnreachableException | IndexOutOfBoundsException e) {
            logger.error(e.toString(), e);
            itemManager = new CMSNavigationManager("");
        }
    }

    /**
     * <p>
     * Getter for the field <code>menuItemList</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMenuItemList() {
        return menuItemList;
    }

    /**
     * <p>
     * Setter for the field <code>menuItemList</code>.
     * </p>
     *
     * @param menuItemList a {@link java.lang.String} object.
     */
    public void setMenuItemList(String menuItemList) {
        this.menuItemList = menuItemList;
    }

    /**
     * Creates the visible items hiearchy from the string argument
     *
     * @return true if the items could be serialized. False if the item ids don't match any items
     * @param itemString a {@link java.lang.String} object.
     */
    public boolean deserializeMenuItems(String itemString) {
        logger.trace("menu items:\n{}", itemString);
        List<CMSNavigationItem> selectedItems = new ArrayList<>();
        String[] ids = itemString.split("\\&?item=");
        int previousLevel = -1;
        CMSNavigationItem previousItem = null;
        for (String id : ids) {
            if (id == null || id.trim().isEmpty()) {
                continue;
            }
            int level = Integer.parseInt(id.substring(id.indexOf('?') + 1));
            String localId = id.substring(0, id.indexOf('?'));
            Optional<CMSNavigationItem> oItem = getItemManager().getItem(localId);
            if (oItem.isPresent()) {
                CMSNavigationItem item = oItem.get();
                item.setAssociatedTheme(getSelectedTheme());
                item.setChildItems(new ArrayList<>());
                item.setParentItem(null);
                if (level == 0 || previousItem == null) {
                    item.setOrder(selectedItems.size());
                    selectedItems.add(item);
                } else {
                    int relativeLevel = previousLevel - level;
                    CMSNavigationItem parent = previousItem;
                    if (relativeLevel >= 0) {
                        for (int i = -1; i < relativeLevel; i++) {
                            if (parent.getParentItem() != null) {
                                parent = parent.getParentItem();
                            }

                        }
                    }
                    item.setOrder(parent.getChildItems().size());
                    item.setParentItem(parent);
                }
                previousLevel = level;
                previousItem = item;
            } else {
                return false;
            }
        }
        getItemManager().setVisibleItems(selectedItems);
        return true;
    }

    /**
     * <p>
     * saveMenuItems.
     * </p>
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void saveMenuItems() throws DAOException {
        try {
            if (deserializeMenuItems(getMenuItemList())) {
                getItemManager().saveVisibleItems(getSelectedTheme());
                getItemManager().reload();
            }
        } catch (ConcurrentModificationException e) {
            logger.error(e.getMessage(), e);
            Messages.error("An error occured: " + e.getClass() + ":\n\t" + e.getMessage());
        }
    }

    /**
     * <p>
     * createNavigationItem.
     * </p>
     */
    public void createNavigationItem() {
        selectedNavigationItem = new CMSNavigationItem();
        selectedNavigationItem.setAbsoluteLink(true);
        selectedNavigationItem.setOrder(-1);
        setEditMode(false);
    }

    /**
     * <p>
     * getNavigationItem.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.cms.CMSNavigationItem} object.
     */
    public CMSNavigationItem getNavigationItem() {
        if (selectedNavigationItem == null) {
            createNavigationItem();
        }
        return selectedNavigationItem;
    }

    /**
     * <p>
     * getAvailableMenuItems.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<SelectableNavigationItem> getAvailableMenuItems() {
        return getItemManager().getAvailableItems();
    }

    /**
     * <p>
     * getVisibleMenuItems.
     * </p>
     *
     * @return the list from {@link #getVisibleMenuItems()} filtered for items associated with the given theme. Items without theme are associated
     *         with the main theme If the given theme is blank, all items are returned
     */
    public List<CMSNavigationItem> getVisibleMenuItems() {
        return getItemManager().getVisibleItems();
    }

    /**
     * <p>
     * saveNavigationItem.
     * </p>
     */
    public void saveNavigationItem() {
        if (deserializeMenuItems(getMenuItemList()) && getNavigationItem().getSortingListId() == null) {
            getNavigationItem().setAssociatedTheme(getSelectedTheme());
            getItemManager().addVisibleItem(getNavigationItem());
            selectedNavigationItem = null;
        }
    }

    /**
     * <p>
     * Getter for the field <code>itemManager</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.cms.CMSNavigationManager} object.
     */
    public CMSNavigationManager getItemManager() {
        return itemManager;
    }

    /**
     * <p>
     * Setter for the field <code>itemManager</code>.
     * </p>
     *
     * @param itemManager a {@link io.goobi.viewer.model.cms.CMSNavigationManager} object.
     */
    public void setItemManager(CMSNavigationManager itemManager) {
        this.itemManager = itemManager;
    }

    /**
     * <p>
     * isEditMode.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isEditMode() {
        return this.editMode;
    }

    /**
     * <p>
     * Setter for the field <code>editMode</code>.
     * </p>
     *
     * @param editMode the editMode to set
     */
    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    /**
     * <p>
     * Getter for the field <code>selectableThemes</code>.
     * </p>
     *
     * @return a list of all configured themes for which we may create menus
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<String> getSelectableThemes() throws PresentationException, IndexUnreachableException {
        if (selectableThemes == null) {
            selectableThemes = new ArrayList<>(cmsBean.getAllowedSubthemeDiscriminatorValues(userBean.getUser()));
            // Add main theme to the list of allowed themes if user either has access to all themes or no subthemes exist in the index
            if ((userBean.getUser() != null && userBean.getUser().hasPrivilegeForAllSubthemeDiscriminatorValues())
                    || SolrTools.getExistingSubthemes().isEmpty()) {
                selectableThemes.add(0, DataManager.getInstance().getConfiguration().getTheme());
            }
        }
        return selectableThemes;
    }

    /**
     * <p>
     * getSelectedTheme.
     * </p>
     *
     * @return the selectedTheme
     */
    public String getSelectedTheme() {
        return getItemManager().getAssociatedTheme();
    }

    /**
     * <p>
     * setSelectedTheme.
     * </p>
     *
     * @param selectedTheme the selectedTheme to set
     */
    public void setSelectedTheme(String selectedTheme) {
        if (!selectedTheme.equals(getItemManager().getAssociatedTheme())) {
            setItemManager(new CMSNavigationManager(selectedTheme));
        }
    }

    /**
     * <p>
     * addSelectedItemsToMenu.
     * </p>
     */
    public void addSelectedItemsToMenu() {
        getItemManager().addSelectedItemsToMenu();
    }

}
