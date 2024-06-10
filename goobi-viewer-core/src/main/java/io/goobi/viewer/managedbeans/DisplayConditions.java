package io.goobi.viewer.managedbeans;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.json.JsonStringConverter;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.viewer.record.views.VisibilityCondition;
import io.goobi.viewer.model.viewer.record.views.VisibilityConditionInfo;

@Named
@SessionScoped
public class DisplayConditions implements Serializable {

    private static final long serialVersionUID = 6193053985791285569L;
    @Inject
    protected ActiveDocumentBean activeDocumentBean;
    @Inject
    protected NavigationHelper navigationHelper;
    @Inject
    private HttpServletRequest httpRequest;

    public boolean matchRecord(String json)
            throws IOException, IndexUnreachableException, DAOException, RecordNotFoundException, PresentationException {
        json = json.replaceAll(":\\s*!\\[", ":[!,");
        VisibilityConditionInfo info = JsonStringConverter.of(VisibilityConditionInfo.class).convert(json);
        VisibilityCondition condition = new VisibilityCondition(info);
        return condition.matchesRecord(navigationHelper.getCurrentPageType(), activeDocumentBean.getViewManager(), httpRequest);
    }

    public boolean matchPage(String json) throws IOException, IndexUnreachableException, DAOException, RecordNotFoundException {
        json = json.replaceAll(":\\s*![", ":[!,");
        VisibilityConditionInfo info = JsonStringConverter.of(VisibilityConditionInfo.class).convert(json);
        VisibilityCondition condition = new VisibilityCondition(info);
        return condition.matchesPage(navigationHelper.getCurrentPageType(), activeDocumentBean.getViewManager().getCurrentPage(), httpRequest);
    }

    public UIComponentHelper getTag(String id) {
        return UIComponentHelper.getCurrentComponent().getChild(id);
    }

    public static class UIComponentHelper {

        private final UIComponent component;

        public static UIComponentHelper getCurrentComponent() {
            return new UIComponentHelper(UIComponent.getCurrentComponent(FacesContext.getCurrentInstance()));
        }

        public UIComponentHelper(UIComponent component) {
            this.component = component;
        }

        public UIComponent getComponent() {
            return component;
        }

        public Long getChildCount(String visibilityClass) {
            return getDescendants(this.component)
                    .stream()
                    .filter(child -> child.isRendered())
                    .filter(child -> StringUtils.isNotBlank(visibilityClass) ? hasVisibilityTag(child, visibilityClass) : true)
                    .count();
        }

        public UIComponentHelper getChild(String id) {
            if (StringUtils.isNotBlank(id)) {
                return this.getDescendants(this.component)
                        .stream()
                        .filter(child -> id.equals(child.getId()))
                        .findAny()
                        .map(UIComponentHelper::new)
                        .orElse(null);
            } else {
                throw new IllegalArgumentException("Must pass a non-null value for id of descendant you want to find");
            }

        }

        private List<UIComponent> getDescendants(UIComponent container) {
            List<UIComponent> descs = new ArrayList<UIComponent>();
            for (UIComponent child : container.getChildren()) {
                descs.add(child);
                descs.addAll(getDescendants(child));
            }
            return descs;
        }

        private boolean hasVisibilityTag(UIComponent c, String visibilityClass) {
            Object styles = c.getAttributes().get("visibilty-class");
            if (styles instanceof Collection) {
                return ((Collection) styles).contains(visibilityClass);
            } else if (styles != null) {
                return styles.toString().contains(visibilityClass);
            } else {
                return false;
            }
        }

    }

}
