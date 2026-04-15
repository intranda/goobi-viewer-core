package io.goobi.viewer.model.viewer.record.views;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.toc.TOC;
import io.goobi.viewer.model.viewer.MimeType;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.ViewManager;
import io.goobi.viewer.model.viewer.pageloader.EagerPageLoader;
import io.goobi.viewer.model.viewer.pageloader.IPageLoader;
import jakarta.servlet.http.HttpServletRequest;

class VisibilityConditionTest {

    /**
     * @verifies read condition
     */
    @Test
    void getMimeType_shouldReadCondition() {
        VisibilityConditionInfo info = new VisibilityConditionInfo();
        info.setMimeType(List.of("image"));
        VisibilityCondition cond = new VisibilityCondition(info);
        Assertions.assertTrue(cond.getMimeType().matches(List.of("image")));
    }

    /**
     * @verifies file type condition
     */
    @Test
    void getFileTypes_shouldFileTypeCondition() {
        VisibilityConditionInfo info = new VisibilityConditionInfo();
        info.setContentType(List.of("IMAGE"));
        VisibilityCondition cond = new VisibilityCondition(info);
        Assertions.assertTrue(cond.getFileTypes().matches(List.of(FileType.IMAGE)));
        Assertions.assertFalse(cond.getFileTypes().matches(List.of(FileType.EPUB)));
    }

    /**
     * @verifies read unknown condition
     */
    @Test
    void getMimeType_shouldReadUnknownCondition() {
        VisibilityConditionInfo info = new VisibilityConditionInfo();
        info.setMimeType(List.of("images"));
        VisibilityCondition cond = new VisibilityCondition(info);
        Assertions.assertFalse(cond.getMimeType().matches(List.of("image")));
    }

    /**
     * @verifies match any mime type when only numPages is set
     */
    @Test
    void getMimeType_shouldMatchAnyMimeTypeWhenOnlyNumPagesIsSet() {
        VisibilityConditionInfo info = new VisibilityConditionInfo();
        info.setNumPages("2");
        VisibilityCondition cond = new VisibilityCondition(info);
        Assertions.assertTrue(cond.getMimeType().matches(List.of("image")));
        Assertions.assertTrue(cond.getMimeType().matches(List.of("application")));
        Assertions.assertTrue(cond.getNumPages().matches(312));
    }

    /**
     * @verifies mime type
     */
    @Test
    void matchesRecord_shouldMimeType()
            throws IndexUnreachableException, DAOException, RecordNotFoundException, PresentationException, ViewerConfigurationException {

        RecordPropertyCache cache = new RecordPropertyCache();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        ViewManager viewManager = createRecord();

        VisibilityConditionInfo infoMimeType = new VisibilityConditionInfo();
        infoMimeType.setMimeType(List.of("IMAGE"));
        VisibilityCondition condMimeType = new VisibilityCondition(infoMimeType);
        Assertions.assertTrue(condMimeType.matchesRecord(PageType.viewImage, viewManager, request, cache));
    }

    /**
     * @verifies doc type
     */
    @Test
    void matchesRecord_shouldDocType()
            throws IndexUnreachableException, DAOException, RecordNotFoundException, PresentationException, ViewerConfigurationException {

        RecordPropertyCache cache = new RecordPropertyCache();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        ViewManager viewManager = createRecord();

        VisibilityConditionInfo infoDocType = new VisibilityConditionInfo();
        infoDocType.setDocType(List.of("subStruct"));
        VisibilityCondition condDocType = new VisibilityCondition(infoDocType);
        Assertions.assertTrue(condDocType.matchesRecord(PageType.viewImage, viewManager, request, cache));
    }

    /**
     * When the pageType condition does not match the current view, no access-condition check should
     * be performed. Previously, checkAccess() ran before views.matches(), so a Solr query was
     * issued even when the pageType alone would have disqualified the condition.
     * @verifies no access check when page type fails
     */
    @Test
    void matchesRecord_shouldNoAccessCheckWhenPageTypeFails()
            throws IndexUnreachableException, DAOException, RecordNotFoundException, PresentationException, ViewerConfigurationException {

        RecordPropertyCache cache = Mockito.spy(new RecordPropertyCache());
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        ViewManager viewManager = createRecord();

        // Condition requires viewFulltext — but current view is viewObject
        VisibilityConditionInfo info = new VisibilityConditionInfo();
        info.setPageType(List.of("viewFulltext"));
        info.setAccessCondition("VIEW_FULLTEXT");
        VisibilityCondition condition = new VisibilityCondition(info);

        boolean result = condition.matchesRecord(PageType.viewObject, viewManager, request, cache);

        Assertions.assertFalse(result, "Condition should fail because pageType does not match");
        // getPermissionForRecord must NOT have been called since pageType short-circuits first
        Mockito.verify(cache, Mockito.never()).getPermissionForRecord(Mockito.any(), Mockito.any(), Mockito.any());
    }

    /**
     * When no contentType condition is specified, getFileTypesForRecord() must not be called,
     * avoiding an unnecessary Solr query for all page filenames.
     * @verifies no file type query when condition absent
     */
    @Test
    void matchesRecord_shouldNoFileTypeQueryWhenConditionAbsent()
            throws IndexUnreachableException, DAOException, RecordNotFoundException, PresentationException, ViewerConfigurationException {

        RecordPropertyCache cache = Mockito.spy(new RecordPropertyCache());
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        ViewManager viewManager = createRecord();

        // Condition has no contentType — only accessCondition
        VisibilityConditionInfo info = new VisibilityConditionInfo();
        info.setAccessCondition("DOWNLOAD_PDF");
        VisibilityCondition condition = new VisibilityCondition(info);

        // Mock the access permission so we can reach the fileTypes check
        Mockito.doReturn(AccessPermission.granted()).when(cache).getPermissionForRecord(Mockito.any(), Mockito.any(), Mockito.any());

        condition.matchesRecord(PageType.viewObject, viewManager, request, cache);

        // getFileTypesForRecord must NOT have been called since no contentType condition exists
        Mockito.verify(cache, Mockito.never()).getFileTypesForRecord(Mockito.any(), Mockito.anyBoolean());
    }

    protected ViewManager createRecord() throws IndexUnreachableException, PresentationException, DAOException {
        StructElement topStruct = new StructElement("id-topstruct", new SolrDocument(Map.of("MIMETYPE", "image/jpeg")));
        StructElement currentStruct = new StructElement("id-current", new SolrDocument(Map.of("MIMETYPE", "image/jpeg")));
        IPageLoader pageLoader = EagerPageLoader.create(topStruct, false);
        TOC toc = Mockito.mock(TOC.class);
        Mockito.when(toc.getTocElements()).thenReturn(Collections.emptyList());

        ViewManager viewManager = Mockito.mock(ViewManager.class);
        Mockito.when(viewManager.getMimeType()).thenReturn("image/jpeg");
        Mockito.when(viewManager.getTopStructElement()).thenReturn(topStruct);
        Mockito.when(viewManager.getCurrentStructElement()).thenReturn(currentStruct);
        Mockito.when(viewManager.getTopStructElementIddoc()).thenReturn(topStruct.getLuceneId());
        Mockito.when(viewManager.getCurrentStructElementIddoc()).thenReturn(currentStruct.getLuceneId());
        Mockito.when(viewManager.getPageLoader()).thenReturn(pageLoader);
        Mockito.when(viewManager.getToc()).thenReturn(toc);
        Mockito.when(viewManager.getMediaType()).thenReturn(new MimeType("image/jpeg"));
        return viewManager;
    }

}
