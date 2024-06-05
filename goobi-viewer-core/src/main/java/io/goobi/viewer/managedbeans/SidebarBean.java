package io.goobi.viewer.managedbeans;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UINamingContainer;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.json.JsonStringConverter;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.viewer.record.views.VisibilityCondition;
import io.goobi.viewer.model.viewer.record.views.VisibilityConditionInfo;

@Named
@SessionScoped
public class SidebarBean implements Serializable {

    private static final long serialVersionUID = 6193053985791285569L;
    @Inject
    protected ActiveDocumentBean activeDocumentBean;
    @Inject
    protected NavigationHelper navigationHelper;
    @Inject
    private HttpServletRequest httpRequest;

    public boolean checkVisibilityForRecord(String json) throws IOException, IndexUnreachableException, DAOException, RecordNotFoundException {
        json = json.replaceAll(":\\s*!\\[", ":[!,");
        VisibilityConditionInfo info = JsonStringConverter.of(VisibilityConditionInfo.class).convert(json);
        VisibilityCondition condition = new VisibilityCondition(info);
        boolean visible = condition.matchesRecord(navigationHelper.getCurrentPageType(), activeDocumentBean.getViewManager(), httpRequest);
        return visible;
    }

    public boolean checkVisibilityForPage(String json) throws IOException, IndexUnreachableException, DAOException, RecordNotFoundException {
        json = json.replaceAll(":\\s*![", ":[!,");
        VisibilityConditionInfo info = JsonStringConverter.of(VisibilityConditionInfo.class).convert(json);
        VisibilityCondition condition = new VisibilityCondition(info);
        return condition.matchesPage(navigationHelper.getCurrentPageType(), activeDocumentBean.getViewManager().getCurrentPage(), httpRequest);
    }

    public long getChildCount(UINamingContainer component, String styleClass) {
        return getDescendants(component.getFacets()
                .get("javax.faces.component.COMPOSITE_FACET_NAME"))
                        .stream()
                        .filter(child -> child.isRendered())
                        .filter(child -> StringUtils.isNotBlank(styleClass) ? hasStyleClass(child, styleClass) : true)
                        .count();
    }

    private List<UIComponent> getDescendants(UIComponent container) {
        List<UIComponent> descs = new ArrayList<UIComponent>();
        for (UIComponent child : container.getChildren()) {
            descs.add(child);
            descs.addAll(getDescendants(child));
        }
        return descs;
    }

    private boolean hasStyleClass(UIComponent c, String styleClass) {
        //        Object data = c.getAttributes().get("data-test");
        Object styles = c.getAttributes().get("visibilty-type");
        if (styles instanceof Collection) {
            return ((Collection) styles).contains(styleClass);
        } else if (styles != null) {
            return styles.toString().contains(styleClass);
        } else {
            return false;
        }
    }

}
