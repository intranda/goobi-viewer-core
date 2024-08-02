/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
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
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.viewer.BaseMimeType;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.ViewManager;
import io.goobi.viewer.solr.SolrConstants;

/**
 * A class containing all conditions to possibly check for when deciding whether to display a page element for a record page
 */
public class VisibilityCondition {

    private final AnyMatchCondition fileTypes;
    private final Condition sourceFormat;
    private final Condition mimeType;
    private final Condition accessCondition;
    private final Condition views;
    private final Condition docTypes;
    private final Condition numPages;
    private final Condition tocSize;

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
                new AnyMatchCondition<String>(
                        info.getSourceFormat().stream().filter(s -> !s.equals("!")).collect(Collectors.toList()),
                        !info.getSourceFormat().contains("!")),
                new Condition<BaseMimeType>(BaseMimeType.getByName(getValue(info.getBaseMimeType())), !isNegated(info.getBaseMimeType())),
                new Condition<String>(getValue(info.getAccessCondition()), !isNegated(info.getAccessCondition())),
                new AnyMatchCondition<PageType>(
                        info.getPageTypes().stream().filter(s -> !s.equals("!")).map(PageType::getByName).collect(Collectors.toList()),
                        !info.getPageTypes().contains("!")),
                new AnyMatchCondition<String>(info.getDocTypes().stream().filter(s -> !s.equals("!")).collect(Collectors.toList()),
                        !info.getDocTypes().contains("!")),
                ComparisonCondition.of(info.getNumPages()),
                ComparisonCondition.of(info.getTocSize()));
    }

    public VisibilityCondition(AnyMatchCondition<FileType> fileTypes, AnyMatchCondition<String> sourceFormat, Condition<BaseMimeType> mimeType,
            Condition<String> accessCondition, AnyMatchCondition<PageType> views, AnyMatchCondition<String> docTypes, Condition numPages,
            Condition tocSize) {
        this.fileTypes = fileTypes;
        this.sourceFormat = sourceFormat;
        this.mimeType = mimeType.getValue() == null || mimeType.getValue() == BaseMimeType.NONE ? Condition.NONE : mimeType;
        this.accessCondition = StringUtils.isBlank(accessCondition.getValue()) ? Condition.NONE : accessCondition;
        this.views = views;
        this.docTypes = docTypes;
        this.numPages = numPages;
        this.tocSize = tocSize;
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

    @SuppressWarnings("unchecked")
    public boolean matchesRecord(PageType pageType, ViewManager viewManager, HttpServletRequest request, RecordPropertyCache properties)
            throws IndexUnreachableException, DAOException, RecordNotFoundException, PresentationException {

        List<String> docTypes = new ArrayList<>(List.of(viewManager.getTopStructElement().getDocStructType()));
        if (viewManager.getTopStructElement().isGroup()) {
            docTypes.add("group");
        } else if (viewManager.getTopStructElement().isGroupMember()) {
            docTypes.add("groupMember");
        }
        if (viewManager.getTopStructElement().isAnchor()) {
            docTypes.add("anchor");
        } else if (viewManager.getTopStructElement().isAnchorChild()) {
            docTypes.add("volume");
        }
        if (viewManager.getTopStructElement().isWork()) {
            docTypes.add("record");
        }

        Collection<FileType> existingFileTypes = properties.getFileTypesForRecord(viewManager);

        BaseMimeType baseMimeType = BaseMimeType.getByName(viewManager.getTopStructElement().getMetadataValue(SolrConstants.MIMETYPE));
        return checkAccess(viewManager, request, properties)
                && this.fileTypes.matches(existingFileTypes)
                && this.sourceFormat.matches(List.of(viewManager.getTopStructElement().getSourceDocFormat()))
                && this.mimeType.matches(baseMimeType)
                && this.views.matches(List.of(pageType))
                && this.docTypes.matches(docTypes)
                && this.numPages.matches(viewManager.getPageLoader().getNumPages())
                && this.tocSize.matches(viewManager.getToc().getTocElements().size());
    }

    public boolean checkAccess(ViewManager viewManager, HttpServletRequest request, RecordPropertyCache properties)
            throws IndexUnreachableException, DAOException, RecordNotFoundException, PresentationException {
        if (this.accessCondition != Condition.NONE) {
            AccessPermission accessPermission = properties.getPermissionForRecord(viewManager, this.accessCondition.getValue().toString(), request);
            return this.accessCondition.isMatchIfEqual() ? accessPermission.isGranted() : !accessPermission.isGranted();
        }
        return true;
    }

    public boolean matchesPage(PageType pageType, PhysicalElement page, HttpServletRequest request, RecordPropertyCache properties)
            throws IndexUnreachableException, DAOException, RecordNotFoundException, PresentationException {

        Collection<FileType> existingFileTypes = properties.getFileTypesForPage(page);
        BaseMimeType baseMimeType = page.getBaseMimeType();
        return checkAccess(page, request, properties)
                && this.fileTypes.matches(existingFileTypes)
                && this.mimeType.matches(baseMimeType)
                && this.views.matches(List.of(pageType));
    }

    public boolean checkAccess(PhysicalElement page, HttpServletRequest request, RecordPropertyCache properties)
            throws IndexUnreachableException, DAOException, PresentationException, RecordNotFoundException {
        if (this.accessCondition != Condition.NONE) {
            AccessPermission access = properties.getPermissionForPage(page, this.accessCondition.getValue().toString(), request);
            return this.accessCondition.isMatchIfEqual() ? access.isGranted() : !access.isGranted();
        } else {
            return true;
        }
    }

    public Condition getAccessCondition() {
        return accessCondition;
    }

    public AnyMatchCondition getFileTypes() {
        return fileTypes;
    }

    public Condition getDocTypes() {
        return docTypes;
    }

    public Condition getMimeType() {
        return mimeType;
    }

    public Condition getNumPages() {
        return numPages;
    }

    public Condition getSourceFormat() {
        return sourceFormat;
    }

    public Condition getTocSize() {
        return tocSize;
    }

    public Condition getViews() {
        return views;
    }

}
