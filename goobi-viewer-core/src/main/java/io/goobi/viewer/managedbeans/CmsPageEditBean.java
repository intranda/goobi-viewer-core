package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.widgets.WidgetDisplayElement;

@Named
@ViewScoped
public class CmsPageEditBean implements Serializable {

    private static final long serialVersionUID = 7163586584773468296L;
    
    private Map<WidgetDisplayElement, Boolean> sidebarWidgets;
        
    @Inject
    public CmsPageEditBean(CMSSidebarWidgetsBean widgetsBean) {
        try {
            this.sidebarWidgets = widgetsBean.getAllWidgets().stream().collect(Collectors.toMap(Function.identity(), w -> Boolean.FALSE));
        } catch (DAOException e) {
            this.sidebarWidgets = Collections.EMPTY_MAP;
        }
    }
    
    public Map<WidgetDisplayElement, Boolean> getSidebarWidgets() {
        return sidebarWidgets;
    }
    
    public void setSidebarWidgets(Map<WidgetDisplayElement, Boolean> sidebarWidgets) {
        this.sidebarWidgets = sidebarWidgets;
    }
    
    public List<WidgetDisplayElement> getSelectedWidgets() {
        return this.sidebarWidgets.entrySet().stream().filter(e -> e.getValue()).map(Map.Entry::getKey).collect(Collectors.toList());
    }
    
    public void resetSelectedWidgets() {
        this.sidebarWidgets.entrySet().forEach(e -> e.setValue(false));
    }
    
    public List<WidgetDisplayElement> getAndResetSelectedWidgets() {
        List<WidgetDisplayElement> selected = getSelectedWidgets();
        resetSelectedWidgets();
        return selected;
    }
    

}
