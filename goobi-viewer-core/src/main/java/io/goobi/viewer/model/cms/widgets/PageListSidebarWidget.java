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
package io.goobi.viewer.model.cms.widgets;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import io.goobi.viewer.dao.converter.NumberListConverter;
import io.goobi.viewer.model.cms.PageList;
import io.goobi.viewer.model.cms.widgets.type.CustomWidgetType;

@Entity
@DiscriminatorValue("PageListSidebarWidget")
public class PageListSidebarWidget extends CustomSidebarWidget {
    
    @Column(name = "page_ids", nullable = true, columnDefinition = "MEDIUMTEXT")
    @Convert(converter = NumberListConverter.class)
    private List<Long> pageIds = new ArrayList<>();
    
    public PageListSidebarWidget() {
        
    }
    
    public PageListSidebarWidget(PageListSidebarWidget o) {
        super(o);
        this.pageIds = new ArrayList<>(o.pageIds);
    }
    
    public List<Long> getPageIds() {
        return pageIds;
    }
    
    public void setPageIds(List<Long> pageIds) {
        this.pageIds = pageIds;
    }
    
    public PageList getPageList() {
        return new PageList(pageIds);
    }
    
    @Override
    public CustomWidgetType getType() {
        return CustomWidgetType.WIDGET_CMSPAGES;
    }

}
