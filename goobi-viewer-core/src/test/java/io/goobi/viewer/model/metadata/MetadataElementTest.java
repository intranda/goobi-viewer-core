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
package io.goobi.viewer.model.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.metadata.MetadataParameter.MetadataParameterType;
import io.goobi.viewer.model.viewer.StructElement;

// Extends AbstractDatabaseAndSolrEnabledTest because MetadataElement.init() calls
// StructElementStub.getUrl() → DefaultURLBuilder.buildPageUrl() → JPADAO.getCMSPageDefaultViewForRecord()
class MetadataElementTest extends AbstractDatabaseAndSolrEnabledTest {

    /**
     * @see MetadataElement#isSkip()
     * @verifies return true if metadata list empty
     */
    @Test
    void isSkip_shouldReturnTrueIfMetadataListEmpty() throws Exception {
        MetadataElement me = new MetadataElement();
        Assertions.assertTrue(me.isSkip());

    }

    /**
     * @verifies return true if all metadata fields blank
     */
    @Test
    void isSkip_shouldReturnTrueIfAllMetadataFieldsBlank() throws Exception {
        MetadataElement me = new MetadataElement();
        me.getMetadataList().add(new Metadata());
        Assertions.assertTrue(me.getMetadataList().get(0).isBlank());
        Assertions.assertFalse(me.getMetadataList().get(0).isHideIfOnlyMetadataField());
        Assertions.assertTrue(me.isSkip());
    }

    /**
     * @verifies return true if all metadata fields hidden
     */
    @Test
    void isSkip_shouldReturnTrueIfAllMetadataFieldsHidden() throws Exception {
        MetadataElement me = new MetadataElement();
        {
            Metadata md = new Metadata().setHideIfOnlyMetadataField(true);
            md.getParams().add(new MetadataParameter().setType(MetadataParameterType.FIELD).setKey("foo"));
            md.setParamValue(0, 0, Collections.singletonList("bar"), "foo", null, null, null, null);
            Assertions.assertFalse(md.isBlank());
            me.getMetadataList().add(md);
        }
        {
            Metadata md = new Metadata().setHideIfOnlyMetadataField(true);
            md.getParams().add(new MetadataParameter().setType(MetadataParameterType.FIELD).setKey("label"));
            md.setParamValue(0, 0, Collections.singletonList("value"), "label", null, null, null, null);
            Assertions.assertFalse(md.isBlank());
            me.getMetadataList().add(md);
        }
        Assertions.assertTrue(me.isSkip());
    }

    /**
     * @verifies return false if non hidden metadata fields exist
     */
    @Test
    void isSkip_shouldReturnFalseIfNonHiddenMetadataFieldsExist() throws Exception {
        MetadataElement me = new MetadataElement();
        {
            Metadata md = new Metadata();
            md.getParams().add(new MetadataParameter().setType(MetadataParameterType.FIELD).setKey("foo"));
            md.setParamValue(0, 0, Collections.singletonList("bar"), "foo", null, null, null, null);
            Assertions.assertFalse(md.isBlank());
            me.getMetadataList().add(md);
        }
        {
            Metadata md = new Metadata().setHideIfOnlyMetadataField(true);
            md.getParams().add(new MetadataParameter().setType(MetadataParameterType.FIELD).setKey("label"));
            md.setParamValue(0, 0, Collections.singletonList("value"), "label", null, null, null, null);
            Assertions.assertFalse(md.isBlank());
            me.getMetadataList().add(md);
        }
        Assertions.assertFalse(me.isSkip());
    }

    /**
     * @verifies return false if at least one metadata with same type not single string
     */
    @Test
    void isDisplayBoxed_shouldReturnFalseIfAtLeastOneMetadataWithSameTypeNotSingleString() throws Exception {
        MetadataElement me = new MetadataElement();
        {
            Metadata md = new Metadata();
            md.getParams().add(new MetadataParameter().setType(MetadataParameterType.FIELD).setKey("foo"));
            md.setParamValue(0, 0, Collections.singletonList("bar"), "foo", null, null, null, null);
            Assertions.assertFalse(md.isBlank());
            me.getMetadataList().add(md);
            md.setSingleString(true);
        }
        {
            Metadata md = new Metadata().setHideIfOnlyMetadataField(true);
            md.getParams().add(new MetadataParameter().setType(MetadataParameterType.FIELD).setKey("label"));
            md.setParamValue(0, 0, Collections.singletonList("value"), "label", null, null, null, null);
            Assertions.assertFalse(md.isBlank());
            me.getMetadataList().add(md);
            md.setSingleString(false); // single string false
        }
        Assertions.assertFalse(me.isDisplayBoxed(0));
    }

    /**
     * @verifies return true if all metadata of same type single string
     */
    @Test
    void isDisplayBoxed_shouldReturnTrueIfAllMetadataOfSameTypeSingleString() throws Exception {
        MetadataElement me = new MetadataElement();
        {
            Metadata md = new Metadata();
            md.getParams().add(new MetadataParameter().setType(MetadataParameterType.FIELD).setKey("foo"));
            md.setParamValue(0, 0, Collections.singletonList("bar"), "foo", null, null, null, null);
            Assertions.assertFalse(md.isBlank());
            me.getMetadataList().add(md);
            md.setSingleString(true);
        }
        {
            Metadata md = new Metadata().setHideIfOnlyMetadataField(true);
            md.getParams().add(new MetadataParameter().setType(MetadataParameterType.FIELD).setKey("label"));
            md.setParamValue(0, 0, Collections.singletonList("value"), "label", null, null, null, null);
            Assertions.assertFalse(md.isBlank());
            me.getMetadataList().add(md);
            md.setSingleString(false); // single string false
            md.setType(1); //different type
        }
        Assertions.assertTrue(me.isDisplayBoxed(0));
    }

    /**
     * @see MetadataElement#getDocStructTypeLabel()
     * @verifies return docstruct type if record
     */
    @Test
    void getDocStructTypeLabel_shouldReturnDocstructTypeIfRecord() throws Exception {
        MetadataElement me = new MetadataElement();
        me.setDocStructType("manuscript");
        Assertions.assertEquals("manuscript", me.getDocStructTypeLabel());
    }

    /**
     * @see MetadataElement#getDocStructTypeLabel()
     * @verifies return group type if group
     */
    @Test
    void getDocStructTypeLabel_shouldReturnGroupTypeIfGroup() throws Exception {
        MetadataElement me = new MetadataElement();
        me.setDocStructType("_GROUPS");
        me.setGroupType("Series");
        Assertions.assertEquals("Series", me.getDocStructTypeLabel());
    }

    /**
     * Verifies that MetadataType.getTabName() constructs the correct message key from the
     * view index and type values, and returns it when a matching translation exists.
     *
     * @see MetadataElement.MetadataType#getTabName(int)
     * @verifies return correct message key
     */
    @Test
    void getTabName_shouldReturnCorrectMessageKey() throws Exception {
        MetadataElement me = new MetadataElement();
        // MetadataType is an inner class of MetadataElement; create one with type=0
        MetadataElement.MetadataType mt = me.new MetadataType(0);

        // The key format is "metadataTab_<viewIndex>_<type>".
        // With viewIndex=0 and type=0 the key is "metadataTab_0_0".
        // If the key is not in the message bundles, getTabName returns "".
        String result = mt.getTabName(0);
        Assertions.assertEquals("", result, "Expected empty string when message key is not in the bundle");

        // With a different type value that also doesn't exist, the result should still be ""
        MetadataElement.MetadataType mt2 = me.new MetadataType(5);
        Assertions.assertEquals("", mt2.getTabName(1), "Expected empty string for non-existent key metadataTab_1_5");
    }

    /**
     * Verifies that getMetadata(name, language) returns the correct language-specific Metadata
     * field when it exists in the metadata list.
     *
     * @see MetadataElement#getMetadata(String, String)
     * @verifies return correct language metadata field
     */
    @Test
    void getMetadata_shouldReturnCorrectLanguageMetadataField() throws Exception {
        MetadataElement me = new MetadataElement();

        // Add metadata entries for different languages
        Metadata mdDe = new Metadata("", "MD_TITLE_LANG_DE", "", "German Title");
        Metadata mdEn = new Metadata("", "MD_TITLE_LANG_EN", "", "English Title");
        Metadata mdGeneric = new Metadata("", "MD_TITLE", "", "Generic Title");
        me.getMetadataList().add(mdDe);
        me.getMetadataList().add(mdEn);
        me.getMetadataList().add(mdGeneric);

        // Request the English variant
        Metadata result = me.getMetadata("MD_TITLE", "EN");
        Assertions.assertNotNull(result);
        Assertions.assertEquals("MD_TITLE_LANG_EN", result.getLabel());

        // Request the German variant
        Metadata resultDe = me.getMetadata("MD_TITLE", "DE");
        Assertions.assertNotNull(resultDe);
        Assertions.assertEquals("MD_TITLE_LANG_DE", resultDe.getLabel());
    }

    /**
     * Verifies that getMetadata falls back to the non-language-specific field
     * when the requested language variant is not present in the metadata list.
     *
     * @see MetadataElement#getMetadata(String, String)
     * @verifies fall back to non language field if language field not found
     */
    @Test
    void getMetadata_shouldFallBackToNonLanguageFieldIfLanguageFieldNotFound() throws Exception {
        MetadataElement me = new MetadataElement();

        // Only add a generic (non-language) metadata entry — no French variant exists
        Metadata mdGeneric = new Metadata("", "MD_TITLE", "", "Generic Title");
        me.getMetadataList().add(mdGeneric);

        // Request French variant, which does not exist — should fall back to generic
        Metadata result = me.getMetadata("MD_TITLE", "FR");
        Assertions.assertNotNull(result);
        Assertions.assertEquals("MD_TITLE", result.getLabel());
    }

    /**
     * @verifies get fold position
     */
    @Test
    void getMetadataListBeforeFold_shouldGetFoldPosition() throws PresentationException, IndexUnreachableException {
        String iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier(PI_KLEIUNIV);
        Assertions.assertNotNull(iddoc);
        StructElement element = new StructElement(iddoc);
        element.setWork(true);

        MetadataElement mdElement = new MetadataElement().init(element, 0, Locale.GERMANY);
        List<Metadata> md = mdElement.getMetadataList();
        int foldPosition = mdElement.getMetadataFoldIndex();
        assertTrue(foldPosition > 0 && foldPosition < md.size());
        assertEquals(mdElement.getMetadataListBeforeFold().size(), foldPosition);
        assertEquals(md.size(), mdElement.getMetadataListBeforeFold().size() + mdElement.getMetadataListAfterFold().size());
    }
}
