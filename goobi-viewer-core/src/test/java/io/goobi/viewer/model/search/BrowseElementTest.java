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
package io.goobi.viewer.model.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrConstants.MetadataGroupType;

class BrowseElementTest extends AbstractDatabaseAndSolrEnabledTest {

    @BeforeAll
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();
    }

    /**
     * @see BrowseElement#addSortFieldsToMetadata(StructElement,List)
     * @verifies add sort fields correctly
     */
    @Test
    void addSortFieldsToMetadata_shouldAddSortFieldsCorrectly() throws Exception {
        StructElement se = new StructElement();
        se.setPi("PPN123");
        se.getMetadataFields().put("SORT_FOO", Collections.singletonList("bar"));
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        be.addSortFieldsToMetadata(se, Collections.singletonList(new StringPair("SORT_FOO", "bar")), null);
        assertEquals(1, be.getMetadataList().size());
        assertEquals("SORT_FOO", be.getMetadataList().get(0).getLabel());
        assertEquals(1, be.getMetadataList().get(0).getValues().size());
        assertEquals(1, be.getMetadataList().get(0).getValues().get(0).getParamValues().size());
        assertEquals(1, be.getMetadataList().get(0).getValues().get(0).getParamValues().get(0).size());
        assertEquals("bar", be.getMetadataList().get(0).getValues().get(0).getParamValues().get(0).get(0));
    }

    /**
     * @see BrowseElement#addSortFieldsToMetadata(StructElement,List,Set)
     * @verifies not add fields on ignore list
     */
    @Test
    void addSortFieldsToMetadata_shouldNotAddFieldsOnIgnoreList() throws Exception {
        StructElement se = new StructElement();
        se.setPi("PPN123");
        se.getMetadataFields().put("SORT_FOO", Collections.singletonList("bar"));
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        be.addSortFieldsToMetadata(se, Collections.singletonList(new StringPair("SORT_FOO", "bar")), Collections.singleton("SORT_FOO"));
        assertEquals(0, be.getMetadataList().size());
    }

    /**
     * @see BrowseElement#addSortFieldsToMetadata(StructElement,List)
     * @verifies not add fields already in the list
     */
    @Test
    void addSortFieldsToMetadata_shouldNotAddFieldsAlreadyInTheList() throws Exception {
        StructElement se = new StructElement();
        se.setPi("PPN123");
        se.getMetadataFields().put("SORT_FOO", Collections.singletonList("bar"));

        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        be.getMetadataList().add(new Metadata(String.valueOf(se.getLuceneId()), "MD_FOO", "", "old value"));

        be.addSortFieldsToMetadata(se, Collections.singletonList(new StringPair("SORT_FOO", "bar")), null);
        assertEquals(1, be.getMetadataList().size());
        assertEquals("MD_FOO", be.getMetadataList().get(0).getLabel());
        assertEquals(1, be.getMetadataList().get(0).getValues().size());
        assertEquals(1, be.getMetadataList().get(0).getValues().get(0).getParamValues().size());
        assertEquals(1, be.getMetadataList().get(0).getValues().get(0).getParamValues().get(0).size());
        assertEquals("old value", be.getMetadataList().get(0).getValues().get(0).getParamValues().get(0).get(0));
    }

    /**
     * @see BrowseElement#getMimeTypeFromExtension(String)
     * @verifies return empty string for unknown file extensions
     */
    @Test
    void getMimeTypeFromExtension_shouldReturnEmptyStringForUnknownFileExtensions() {
        assertEquals("", BrowseElement.getMimeTypeFromExtension("file:///opt/digiverso/foo.bar"));
    }

    /**
     * @see BrowseElement#generateDefaultLabel(StructElement)
     * @verifies translate docstruct label
     */
    @Test
    void generateDefaultLabel_shouldTranslateDocstructLabel() {
        //        BrowseElement be = new BrowseElement("PPN123", 1, null, null, Locale.GERMAN, null, null);
        StructElement se = new StructElement();
        se.setDocStructType("Monograph");
        String label = BrowseElement.generateDefaultLabel(se, Locale.GERMAN);
        assertEquals("Monografie", label);
    }

    /**
     * @see BrowseElement#getFulltextForHtml()
     * @verifies remove any line breaks
     */
    @Test
    void getFulltextForHtml_shouldRemoveAnyLineBreaks() {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR", "foo\nbar", Locale.ENGLISH, null, null);
        assertEquals("foo bar", be.getFulltextForHtml());
    }

    /**
     * @see BrowseElement#getFulltextForHtml()
     * @verifies remove any JS
     */
    @Test
    void getFulltextForHtml_shouldRemoveAnyJS() {
        BrowseElement be = new BrowseElement(null, 1, "FROM FOO TO BAR",
                "foo <script type=\"javascript\">\nfunction f {\n alert();\n}\n</script> bar", Locale.ENGLISH, null, null);
        assertEquals("foo  bar", be.getFulltextForHtml());
    }

    @Test
    void test_createMultiLanguageLabel() throws IndexUnreachableException {
        BrowseElement browseElement = new BrowseElement("PI", 0, "bla", "text", Locale.ENGLISH, "/data/1", "url");
        StructElement structElement = new StructElement(new SolrDocument(Map.of(
                SolrConstants.IDDOC, Long.valueOf(12345),
                "MD_TITLE_LANG_DE", "Mein Titel",
                "MD_TITLE_LANG_EN", "My title",
                "MD_TITLE_LANG_FR", "Mon titre")));
        IMetadataValue label = browseElement.createMultiLanguageLabel(structElement);
        assertEquals("Mein Titel", label.getValueOrFallback(Locale.GERMAN));
        assertEquals("My title", label.getValueOrFallback(Locale.ENGLISH));
        assertEquals("Mein Titel", label.getValueOrFallback(Locale.FRENCH)); // French is not among the Faces languages
    }

    // -- Tests for initDocstructHierarchy --

    /**
     * @see BrowseElement(StructElement, ...)
     * @verifies populate struct element hierarchy for a simple work
     */
    @Test
    void initDocstructHierarchy_shouldPopulateStructElements() throws Exception {
        StructElement se = new StructElement(new SolrDocument(Map.of(
                SolrConstants.IDDOC, "1",
                SolrConstants.ISWORK, "true",
                SolrConstants.PI, "PPN_WORK",
                SolrConstants.DOCSTRCT, "Monograph")));
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        assertFalse(be.getStructElements().isEmpty());
        assertEquals("Monograph", be.getStructElements().get(0).getDocStructType());
    }

    // -- Tests for initDocType --

    /**
     * @see BrowseElement(StructElement, ...)
     * @verifies set docType from DOCTYPE field
     */
    @Test
    void initDocType_shouldSetDocType() throws Exception {
        StructElement se = new StructElement();
        se.setPi("PPN123");
        se.getMetadataFields().put(SolrConstants.DOCTYPE, Collections.singletonList("DOCSTRCT"));
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        assertEquals(DocType.DOCSTRCT, be.getDocType());
    }

    /**
     * @see BrowseElement(StructElement, ...)
     * @verifies set metadataGroupType for METADATA doctype
     */
    @Test
    void initDocType_shouldSetMetadataGroupTypeForMetadataDocType() throws Exception {
        StructElement se = new StructElement();
        se.setPi("PPN123");
        se.getMetadataFields().put(SolrConstants.DOCTYPE, Collections.singletonList("METADATA"));
        se.getMetadataFields().put(SolrConstants.METADATATYPE, Collections.singletonList("PERSON"));
        se.getMetadataFields().put(SolrConstants.LABEL, Collections.singletonList("MD_AUTHOR"));
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        assertEquals(DocType.METADATA, be.getDocType());
        assertEquals(MetadataGroupType.PERSON, be.getMetadataGroupType());
        assertEquals("MD_AUTHOR", be.getOriginalFieldName());
    }

    // -- Tests for initCoreFields --

    /**
     * @see BrowseElement(StructElement, ...)
     * @verifies copy core fields from struct element
     */
    @Test
    void initCoreFields_shouldCopyCoreFields() throws Exception {
        StructElement se = new StructElement(new SolrDocument(Map.of(
                SolrConstants.IDDOC, "99",
                SolrConstants.ISWORK, "true",
                SolrConstants.PI, "PPN_CORE",
                SolrConstants.DOCSTRCT, "Monograph",
                SolrConstants.LOGID, "LOG_0001")));
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        assertEquals("PPN_CORE", be.getPi());
        assertEquals("99", be.getIddoc());
        assertEquals("LOG_0001", be.getLogId());
        assertEquals("Monograph", be.getDocStructType());
        assertTrue(be.isWork());
        assertFalse(be.isAnchor());
    }

    /**
     * @see BrowseElement(StructElement, ...)
     * @verifies not generate url if pi is null
     */
    @Test
    void initCoreFields_shouldReturnEarlyIfPiNull() throws Exception {
        StructElement se = new StructElement();
        // No PI set — constructor should return early
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        assertNull(be.getPi());
        assertNull(be.getUrl());
    }

    // -- Tests for resolveMimeType --

    /**
     * @see BrowseElement(StructElement, ...)
     * @verifies resolve mime type from struct element
     */
    @Test
    void resolveMimeType_shouldSetMimeType() throws Exception {
        StructElement se = new StructElement(new SolrDocument(Map.of(
                SolrConstants.IDDOC, "1",
                SolrConstants.ISWORK, "true",
                SolrConstants.PI, "PPN_MIME",
                SolrConstants.DOCSTRCT, "Monograph",
                SolrConstants.MIMETYPE, "image/tiff")));
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        assertTrue(be.isHasImages());
    }

    // -- Tests for resolveImageNo --

    /**
     * @see BrowseElement(StructElement, ...)
     * @verifies resolve image number from ORDER field
     */
    @Test
    void resolveImageNo_shouldUseOrderField() throws Exception {
        StructElement se = new StructElement(new SolrDocument(Map.of(
                SolrConstants.IDDOC, "1",
                SolrConstants.ISWORK, "true",
                SolrConstants.PI, "PPN_IMG",
                SolrConstants.DOCSTRCT, "Monograph",
                SolrConstants.ORDER, "42")));
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        assertEquals(42, be.getImageNo());
    }

    /**
     * @see BrowseElement(StructElement, ...)
     * @verifies resolve image number from THUMBPAGENO field
     */
    @Test
    void resolveImageNo_shouldUseThumbPageNo() throws Exception {
        StructElement se = new StructElement(new SolrDocument(Map.of(
                SolrConstants.IDDOC, "1",
                SolrConstants.ISWORK, "true",
                SolrConstants.PI, "PPN_THUMB",
                SolrConstants.DOCSTRCT, "Monograph",
                SolrConstants.THUMBPAGENO, "7")));
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        assertEquals(7, be.getImageNo());
    }

    /**
     * @see BrowseElement(StructElement, ...)
     * @verifies default image number to 1
     */
    @Test
    void resolveImageNo_shouldDefaultToOne() throws Exception {
        StructElement se = new StructElement(new SolrDocument(Map.of(
                SolrConstants.IDDOC, "1",
                SolrConstants.ISWORK, "true",
                SolrConstants.PI, "PPN_DEF",
                SolrConstants.DOCSTRCT, "Monograph")));
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        assertEquals(1, be.getImageNo());
    }

    // -- Tests for initThumbnail --

    /**
     * @see BrowseElement(StructElement, ...)
     * @verifies not fail if thumbnail handler is null
     */
    @Test
    void initThumbnail_shouldNotFailIfThumbsNull() throws Exception {
        StructElement se = new StructElement(new SolrDocument(Map.of(
                SolrConstants.IDDOC, "1",
                SolrConstants.ISWORK, "true",
                SolrConstants.PI, "PPN_NO_THUMB",
                SolrConstants.DOCSTRCT, "Monograph")));
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        assertNull(be.getThumbnailUrl());
    }

    // -- Tests for initMediaFlags --

    /**
     * @see BrowseElement(StructElement, ...)
     * @verifies set hasImages for image mime type
     */
    @Test
    void initMediaFlags_shouldSetHasImagesForImageMimeType() throws Exception {
        StructElement se = new StructElement(new SolrDocument(Map.of(
                SolrConstants.IDDOC, "1",
                SolrConstants.ISWORK, "true",
                SolrConstants.PI, "PPN_IMG2",
                SolrConstants.DOCSTRCT, "Monograph",
                SolrConstants.MIMETYPE, "image/jpeg")));
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        assertTrue(be.isHasImages());
        assertFalse(be.isHasMedia());
    }

    /**
     * @see BrowseElement(StructElement, ...)
     * @verifies set hasMedia for sandboxed html mime type
     */
    @Test
    void initMediaFlags_shouldSetHasMediaForSandboxedHtml() throws Exception {
        StructElement se = new StructElement();
        se.setPi("PPN123");
        se.setDocStructType("Monograph");
        se.getMetadataFields().put(SolrConstants.MIMETYPE, Collections.singletonList("text/html-sandboxed"));
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        assertFalse(be.isHasImages());
        assertTrue(be.isHasMedia());
    }

    /**
     * @see BrowseElement(StructElement, ...)
     * @verifies detect TEI files
     */
    @Test
    void initMediaFlags_shouldDetectTeiFiles() throws Exception {
        StructElement se = new StructElement();
        se.setPi("PPN123");
        se.getMetadataFields().put(SolrConstants.FILENAME_TEI + "_DE", Collections.singletonList("tei_de.xml"));
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        assertTrue(be.isHasTeiFiles());
    }

    /**
     * @see BrowseElement(StructElement, ...)
     * @verifies set record languages
     */
    @Test
    void initMediaFlags_shouldSetRecordLanguages() throws Exception {
        StructElement se = new StructElement();
        se.setPi("PPN123");
        se.getMetadataFields().put(SolrConstants.LANGUAGE, List.of("de", "en"));
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        assertNotNull(be.getRecordLanguages());
        assertEquals(2, be.getRecordLanguages().size());
        assertTrue(be.getRecordLanguages().contains("de"));
        assertTrue(be.getRecordLanguages().contains("en"));
    }

    /**
     * @see BrowseElement(StructElement, ...)
     * @verifies not set hasImages or hasMedia for unknown mime type
     */
    @Test
    void initMediaFlags_shouldNotSetFlagsForUnknownMimeType() throws Exception {
        StructElement se = new StructElement();
        se.setPi("PPN123");
        se.getMetadataFields().put(SolrConstants.MIMETYPE, Collections.singletonList("application/octet-stream"));
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        assertFalse(be.isHasImages());
        assertFalse(be.isHasMedia());
        assertEquals(PageType.viewMetadata, be.determinePageType());
    }

    /**
     * PDF record without image derivatives should resolve to metadata view.
     *
     * @see BrowseElement#determinePageType()
     * @verifies return metadata page type for pdf mime type without images
     */
    @Test
    void determinePageType_shouldReturnMetadataForPdfWithoutImages() throws Exception {
        StructElement se = new StructElement();
        se.setPi("PPN123");
        se.setDocStructType("Monograph");
        se.getMetadataFields().put(SolrConstants.MIMETYPE, Collections.singletonList("application/pdf"));
        // hasImages defaults to false in StructElement(), matching BOOL_IMAGEAVAILABLE=false
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        assertEquals(false, be.isHasImages());
        assertEquals(false, be.isHasMedia());
        assertEquals(PageType.viewMetadata, be.determinePageType());
    }

    /**
     * PDF record with image derivatives (BOOL_IMAGEAVAILABLE=true) should resolve to object view.
     *
     * @see BrowseElement#determinePageType()
     * @verifies return object page type for pdf mime type with images
     */
    @Test
    void determinePageType_shouldReturnObjectForPdfWithImages() throws Exception {
        StructElement se = new StructElement(new SolrDocument(Map.of(
                SolrConstants.IDDOC, "12345",
                SolrConstants.ISWORK, "true",
                SolrConstants.PI, "PPN123",
                SolrConstants.DOCSTRCT, "Monograph",
                SolrConstants.MIMETYPE, "application/pdf",
                SolrConstants.BOOL_IMAGEAVAILABLE, "true")));
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        assertEquals(true, be.isHasImages());
        assertEquals(false, be.isHasMedia());
        assertEquals(PageType.viewObject, be.determinePageType());
    }

    /**
     * Audio record should still resolve to object view via hasMedia.
     *
     * @see BrowseElement#determinePageType()
     * @verifies return object page type for audio mime type
     */
    @Test
    void determinePageType_shouldReturnObjectForAudio() throws Exception {
        StructElement se = new StructElement();
        se.setPi("PPN123");
        se.setDocStructType("Monograph");
        se.getMetadataFields().put(SolrConstants.MIMETYPE, Collections.singletonList("audio/mpeg"));
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        assertEquals(false, be.isHasImages());
        assertEquals(true, be.isHasMedia());
        assertEquals(PageType.viewObject, be.determinePageType());
    }

    /**
     * Video record should still resolve to object view via hasMedia.
     *
     * @see BrowseElement#determinePageType()
     * @verifies return object page type for video mime type
     */
    @Test
    void determinePageType_shouldReturnObjectForVideo() throws Exception {
        StructElement se = new StructElement();
        se.setPi("PPN123");
        se.setDocStructType("Monograph");
        se.getMetadataFields().put(SolrConstants.MIMETYPE, Collections.singletonList("video/mp4"));
        BrowseElement be = new BrowseElement(se, Collections.singletonMap(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, new ArrayList<>()),
                Locale.ENGLISH, null, null, null);
        assertEquals(false, be.isHasImages());
        assertEquals(true, be.isHasMedia());
        assertEquals(PageType.viewObject, be.determinePageType());
    }
}
