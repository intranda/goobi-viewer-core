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
