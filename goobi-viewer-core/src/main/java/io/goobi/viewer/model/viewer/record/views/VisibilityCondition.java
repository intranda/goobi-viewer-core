package io.goobi.viewer.model.viewer.record.views;

import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.viewer.BaseMimeType;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.ViewManager;
import io.goobi.viewer.solr.SolrConstants;

public enum VisibilityCondition {

    ALTO(
            new AnyMatchCondition<FileType>(List.of(FileType.ALTO), true),
            Condition.NONE,
            new Condition<String>(IPrivilegeHolder.PRIV_VIEW_FULLTEXT, true),
            new AnyMatchCondition<PageType>(List.of(PageType.viewFulltext, PageType.viewFullscreen), true),
            Condition.NONE);

    private final AnyMatchCondition<FileType> requiredFileTypes;
    private final Condition<BaseMimeType> mimeType;
    private final Condition<String> accessCondition;
    private final AnyMatchCondition<PageType> views;
    private final Condition<Boolean> hasPages;

    private VisibilityCondition(AnyMatchCondition<FileType> requiredFileTypes, Condition<BaseMimeType> mimeType,
            Condition<String> accessCondition, AnyMatchCondition<PageType> views, Condition<Boolean> hasPages) {
        this.requiredFileTypes = requiredFileTypes;
        this.mimeType = mimeType;
        this.accessCondition = accessCondition;
        this.views = views;
        this.hasPages = hasPages;
    }

    public boolean matchesRecord(PageType pageType, ViewManager viewManager, HttpServletRequest request)
            throws IndexUnreachableException, DAOException, RecordNotFoundException {

        if (this.accessCondition != Condition.NONE) {
            AccessPermission access =
                    AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(viewManager.getPi(), null, this.accessCondition.getValue(),
                            request);
            return this.accessCondition.isMatchIfEqual() ? access.isGranted() : !access.isGranted();
        }

        Collection<FileType> existingFileTypes = FileType.containedFiletypes(viewManager);
        BaseMimeType baseMimeType = BaseMimeType.getByName(viewManager.getTopStructElement().getMetadataValue(SolrConstants.MIMETYPE));
        return this.requiredFileTypes.matches(existingFileTypes) &&
                this.mimeType.matches(baseMimeType) &&
                this.views.matches(List.of(pageType)) &&
                this.hasPages.matches(viewManager.isHasPages());
    }

    public boolean matchesPage(PageType pageType, PhysicalElement page, HttpServletRequest request)
            throws IndexUnreachableException, DAOException, RecordNotFoundException {

        if (this.accessCondition != Condition.NONE) {
            AccessPermission access =
                    AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, page.getPi(), page.getFileName(),
                            this.accessCondition.getValue());
            return this.accessCondition.isMatchIfEqual() ? access.isGranted() : !access.isGranted();
        }

        Collection<FileType> existingFileTypes = FileType.containedFiletypes(page);
        BaseMimeType baseMimeType = page.getBaseMimeType();
        return this.requiredFileTypes.matches(existingFileTypes) &&
                this.mimeType.matches(baseMimeType) &&
                this.views.matches(List.of(pageType)) &&
                this.hasPages.matches(true);
    }

}
