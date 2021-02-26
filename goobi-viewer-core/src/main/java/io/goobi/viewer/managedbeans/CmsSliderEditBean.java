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
import java.util.List;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.org.apache.bcel.internal.generic.NEW;

import io.goobi.viewer.model.cms.CMSSlider;
import io.goobi.viewer.model.cms.CMSSlider.SourceType;

/**
 * @author florian
 *
 */
@Named
@ConversationScoped
public class CmsSliderEditBean implements Serializable {

    private static final long serialVersionUID = -2204866565916114208L;

    private static final Logger logger = LoggerFactory.getLogger(CmsSliderEditBean.class);

    private CMSSlider selectedSlider = null;

    /**
     * Manages the conversation scope. Starts in adminCmsSliderSelectType and continues with adminCmsSliderEdit to keep the selected slider type
     */
    @Inject
    private Conversation conversation;

    /**
     * @param selectedSlider the selectedSlider to set
     */
    public void setSelectedSlider(CMSSlider selectedSlider) {
        this.selectedSlider = selectedSlider;
    }

    /**
     * @return the selectedSlider
     */
    public CMSSlider getSelectedSlider() {
        return selectedSlider;
    }

    /**
     * Conversation should start when entering the adminCmsSliderSelectType page
     */
    public void startConversation() {
        if (!FacesContext.getCurrentInstance().isPostback() && conversation.isTransient()) {
            conversation.begin();
        }
    }
    
    /**
     * End the conversation upon leaving adminCmsSliderEdit
     */
    public void endConversation() {
        if(!conversation.isTransient()){
            conversation.end();
        }
    }
    
    public boolean isNewSlider() {
        return this.selectedSlider == null || this.selectedSlider.getId() == null;
    }
    
    public void createSlider(SourceType type) {
        this.selectedSlider = new CMSSlider(type);
    }

    public List<SourceType> getSourceTypes() {
        return Arrays.asList(SourceType.values());
    }
}
