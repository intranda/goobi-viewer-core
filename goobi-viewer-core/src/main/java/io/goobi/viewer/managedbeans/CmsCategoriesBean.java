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
import java.util.List;
import java.util.stream.Stream;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.persistence.RollbackException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.cms.CMSCategory;

/**
 * Managed Bean for editing, deleting and creating {@link CMSCategory categories}.
 *
 * @author Florian Alpers
 */
@Named("cmsCategoriesBean")
@SessionScoped
public class CmsCategoriesBean implements Serializable {

    private static final long serialVersionUID = 5297975169931740605L;

    private static final Logger logger = LogManager.getLogger(CmsCategoriesBean.class);

    /**
     * Value holder for "name" input field. Must not be empty and not equals another category name for category to be valid
     */
    private String categoryName = "";

    /**
     * Value holder for "description" input field. Optional
     */
    private String categoryDescription = "";

    /**
     * A category currently being edited. If null, no category is being edited and a new one can be created
     */
    private CMSCategory selectedCategory = null;

    /**
     * Check if {@link #getCategoryName()} is empty or equal (ignoring case) to the name of any existing category. If we are editing a category,
     * obviously ignore this category for the check
     *
     * @return true if {@link #getCategoryName()} returns a valid name for a category
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isValid() throws DAOException {
        if (StringUtils.isNotBlank(getCategoryName())) {
            Stream<CMSCategory> stream = getAllCategories().stream();
            if (getSelectedCategory() != null) {
                stream = stream.filter(cat -> !getSelectedCategory().equals(cat));
            }
            return stream.noneMatch(cat -> cat.getName().equalsIgnoreCase(getCategoryName()));
        }

        return false;
    }

    /**
     * Start editing the given category. Editing will continue until either {@link #saveCategoryAction()} or {@link #cancelAction()} is executed
     *
     * @param category The category to edit
     */
    public void edit(CMSCategory category) {
        this.selectedCategory = category;
        this.setCategoryName(category.getName());
        this.setCategoryDescription(category.getDescription());
    }

    /**
     * If editing mode is active, set categoryName and categoryDescription to the currently selected category, persist it and end the editing mode.
     *
     * <p>Otherwise, if {@link #isValid()} is true, create a new category based on {@link #getCategoryName()} and
     * {@link io.goobi.viewer.managedbeans.CmsCategoriesBean#getCategoryDescription()} and persist it. Also clear categoryName and
     * categoryDescription.
     *
     * @return Navigation outcome
     */
    public String saveCategoryAction() {
        try {
            if (isValid()) {
                if (isEditing()) {
                    this.selectedCategory.setName(getCategoryName());
                    this.selectedCategory.setDescription(getCategoryDescription());
                    DataManager.getInstance().getDao().updateCategory(this.selectedCategory);
                    Messages.info("updatedSuccessfully");
                } else {
                    CMSCategory category = new CMSCategory();
                    category.setName(getCategoryName());
                    category.setDescription(getCategoryDescription());
                    DataManager.getInstance().getDao().addCategory(category);
                    Messages.info("addedSuccessfully");
                }

                BeanUtils.getCmsMediaBean().resetData();
                endEditing();
                return "pretty:adminCmsCategories";
            }
        } catch (DAOException e) {
            logger.error(e.getMessage(), e);
            Messages.error("errSave");
        }

        if (isEditing()) {
            return "pretty:adminCmsEditCategory";
        }

        return "pretty:adminCmsNewCategory";
    }

    /**
     * Delete the given Category in DAO. Also clear categoryName and categoryDescription
     *
     * @param category category to delete from the database
     * @return Navigation outcome
     */
    public String deleteCategoryAction(CMSCategory category) {
        if (getSelectedCategory() != null && getSelectedCategory().equals(category)) {
            endEditing();
        }
        try {
            DataManager.getInstance().getDao().deleteCategory(category);
            Messages.info("admin__category_delete_success");
        } catch (RollbackException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("cannot delete or update a parent row")) {
                Messages.error("admin__category_delete_error_inuse");
            } else {
                logger.error("Error deleting category ", e);
                Messages.error("admin__category_delete_error");
            }
        } catch (DAOException e) {
            logger.error("Error deleting category ", e);
            Messages.error("admin__category_delete_error");

        }

        return "pretty:adminCmsCategories";
    }

    /**
     * End the editing mode if active without persisting anything. Also clear categoryName and categoryDescription
     *
     * @return Navigation outcome
     */
    public String cancelAction() {
        endEditing();
        return "pretty:adminCmsCategories";
    }

    /**
     * Check is we are currently editing an existing category, i.e. {@link #getSelectedCategory} doesn't return null
     *
     * @return true if we are currently editing an existing category, i.e. {@link #getSelectedCategory} doesn't return null
     */
    public boolean isEditing() {
        return getSelectedCategory() != null;
    }

    /**
     * <p>endEditing.
     */
    public void endEditing() {
        this.selectedCategory = null;
        setCategoryName("");
        setCategoryDescription("");
    }

    /**
     * Returns a newly created list of all saved categories.
     *
     * @return a newly created list of all saved categories
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSCategory> getAllCategories() throws DAOException {
        return new ArrayList<>(DataManager.getInstance().getDao().getAllCategories());
    }

    /**
     * Getter for the field <code>categoryName</code>.
     *
     * @return the name entered for the new or edited category
     */
    public String getCategoryName() {
        return categoryName;
    }

    /**
     * Setter for the field <code>categoryName</code>.
     *
     * @param categoryName name entered for the new or edited category
     */
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    /**
     * Getter for the field <code>categoryDescription</code>.
     *
     * @return the description entered for the new or edited category
     */
    public String getCategoryDescription() {
        return categoryDescription;
    }

    /**
     * Setter for the field <code>categoryDescription</code>.
     *
     * @param categoryDescription description entered for the new or edited category
     */
    public void setCategoryDescription(String categoryDescription) {
        this.categoryDescription = categoryDescription;
    }

    /**
     * Getter for the field <code>selectedCategory</code>.
     *
     * @return the currently selected CMS category, or null if none is selected
     */
    public CMSCategory getSelectedCategory() {
        return selectedCategory;
    }

    /**
     * <p>getSelectedCategoryId.
     *
     * @return ID of the selected category
     */
    public Long getSelectedCategoryId() {
        if (selectedCategory != null) {
            return selectedCategory.getId();
        }

        return null;
    }

    /**
     * <p>setSelectedCategoryId.
     *
     * @param id database ID of the category to edit
     * @throws io.goobi.viewer.exceptions.DAOException
     */
    public void setSelectedCategoryId(Long id) throws DAOException {
        edit(DataManager.getInstance().getDao().getCategory(id));
    }
}
