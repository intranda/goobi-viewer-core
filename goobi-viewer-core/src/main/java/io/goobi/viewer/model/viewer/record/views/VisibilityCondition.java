package io.goobi.viewer.model.viewer.record.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.viewer.BaseMimeType;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.ViewManager;
import io.goobi.viewer.solr.SolrConstants;

public class VisibilityCondition {

    private final AnyMatchCondition<FileType> fileTypes;
    private final Condition<String> sourceFormat;
    private final Condition<BaseMimeType> mimeType;
    private final Condition<String> accessCondition;
    private final AnyMatchCondition<PageType> views;
    private final AnyMatchCondition<String> docTypes;
    private final Condition<Boolean> hasPages;

    public VisibilityCondition(VisibilityConditionInfo info) {
        this(
                new AnyMatchCondition<FileType>(
                        info.getRequiredFileTypes()
                                .stream()
                                .filter(s -> !s.equals("!"))
                                .map(String::toUpperCase)
                                .map(FileType::valueOf)
                                .collect(Collectors.toList()),
                        !info.getRequiredFileTypes().contains("!")),
                new Condition<String>(getValue(info.getSourceFormat()), !isNegated(info.getSourceFormat())),
                new Condition<BaseMimeType>(BaseMimeType.getByName(getValue(info.getBaseMimeType())), !isNegated(info.getBaseMimeType())),
                new Condition<String>(getValue(info.getAccessCondition()), !isNegated(info.getAccessCondition())),
                new AnyMatchCondition<PageType>(
                        info.getPageTypes().stream().filter(s -> !s.equals("!")).map(PageType::getByName).collect(Collectors.toList()),
                        !info.getPageTypes().contains("!")),
                new AnyMatchCondition<String>(info.getDocTypes().stream().filter(s -> !s.equals("!")).collect(Collectors.toList()),
                        !info.getDocTypes().contains("!")),
                Condition.of(info.getHasPages(), true));
    }

    public VisibilityCondition(AnyMatchCondition<FileType> fileTypes, Condition<String> sourceFormat, Condition<BaseMimeType> mimeType,
            Condition<String> accessCondition, AnyMatchCondition<PageType> views, AnyMatchCondition<String> docTypes, Condition<Boolean> hasPages) {
        this.fileTypes = fileTypes;
        this.sourceFormat = sourceFormat;
        this.mimeType = mimeType;
        this.accessCondition = accessCondition;
        this.views = views;
        this.docTypes = docTypes;
        this.hasPages = hasPages;
    }

    private static String getValue(String string) {
        if (StringUtils.isNotBlank(string)) {
            return string.replaceFirst("^!", "");
        } else {
            return "";
        }
    }

    private static boolean isNegated(String string) {
        return StringUtils.isNotBlank(string) && string.startsWith("!");
    }

    public boolean matchesRecord(PageType pageType, ViewManager viewManager, HttpServletRequest request)
            throws IndexUnreachableException, DAOException, RecordNotFoundException, PresentationException {

        List<String> docTypes = new ArrayList<>(List.of(viewManager.getTopStructElement().getDocStructType()));
        if (viewManager.getTopStructElement().isGroup()) {
            docTypes.add("group");
        } else if (viewManager.getTopStructElement().isGroupMember()) {
            docTypes.add("groupMember");
        } else if (viewManager.getTopStructElement().isAnchor()) {
            docTypes.add("anchor");
        } else if (viewManager.getTopStructElement().isAnchorChild()) {
            docTypes.add("volume");
        } else if (viewManager.getTopStructElement().isWork()) {
            docTypes.add("record");
        }

        Collection<FileType> existingFileTypes = FileType.containedFiletypes(viewManager);
        BaseMimeType baseMimeType = BaseMimeType.getByName(viewManager.getTopStructElement().getMetadataValue(SolrConstants.MIMETYPE));
        return checkAccess(viewManager, request) &&
                this.fileTypes.matches(existingFileTypes) &&
                this.sourceFormat.matches(viewManager.getTopStructElement().getSourceDocFormat()) &&
                this.mimeType.matches(baseMimeType) &&
                this.views.matches(List.of(pageType)) &&
                this.docTypes.matches(docTypes) &&
                this.hasPages.matches(viewManager.isHasPages());
    }

    public boolean checkAccess(ViewManager viewManager, HttpServletRequest request)
            throws IndexUnreachableException, DAOException, RecordNotFoundException {
        if (this.accessCondition != Condition.NONE) {
            AccessPermission accessPermission =
                    AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(viewManager.getPi(), null, this.accessCondition.getValue(),
                            request);
            return this.accessCondition.isMatchIfEqual() ? accessPermission.isGranted() : !accessPermission.isGranted();
        }
        return true;
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
        return this.fileTypes.matches(existingFileTypes) &&
                this.mimeType.matches(baseMimeType) &&
                this.views.matches(List.of(pageType)) &&
                this.hasPages.matches(true);
    }

}
