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
package io.goobi.viewer.model.cms.pages.content.types;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import io.goobi.viewer.model.maps.GeoMap;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "cms_content_geomap")
@DiscriminatorValue("geomap")
public class CMSGeomapContent extends CMSContent {

    private static final Logger logger = LogManager.getLogger(CMSGeomapContent.class);

    private static final String COMPONENT_NAME = "geomap";

    @JoinColumn(name = "geomap_id")
    private GeoMap map;

    public CMSGeomapContent() {
        super();
    }

    public CMSGeomapContent(CMSGeomapContent orig) {
        super(orig);
        this.map = orig.map;
    }

    @Override
    public String getBackendComponentName() {
        return COMPONENT_NAME;
    }

    @Override
    public CMSContent copy() {
        return new CMSGeomapContent(this);
    }

    @Override
    public List<File> exportHtmlFragment(String outputFolderPath, String namingScheme) throws IOException, ViewerConfigurationException {
        return Collections.emptyList();
    }

    @Override
    public String handlePageLoad(boolean resetResults, CMSComponent component) throws PresentationException {
        return null;
    }

    public GeoMap getMap() {
        return map;
    }

    public void setMap(GeoMap map) {
        this.map = map;
    }

    public Long getMapId() {
        return Optional.ofNullable(this.map).map(GeoMap::getId).orElse(null);
    }

    public void setMapId(Long id) {
        if (id != null) {
            try {
                this.map = DataManager.getInstance().getDao().getGeoMap(id);
            } catch (DAOException e) {
                logger.error("Error loading map with id {} from database. Error description: {}", id, e.toString());
            }
        } else {
            this.map = null;
        }
    }

    @Override
    public String getData(Integer w, Integer h) {
        return "";
    }

    @Override
    public boolean isEmpty() {
        return this.map == null;
    }
}
