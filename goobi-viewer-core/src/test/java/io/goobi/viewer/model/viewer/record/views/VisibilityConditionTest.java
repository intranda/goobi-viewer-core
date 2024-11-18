package io.goobi.viewer.model.viewer.record.views;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.toc.TOC;
import io.goobi.viewer.model.viewer.BaseMimeType;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.ViewManager;
import io.goobi.viewer.model.viewer.pageloader.EagerPageLoader;
import io.goobi.viewer.model.viewer.pageloader.IPageLoader;

class VisibilityConditionTest {

    @Test
    void testReadCondition() {
        VisibilityConditionInfo info = new VisibilityConditionInfo();
        info.setMimeType(List.of("image"));
        VisibilityCondition cond = new VisibilityCondition(info);
        Assertions.assertTrue(cond.getMimeType().matches(List.of(BaseMimeType.IMAGE)));
    }

    @Test
    void testFileTypeCondition() {
        VisibilityConditionInfo info = new VisibilityConditionInfo();
        info.setContentType(List.of("IMAGE"));
        VisibilityCondition cond = new VisibilityCondition(info);
        Assertions.assertTrue(cond.getFileTypes().matches(List.of(FileType.IMAGE)));
        Assertions.assertFalse(cond.getFileTypes().matches(List.of(FileType.EPUB)));
    }

    @Test
    void testReadUnknownCondition() {
        VisibilityConditionInfo info = new VisibilityConditionInfo();
        info.setMimeType(List.of("images"));
        VisibilityCondition cond = new VisibilityCondition(info);
        Assertions.assertFalse(cond.getMimeType().matches(List.of(BaseMimeType.IMAGE)));
    }

    @Test
    void testOtherCondition() {
        VisibilityConditionInfo info = new VisibilityConditionInfo();
        info.setNumPages("2");
        VisibilityCondition cond = new VisibilityCondition(info);
        Assertions.assertTrue(cond.getMimeType().matches(List.of(BaseMimeType.IMAGE)));
        Assertions.assertTrue(cond.getMimeType().matches(List.of(BaseMimeType.APPLICATION)));
        Assertions.assertTrue(cond.getNumPages().matches(312));
    }

    @Test
    void test_matchesRecord_mimeType()
            throws IndexUnreachableException, DAOException, RecordNotFoundException, PresentationException, ViewerConfigurationException {

        RecordPropertyCache cache = new RecordPropertyCache();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        ViewManager viewManager = createRecord();

        VisibilityConditionInfo infoMimeType = new VisibilityConditionInfo();
        infoMimeType.setMimeType(List.of("IMAGE"));
        VisibilityCondition condMimeType = new VisibilityCondition(infoMimeType);
        Assertions.assertTrue(condMimeType.matchesRecord(PageType.viewImage, viewManager, request, cache));
    }

    @Test
    void test_matchesRecord_docType()
            throws IndexUnreachableException, DAOException, RecordNotFoundException, PresentationException, ViewerConfigurationException {

        RecordPropertyCache cache = new RecordPropertyCache();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        ViewManager viewManager = createRecord();

        VisibilityConditionInfo infoDocType = new VisibilityConditionInfo();
        infoDocType.setDocType(List.of("subStruct"));
        VisibilityCondition condDocType = new VisibilityCondition(infoDocType);
        Assertions.assertTrue(condDocType.matchesRecord(PageType.viewImage, viewManager, request, cache));
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
        return viewManager;
    }

}
