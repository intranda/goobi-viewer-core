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
import java.util.List;
import java.util.stream.Collectors;

import jakarta.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.tabledata.TableDataFilter;
import io.goobi.viewer.model.cms.CMSSlider;
import io.goobi.viewer.model.cms.pages.CMSPage;

/**
 * @author florian
 *
 */
@Named
@ViewScoped
public class CmsSliderBean implements Serializable {

    private static final long serialVersionUID = -2204866565916114208L;

    /**
     * We actually only need a filter String, but we use a complete {@link TableDataFilter} so we can utilize the dataTableColumnFilter component
     */
    private TableDataFilter filter = new TableDataFilter("name_description");

    /**
     *
     * @return an unfiltered list of all persisted sliders
     * @throws DAOException
     */
    public List<CMSSlider> getAllSliders() throws DAOException {
        return getSliders("");
    }

    /**
     *
     * @return all persisted sliders filtered by the current {@link filter}
     * @throws DAOException
     */
    public List<CMSSlider> getSliders() throws DAOException {
        return getSliders(filter.getValue());
    }

    public List<CMSSlider> getSliders(String filter) throws DAOException {
        return DataManager.getInstance()
                .getDao()
                .getAllSliders()
                .stream()
                .filter(slider -> StringUtils.isBlank(filter)
                        || slider.getName().toLowerCase().contains(filter.toLowerCase())
                        || slider.getDescription() != null && slider.getDescription().toLowerCase().contains(filter.toLowerCase()))
                .collect(Collectors.toList());
    }

    public CMSSlider getSlider(long id) throws DAOException {
        return DataManager.getInstance().getDao().getSlider(id);
    }

    public boolean deleteSlider(CMSSlider slider) throws DAOException {
        return DataManager.getInstance().getDao().deleteSlider(slider);
    }

    /**
     * @return the filter
     */
    public TableDataFilter getFilter() {
        return filter;
    }

    public List<CMSPage> getEmbeddingCmsPages(CMSSlider slider) throws DAOException {
        return DataManager.getInstance().getDao().getPagesUsingSlider(slider);

    }
}
