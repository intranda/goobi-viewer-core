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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.toc.TOC;
import io.goobi.viewer.model.viewer.BaseMimeType;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.ViewManager;
import io.goobi.viewer.solr.SolrConstants;
import jakarta.servlet.http.HttpServletRequest;

/**
 * A class containing all conditions to possibly check for when deciding whether to display a page element for a record page
 */
public class VisibilityCondition {

    private final AnyMatchCondition<FileType> fileTypes;

    private final AnyMatchCondition<String> sourceFormat;

    private final AnyMatchCondition<BaseMimeType> mimeType;

    private final Condition<String> accessCondition;

    private final AnyMatchCondition<PageType> views;

    private final AnyMatchCondition<String> docTypes;

    private final ComparisonCondition<Integer> numPages;

    private final ComparisonCondition<Integer> tocSize;

    public VisibilityCondition(VisibilityConditionInfo info) {
        this(
                new AnyMatchCondition<>(
                        info.getContentType()
                                .stream()
                                .filter(s -> !s.equals("!"))
                                .map(String::toUpperCase)
                                .map(FileType::valueOf)
                                .collect(Collectors.toList()),
                        !info.getContentType().contains("!")),
                new AnyMatchCondition<>(
                        info.getSourceFormat().stream().filter(s -> !s.equals("!")).collect(Collectors.toList()),
                        !info.getSourceFormat().contains("!")),
                new AnyMatchCondition<>(
                        info.getMimeType()
                                .stream()
                                .filter(s -> !s.equals("!"))
                                .map(BaseMimeType::getByName)
                                .filter(type -> type != BaseMimeType.NONE)
                                .toList(),
                        !info.getMimeType().contains("!")),
                new Condition<>(getValue(info.getAccessCondition()), !isNegated(info.getAccessCondition())),
                new AnyMatchCondition<>(
                        info.getPageType().stream().filter(s -> !s.equals("!")).map(PageType::getByName).toList(),
                        !info.getPageType().contains("!")),
                new AnyMatchCondition<>(
                        info.getDocType().stream().filter(s -> !s.equals("!")).toList(),
                        !info.getDocType().contains("!")),
                ComparisonCondition.ofInteger(info.getNumPages()),
                ComparisonCondition.ofInteger(info.getTocSize()));
    }

    public VisibilityCondition(AnyMatchCondition<FileType> fileTypes, AnyMatchCondition<String> sourceFormat,
            AnyMatchCondition<BaseMimeType> mimeType,
            Condition<String> accessCondition, AnyMatchCondition<PageType> views, AnyMatchCondition<String> docTypes,
            ComparisonCondition<Integer> numPages,
            ComparisonCondition<Integer> tocSize) {
        this.fileTypes = fileTypes;
        this.sourceFormat = sourceFormat;
        this.mimeType = mimeType;
        this.accessCondition = accessCondition;
        this.views = views;
        this.docTypes = docTypes;
        this.numPages = numPages;
        this.tocSize = tocSize;
    }

    private static String getValue(String string) {
        if (StringUtils.isNotBlank(string)) {
            return string.replaceFirst("^!", "");
        }
        return "";
    }

    private static boolean isNegated(String string) {
        return StringUtils.isNotBlank(string) && string.startsWith("!");
    }

    public boolean matchesRecord(final PageType pageType, ViewManager viewManager, HttpServletRequest request, RecordPropertyCache properties)
            throws IndexUnreachableException, DAOException, RecordNotFoundException, PresentationException, ViewerConfigurationException {

        if (viewManager == null || viewManager.getTopStructElement() == null) {
            return false;
        }

        List<String> docTypesLocal = new ArrayList<>();

        if (viewManager.getTopStructElement() != null && StringUtils.isNotBlank(viewManager.getTopStructElement().getDocStructType())) {
            docTypesLocal.add(viewManager.getTopStructElement().getDocStructType());
        }
        if (viewManager.getTopStructElement().isGroup()) {
            docTypesLocal.add("group");
        } else if (viewManager.getTopStructElement().isGroupMember()) {
            docTypesLocal.add("groupMember");
        }
        if (viewManager.getTopStructElement().isAnchor()) {
            docTypesLocal.add("anchor");
        } else if (viewManager.getTopStructElement().isAnchorChild()) {
            docTypesLocal.add("volume");
        }
        if (viewManager.getTopStructElement().isWork()) {
            docTypesLocal.add("record");
        }
        if (!Objects.equals(viewManager.getTopStructElementIddoc(), viewManager.getCurrentStructElementIddoc())) {
            docTypesLocal.add("subStruct");
        }

        Collection<FileType> existingFileTypes = properties.getFileTypesForRecord(viewManager, true);

        PageType usePageType = pageType != null ? pageType : PageType.other;
        BaseMimeType baseMimeType = BaseMimeType.getByName(viewManager.getTopStructElement().getMetadataValue(SolrConstants.MIMETYPE));
        return checkAccess(viewManager, request, properties)
                && this.fileTypes.matches(existingFileTypes)
                && this.sourceFormat.matches(Optional.ofNullable(viewManager)
                        .map(ViewManager::getTopStructElement)
                        .map(StructElement::getSourceDocFormat)
                        .map(List::of)
                        .orElse(Collections.emptyList()))
                && this.mimeType.matches(List.of(baseMimeType))
                && this.views.matches(List.of(Optional.ofNullable(usePageType).orElse(PageType.other)))
                && this.docTypes.matches(docTypesLocal)
                && this.numPages.matches(viewManager.getPageLoader().getNumPages())
                && this.tocSize.matches(getToc(viewManager).getTocElements().size());
    }

    protected TOC getToc(ViewManager viewManager)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (viewManager.getToc() == null) {
            viewManager.setToc(createTOC(viewManager));
        }
        return viewManager.getToc();
    }

    private static TOC createTOC(ViewManager viewManager)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        TOC toc = new TOC();
        synchronized (toc) {
            if (viewManager != null) {
                toc.generate(viewManager.getTopStructElement(), viewManager.isListAllVolumesInTOC(), viewManager.getMimeType(),
                        Math.max(1, viewManager.getCurrentImageOrder()));
            }
        }
        return toc;
    }

    public boolean checkAccess(ViewManager viewManager, HttpServletRequest request, RecordPropertyCache properties)
            throws IndexUnreachableException, DAOException, RecordNotFoundException, PresentationException {
        if (!this.accessCondition.isEmpty()) {
            AccessPermission accessPermission = properties.getPermissionForRecord(viewManager, this.accessCondition.getValue(), request);
            return this.accessCondition.isMatchIfEqual() ? accessPermission.isGranted() : !accessPermission.isGranted();
        }
        return true;
    }

    public boolean matchesPage(PageType pageType, PhysicalElement page, HttpServletRequest request, RecordPropertyCache properties)
            throws IndexUnreachableException, DAOException, RecordNotFoundException, PresentationException {

        if (page == null) {
            return false;
        }

        Collection<FileType> existingFileTypes = properties.getFileTypesForPage(page, true);
        BaseMimeType baseMimeType = page.getBaseMimeType();
        return checkAccess(page, request, properties)
                && this.fileTypes.matches(existingFileTypes)
                && this.mimeType.matches(List.of(baseMimeType))
                && this.views.matches(List.of(Optional.ofNullable(pageType).orElse(PageType.other)));
    }

    public boolean checkAccess(PhysicalElement page, HttpServletRequest request, RecordPropertyCache properties)
            throws IndexUnreachableException, DAOException, PresentationException, RecordNotFoundException {
        if (!this.accessCondition.isEmpty()) {
            AccessPermission access = properties.getPermissionForPage(page, this.accessCondition.getValue(), request);
            return this.accessCondition.isMatchIfEqual() ? access.isGranted() : !access.isGranted();
        }
        return true;
    }

    public Condition<String> getAccessCondition() {
        return accessCondition;
    }

    public AnyMatchCondition<FileType> getFileTypes() {
        return fileTypes;
    }

    public AnyMatchCondition<String> getDocTypes() {
        return docTypes;
    }

    public AnyMatchCondition<BaseMimeType> getMimeType() {
        return mimeType;
    }

    public ComparisonCondition<Integer> getNumPages() {
        return numPages;
    }

    public AnyMatchCondition<String> getSourceFormat() {
        return sourceFormat;
    }

    public ComparisonCondition<Integer> getTocSize() {
        return tocSize;
    }

    public AnyMatchCondition<PageType> getViews() {
        return views;
    }

}
