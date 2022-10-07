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

import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.cms.CMSSlider;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "cms_content_slider")
public class CMSSliderContent extends CMSContent {

    private static final String COMPONENT_NAME = "slider";
   
    
    @JoinColumn(name = "slider_id")
    private CMSSlider slider;
    
    public CMSSliderContent() {
        
    }
    
    private CMSSliderContent(CMSSliderContent orig) {
        this.slider = orig.slider;
    }
    
    @Override
    public String getBackendComponentName() {
        return COMPONENT_NAME;
    }

    public CMSSlider getSlider() {
        return slider;
    }
    
    public void setSlider(CMSSlider slider) {
        this.slider = slider;
    }
    
    @Override
    public CMSContent copy() {
        return new CMSSliderContent(this);
    }

    @Override
    public List<File> exportHtmlFragment(String outputFolderPath, String namingScheme) throws IOException, ViewerConfigurationException {
        return Collections.emptyList();
    }

    @Override
    public String handlePageLoad(boolean resetResults) throws PresentationException {
        return null;
    }

}
