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
package de.intranda.digiverso.presentation.managedbeans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.messages.Messages;
import de.intranda.digiverso.presentation.model.cms.CMSNavigationItem;
import de.intranda.digiverso.presentation.model.cms.CMSNavigationManager;
import de.intranda.digiverso.presentation.model.cms.SelectableNavigationItem;

@Named
@SessionScoped
public class CmsNavigationBean implements Serializable {

    private static final long serialVersionUID = -8958990090110696045L;

    private static final Logger logger = LoggerFactory.getLogger(CmsNavigationBean.class);

    private String menuItemList = null;
    private CMSNavigationItem selectedNavigationItem = null;
    private CMSNavigationManager itemManager;
    private boolean editMode = false;
    private List<String> selectableThemes = null;

    @PostConstruct
    public void init() {
        itemManager = new CMSNavigationManager(DataManager.getInstance().getConfiguration().getTheme());
    }

    public String getMenuItemList() {
        return menuItemList;
    }

    public void setMenuItemList(String menuItemList) {
        this.menuItemList = menuItemList;
    }

    /**
     * Creates the visible items hiearchy from the string argument
     * @return true if the items could be serialized. False if the item ids don't match any items
     */
    public boolean deserializeMenuItems(String itemString) {
        logger.trace("menu items:\n" + itemString);
        List<CMSNavigationItem> selectedItems = new ArrayList<>();
        String[] ids = itemString.split("\\&?item=");
        int previousLevel = -1;
        CMSNavigationItem previousItem = null;
        for (String id : ids) {
            if (id == null || id.trim().isEmpty()) {
                continue;
            }
            int level = Integer.parseInt(id.substring(id.indexOf('?') + 1));
            id = id.substring(0, id.indexOf('?'));
            Optional<CMSNavigationItem> oItem = getItemManager().getItem(id);
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

    public void saveMenuItems() throws DAOException {
        try {
            if(deserializeMenuItems(getMenuItemList())) {                
                getItemManager().saveVisibleItems(getSelectedTheme());
                getItemManager().reload();
            }
        } catch (ConcurrentModificationException e) {
            logger.error(e.getMessage(), e);
            Messages.error("An error occured: " + e.getClass() + ":\n\t" + e.getMessage());
        }
    }

    public void createNavigationItem() {
        selectedNavigationItem = new CMSNavigationItem();
        selectedNavigationItem.setAbsoluteLink(true);
        selectedNavigationItem.setOrder(-1);
        setEditMode(false);
    }

    public CMSNavigationItem getNavigationItem() {
        if (selectedNavigationItem == null) {
            createNavigationItem();
        }
        return selectedNavigationItem;
    }

    public List<SelectableNavigationItem> getAvailableMenuItems() {
        return getItemManager().getAvailableItems();
    }

    /**
     * 
     * @param theme
     * @return the list from {@link #getVisibleMenuItems()} filtered for items associated with the given theme. Items without theme are associated
     *         with the main theme If the given theme is blank, all items are returned
     */
    public List<CMSNavigationItem> getVisibleMenuItems() {
        return getItemManager().getVisibleItems();
    }

    public void saveNavigationItem() {
        if(deserializeMenuItems(getMenuItemList())) {            
            if (getNavigationItem().getSortingListId() == null) {
                getNavigationItem().setAssociatedTheme(getSelectedTheme());
                getItemManager().addVisibleItem(getNavigationItem());
                selectedNavigationItem = null;
            }
        }
    }

    public CMSNavigationManager getItemManager() {
        return itemManager;
    }

    public void setItemManager(CMSNavigationManager itemManager) {
        this.itemManager = itemManager;
    }

    public boolean isEditMode() {
        return this.editMode;
    }

    /**
     * @param editMode the editMode to set
     */
    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    /**
     * 
     * @return a list of all configured themes for which we may create menus
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public List<String> getSelectableThemes() throws PresentationException, IndexUnreachableException {
        if (selectableThemes == null) {
            selectableThemes = new ArrayList<>(BeanUtils.getCmsBean().getSubThemeDiscriminatorValues());
            selectableThemes.add(0, DataManager.getInstance().getConfiguration().getTheme());
        }
        return selectableThemes;
    }

    /**
     * @return the selectedTheme
     */
    public String getSelectedTheme() {
        return getItemManager().getAssociatedTheme();
    }

    /**
     * @param selectedTheme the selectedTheme to set
     */
    public void setSelectedTheme(String selectedTheme) {
        if(!selectedTheme.equals(getItemManager().getAssociatedTheme())) {            
            setItemManager(new CMSNavigationManager(selectedTheme));
        }
    }

    public void addSelectedItemsToMenu() {
        getItemManager().addSelectedItemsToMenu();
    }
    
}
