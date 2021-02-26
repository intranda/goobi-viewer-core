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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSCollection;
import io.goobi.viewer.model.cms.CMSSlider;
import io.goobi.viewer.model.cms.CMSSlider.SourceType;
import io.goobi.viewer.model.cms.Selectable;

/**
 * @author florian
 *
 */
@Named
@ViewScoped
public class CmsSliderEditBean implements Serializable {

    private static final long serialVersionUID = -2204866565916114208L;

    private static final Logger logger = LoggerFactory.getLogger(CmsSliderEditBean.class);

    private static final String COLLECTION_FIELD = "DC";
    
    private CMSSlider selectedSlider = null;
    
    private List<Selectable<CMSCategory>> selectableCategories;

    private List<String> dcStrings;



    /**
     * 
     */
    public CmsSliderEditBean() {
        try {
            selectableCategories = BeanUtils.getCmsBean().getAllCategories()
                    .stream().map(cat -> new Selectable<CMSCategory>(cat, false)).collect(Collectors.toList());
        } catch (DAOException e) {
            logger.error("Error getting cms categories", e);
            selectableCategories = Collections.emptyList();
        }
    }
    
    /**
     * @param selectedSlider the selectedSlider to set
     */
    public void setSelectedSlider(CMSSlider selectedSlider) {
        this.selectedSlider = selectedSlider;
        readCategories();
        
    }
    
    /**
     * Set the selected slider via id
     * @throws DAOException 
     */
    public void setSliderId(long id) throws DAOException {
        setSelectedSlider(DataManager.getInstance().getDao().getSlider(id));
    }

    /**
     * @return the selectedSlider
     */
    public CMSSlider getSelectedSlider() {
        return selectedSlider;
    }

    public boolean isNewSlider() {
        return this.selectedSlider == null || this.selectedSlider.getId() == null;
    }
    
    public void createSlider(SourceType type) {
        this.selectedSlider = new CMSSlider(type);
    }

    
    /**
     * Persist the {@link #selectedSlider} to the database and return to slider overview page, ending the current jsf conversation
     */
    public String save() {
        if(this.selectedSlider != null) { 
            try {
            if(this.selectedSlider.getId() != null) {
                DataManager.getInstance().getDao().updateSlider(selectedSlider);
            } else {
                DataManager.getInstance().getDao().addSlider(selectedSlider);
            }
            } catch(DAOException e) {
                logger.error("Error saving slider", e);
                Messages.error("button_save_error");
                return "";
            }
            
        }
        return "pretty:adminCmsSliders";

    }
    
    public String getReturnUrl() {
        return "pretty:adminCmsSliders";
    }

    public List<SourceType> getSourceTypes() {
        return Arrays.asList(SourceType.values());
    }
    
    public List<String> getAvailableCollections() {
        if(dcStrings == null) {            
                dcStrings = DataManager.getInstance().getConfiguration().getConfiguredCollectionFields()
                .stream()
                .flatMap(field -> getCollections(field).stream())
                .map(collection -> collection.getLabel(BeanUtils.getLocale()))
                .sorted()
                .collect(Collectors.toList());
        }
        return dcStrings;
    }
    
    private List<CMSCollection> getCollections(String field) {
        try {
            return DataManager.getInstance().getDao().getCMSCollections(field);
        } catch (DAOException e) {
            logger.error("Error getting collections", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Writes all selected categories of {@link #selectableCategories} to the {@link #selectedSlider} if both exist
     */
    public void writeCategories() {
        if(this.selectableCategories != null && this.selectedSlider != null) {
            this.selectedSlider.setCategories(this.selectableCategories.stream()
                    .filter(Selectable::isSelected)
                    .map(Selectable::getValue)
                    .map(CMSCategory::getName)
                    .collect(Collectors.toList()));
        }
    }
    
    private void readCategories() {
        if(this.selectableCategories != null && this.selectedSlider != null) {
            this.selectableCategories.forEach(selCat -> {
                String catName = selCat.getValue().getName();
                if(this.selectedSlider.getCategories().contains(catName)) {
                    selCat.setSelected(true);
                } else {
                    selCat.setSelected(false);
                }
            });
        }
    }
    
    /**
     * @return the selectableCategories
     */
    public List<Selectable<CMSCategory>> getSelectableCategories() {
        return selectableCategories;
    }
}
