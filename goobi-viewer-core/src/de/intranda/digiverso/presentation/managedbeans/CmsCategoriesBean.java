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
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.model.cms.Category;

/**
 * @author florian
 *
 */
public class CmsCategoriesBean implements Serializable {

	private static final long serialVersionUID = 5297975169931740605L;
	
	private static final Logger logger = LoggerFactory.getLogger(CmsCategoriesBean.class);
	
	/**
	 * Value holder for "name" input field.
	 * Must not be empty and not equals another category name for category to be valid
	 */
	private String categoryName = "";
	
	/**
	 * Value holder for "description" input field. Optional
	 */
	private String categoryDescription = "";
	
	/**
	 * A category currently being edited. If null, no category is being edited and a new one can be created
	 */
	private Category selectedCategory = null;

	public boolean isValid() throws DAOException {
		if(StringUtils.isNotBlank(getCategoryName())) {
			Stream<Category> stream = getAllCategories().stream();
			if(getSelectedCategory() != null) {
				stream.filter(cat -> !cat.equals(getSelectedCategory()))
			}
		} else {
			return false;
		}
	}
	
	public List<Category> getAllCategories() throws DAOException {
		return new ArrayList<Category>(DataManager.getInstance().getDao().getAllCategories());
	}
	
	/**
	 * @return the categoryName
	 */
	public String getCategoryName() {
		return categoryName;
	}

	/**
	 * @param categoryName the categoryName to set
	 */
	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	/**
	 * @return the categoryDescription
	 */
	public String getCategoryDescription() {
		return categoryDescription;
	}

	/**
	 * @param categoryDescription the categoryDescription to set
	 */
	public void setCategoryDescription(String categoryDescription) {
		this.categoryDescription = categoryDescription;
	}

	/**
	 * @return the selectedCategory
	 */
	public Category getSelectedCategory() {
		return selectedCategory;
	}
	
	
	

}
