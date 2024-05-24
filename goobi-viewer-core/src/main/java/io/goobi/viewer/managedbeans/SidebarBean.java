package io.goobi.viewer.managedbeans;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.viewer.record.views.VisibilityCondition;

public class SidebarBean {

    @Inject
    protected ActiveDocumentBean activeDocumentBean;
    @Inject
    protected NavigationHelper navigationHelper;
    @Inject
    private HttpServletRequest httpRequest;

    public boolean isVisibleForRecord(VisibilityCondition condition) throws IndexUnreachableException, DAOException, RecordNotFoundException {
        if (activeDocumentBean != null && activeDocumentBean.isRecordLoaded()) {
            return condition.matchesRecord(navigationHelper.getCurrentPageType(), activeDocumentBean.getViewManager(), httpRequest);
        }
        return false;
    }

    public boolean isVisibleForPage(VisibilityCondition condition) throws IndexUnreachableException, DAOException, RecordNotFoundException {
        if (activeDocumentBean != null && activeDocumentBean.isRecordLoaded()) {
            return condition.matchesPage(navigationHelper.getCurrentPageType(), activeDocumentBean.getViewManager().getCurrentPage(), httpRequest);
        }
        return false;
    }

}
