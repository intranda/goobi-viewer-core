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
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.v1.cms.CMSSliderResource;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSSlider;
import io.goobi.viewer.model.cms.CMSSlider.SourceType;
import io.goobi.viewer.model.cms.Selectable;
import io.goobi.viewer.model.cms.collections.CMSCollection;

/**
 * @author florian
 *
 */
@Named
@ViewScoped
public class CmsSliderEditBean implements Serializable {

    private static final long serialVersionUID = -2204866565916114208L;

    private static final Logger logger = LogManager.getLogger(CmsSliderEditBean.class);

    private CMSSlider selectedSlider = null;

    private List<Selectable<CMSCategory>> selectableCategories;

    private List<CMSCollection> cmsCollections;

    private String collectionField;

    /**
     *
     */
    public CmsSliderEditBean() {
        try {
            selectableCategories = BeanUtils.getCmsBean()
                    .getAllCategories()
                    .stream()
                    .map(cat -> new Selectable<CMSCategory>(cat, false))
                    .collect(Collectors.toList());
            collectionField = getAllCollectionFields().get(0);
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
        setSolrField();

    }

    private void setSolrField() {
        if (this.selectedSlider != null && !this.selectedSlider.getCollections().isEmpty()) {
            String solrField = this.selectedSlider.getCollections().get(0);
            solrField = solrField.substring(0, solrField.indexOf("/"));
            if (this.getAllCollectionFields().contains(solrField)) {
                this.collectionField = solrField;
            }

        }
    }

    /**
     * Set the selected slider via id string
     * 
     * @param idString
     * @throws DAOException
     */
    public void setSliderId(String idString) throws DAOException {
        Long id = Long.parseLong(idString);
        setSelectedSlider(DataManager.getInstance().getDao().getSlider(id));
    }

    /**
     * Set the selected slider via id
     * 
     * @param id
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
     * Persist the {@link #selectedSlider} to the database and return to slider overview page, ending the current JSF conversation.
     * 
     * @return Navigation outcome
     */
    public String save() {
        if (this.selectedSlider != null) {
            try {
                if (this.selectedSlider.getStyle() == null) {
                    this.selectedSlider.setStyle("base");
                }
                boolean saved = false;
                if (this.selectedSlider.getId() != null) {
                    saved = DataManager.getInstance().getDao().updateSlider(selectedSlider);
                } else {
                    saved = DataManager.getInstance().getDao().addSlider(selectedSlider);
                }
                if (saved) {
                    Messages.info(null, "button__save__success", "\"" + selectedSlider.getName() + "\"");
                } else {
                    Messages.error("button__save__error");

                }
            } catch (DAOException e) {
                logger.error("Error saving slider", e);
                Messages.error(null, "button__save__error", e.toString());
                return "";
            }

        }
        return "";

    }

    public String getReturnUrl() {
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/admin/cms/slider/";
    }

    public List<SourceType> getSourceTypes() {
        return Arrays.asList(SourceType.values());
    }

    public List<CMSCollection> getAvailableCollections() {
        Locale locale = BeanUtils.getLocale();
        if (cmsCollections == null) {
            cmsCollections = getCollections(collectionField).stream()
                    //                .map(collection -> collection.getLabel(BeanUtils.getLocale()))
                    .sorted((c1, c2) -> ObjectUtils.compare(c1.getLabel(locale), c2.getLabel(locale)))
                    .collect(Collectors.toList());
        }
        return cmsCollections;
    }

    private static List<CMSCollection> getCollections(String field) {
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
        if (this.selectableCategories != null && this.selectedSlider != null) {
            this.selectedSlider.setCategories(this.selectableCategories.stream()
                    .filter(Selectable::isSelected)
                    .map(Selectable::getValue)
                    .map(CMSCategory::getId)
                    .map(Object::toString)
                    .collect(Collectors.toList()));
        }
    }

    private void readCategories() {
        if (this.selectableCategories != null && this.selectedSlider != null) {
            this.selectableCategories.forEach(selCat -> {
                Long catId = selCat.getValue().getId();
                selCat.setSelected(this.selectedSlider.getCategories().contains(catId.toString()));
            });
        }
    }

    /**
     * @return the selectableCategories
     */
    public List<Selectable<CMSCategory>> getSelectableCategories() {
        return selectableCategories;
    }

    public void setStyleFromRequestParameter() {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        String style = params.get("sliderStyle");
        if (StringUtils.isNotBlank(style) && this.selectedSlider != null) {
            selectedSlider.setStyle(style);
        }
    }

    public String getSliderSource() {
        if (this.selectedSlider != null) {
            try {
                List<URI> list = new CMSSliderResource(selectedSlider).getSlides();
                return list.stream().map(URI::toString).collect(Collectors.joining("$"));
            } catch (ContentNotFoundException | IllegalRequestException | PresentationException | IndexUnreachableException e) {
                logger.error("Unable to create slider source: {}", e.toString());
                return "";
            }
        }
        return "";
    }

    /**
     * @return the collectionField
     */
    public String getCollectionField() {
        return collectionField;
    }

    /**
     * @param collectionField the collectionField to set
     */
    public void setCollectionField(String collectionField) {
        this.collectionField = collectionField;
        this.cmsCollections = null;
    }

    public List<String> getAllCollectionFields() {
        return DataManager.getInstance().getConfiguration().getConfiguredCollections();
    }
}
