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
package io.goobi.viewer.model.cms.widgets.embed;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.cms.widgets.CustomSidebarWidget;
import io.goobi.viewer.model.translations.TranslatedText;

@Entity
@DiscriminatorValue("CUSTOM")
public class CMSSidebarElementCustom extends CMSSidebarElement {

    @JoinColumn(name = "custom_widget_id")
    private CustomSidebarWidget widget;
    
    public CMSSidebarElementCustom() {
    }
    
    public CMSSidebarElementCustom(CustomSidebarWidget widget, CMSPage owner) {
        super(widget.getType(), owner);
        this.widget = widget;
    }
    
    public CMSSidebarElementCustom(CMSSidebarElementCustom orig, CMSPage owner) {
        super(orig.getContentType(), owner);
        this.widget = orig.widget;
    }

    public CustomSidebarWidget getWidget() {
        return widget;
    }
    
    public void setWidget(CustomSidebarWidget widget) {
        this.widget = widget;
    }
    
    @Override
    public TranslatedText getTitle() {
        return widget.getTitle();
    }
}
