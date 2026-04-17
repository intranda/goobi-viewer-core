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
package io.goobi.viewer.model.viewer;

import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.TestUtils;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.managedbeans.ContextMocker;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;

class StructElementTest extends AbstractSolrEnabledTest {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(StructElementTest.class);

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        FacesContext facesContext = ContextMocker.mockFacesContext();
        ExternalContext externalContext = Mockito.mock(ExternalContext.class);
        //        ServletContext servletContext = Mockito.mock(ServletContext.class);
        UIViewRoot viewRoot = Mockito.mock(UIViewRoot.class);

        Mockito.when(facesContext.getExternalContext()).thenReturn(externalContext);
        //        Mockito.when(externalContext.getContext()).thenReturn(servletContext);
        //        Mockito.when(facesContext.getViewRoot()).thenReturn(viewRoot);
        //        Mockito.when(viewRoot.getLocale()).thenReturn(Locale.GERMAN);
    }

    /**
     * @see StructElement#createStub()
     * @verifies copy all fields including PI, logid, doctype, label, and metadata to stub
     */
    @Test
    void createStub_shouldCopyAllFieldsIncludingPILogidDoctypeLabelAndMetadataToStub() throws Exception {
        String iddoc = DataManager.getInstance().getSearchIndex().getIddocByLogid(PI_KLEIUNIV, "LOG_0002");
        Assertions.assertNotNull(iddoc);

        StructElement element = new StructElement(iddoc);
        StructElementStub stub = element.createStub();
        Assertions.assertEquals(element.getLuceneId(), stub.getLuceneId());
        Assertions.assertEquals(element.getPi(), stub.getPi());
        Assertions.assertEquals(element.getLogid(), stub.getLogid());
        Assertions.assertEquals(element.getDocStructType(), stub.getDocStructType());
        Assertions.assertEquals(DocType.DOCSTRCT, stub.getDocType());
        Assertions.assertEquals(element.getSourceDocFormat(), stub.getSourceDocFormat());
        Assertions.assertEquals(element.getImageNumber(), stub.getImageNumber());
        Assertions.assertEquals(element.isAnchor(), stub.isAnchor());
        Assertions.assertNotNull(element.getLabel());
        Assertions.assertEquals(element.getLabel(), stub.getLabel());
        Assertions.assertEquals(element.getMetadataFields().size(), stub.getMetadataFields().size());

    }

    /**
     * @verifies return parent StructElement with matching Lucene ID for child element
     */
    @Test
    void getParent_shouldReturnParentStructElementWithMatchingLuceneIDForChildElement() throws Exception {
        String iddoc = DataManager.getInstance().getSearchIndex().getIddocByLogid(PI_KLEIUNIV, "LOG_0002");
        Assertions.assertNotNull(iddoc);

        StructElement element = new StructElement(iddoc);
        StructElement parent = element.getParent();
        Assertions.assertNotNull(parent);
        Assertions.assertEquals(iddocKleiuniv, parent.getLuceneId());
    }

    /**
     * @verifies return true if current record is volume
     */
    @Test
    void isAnchorChild_shouldReturnTrueIfCurrentRecordIsVolume() throws Exception {
        String iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("306653648_1891");
        Assertions.assertNotNull(iddoc);

        StructElement element = new StructElement(iddoc);
        Assertions.assertTrue(element.isAnchorChild());
    }

    /**
     * @see StructElement#isAnchorChild()
     * @verifies return false if current record is not volume
     */
    @Test
    void isAnchorChild_shouldReturnFalseIfCurrentRecordIsNotVolume() throws Exception {
        StructElement element = new StructElement(iddocKleiuniv);
        Assertions.assertFalse(element.isAnchorChild());
    }

    /**
     * @see StructElement#getImageUrl(int,int,int,boolean,boolean)
     * @verifies return IIIF image URL with given width and height constraints
     */
    @Test
    void getImageUrl_shouldReturnIIIFImageURLWithGivenWidthAndHeightConstraints() throws Exception {
        StructElement element = new StructElement(iddocKleiuniv);
        Assertions.assertEquals(
                TestUtils.APPLICATION_ROOT_URL + "api/v1/records/" + PI_KLEIUNIV + "/files/images/00000001.tif/full/!600,800/0/default.jpg",
                element.getImageUrl(600, 800));
    }

    /**
     * @verifies return top level struct element different from child element with expected lucene i d
     */
    @Test
    void getTopStruct_shouldReturnTopLevelStructElementDifferentFromChildElementWithExpectedLuceneID() throws Exception {
        String iddoc = DataManager.getInstance().getSearchIndex().getIddocByLogid(PI_KLEIUNIV, "LOG_0002");
        Assertions.assertNotNull(iddoc);

        StructElement element = new StructElement(iddoc);
        StructElement topStruct = element.getTopStruct();
        Assertions.assertNotNull(topStruct);
        Assertions.assertNotEquals(element, topStruct);
        Assertions.assertEquals(iddocKleiuniv, topStruct.getLuceneId());
    }

    /**
     * @see StructElement#getTopStruct()
     * @verifies return self if topstruct
     */
    @Test
    void getTopStruct_shouldReturnSelfIfTopstruct() throws Exception {
        StructElement element = new StructElement(iddocKleiuniv);
        Assertions.assertTrue(element.isWork());
        StructElement topStruct = element.getTopStruct();
        Assertions.assertEquals(element, topStruct);
        Assertions.assertEquals(iddocKleiuniv, topStruct.getLuceneId());
    }

    /**
     * @verifies return self if anchor
     */
    @Test
    void getTopStruct_shouldReturnSelfIfAnchor() throws Exception {
        String iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("168714434");
        Assertions.assertNotNull(iddoc);
        StructElement element = new StructElement(iddoc);
        Assertions.assertTrue(element.isAnchor());
        StructElement topStruct = element.getTopStruct();
        Assertions.assertEquals(element, topStruct);
        Assertions.assertEquals(iddoc, topStruct.getLuceneId());
    }

    /**
     * @verifies return self if group
     */
    @Test
    void getTopStruct_shouldReturnSelfIfGroup() throws Exception {
        String iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("AC00906132");
        Assertions.assertNotNull(iddoc);
        StructElement element = new StructElement(iddoc);
        Assertions.assertTrue(element.isGroup());
        StructElement topStruct = element.getTopStruct();
        Assertions.assertEquals(element, topStruct);
        Assertions.assertEquals(iddoc, topStruct.getLuceneId());
    }

    /**
     * @verifies return correct value
     */
    @Test
    void getFirstVolumeFieldValue_shouldReturnCorrectValue() throws Exception {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":306653648", null);
        Assertions.assertNotNull(doc);
        StructElement element = new StructElement((String) doc.getFieldValue(SolrConstants.IDDOC), doc);
        Assertions.assertEquals("306653648_1891", element.getFirstVolumeFieldValue(SolrConstants.PI));
    }

    /**
     * @see StructElement#getFirstVolumeFieldValue(String)
     * @verifies return null if StructElement not anchor
     */
    @Test
    void getFirstVolumeFieldValue_shouldReturnNullIfStructElementNotAnchor() throws Exception {
        StructElement element = new StructElement("1387459017772");
        Assertions.assertNull(element.getFirstVolumeFieldValue(SolrConstants.MIMETYPE));
    }

    /**
     * @see StructElement#getFirstVolumeFieldValue(String)
     * @verifies throw IllegalArgumentException if field is null
     */
    @Test
    void getFirstVolumeFieldValue_shouldThrowIllegalArgumentExceptionIfFieldIsNull() throws Exception {
        StructElement element = new StructElement("1387459017772");
        Assertions.assertThrows(IllegalArgumentException.class, () -> element.getFirstVolumeFieldValue(null));
    }

    /**
     * @verifies return true if element has children
     * @see StructElement#isHasChildren()
     */
    @Test
    void isHasChildren_shouldReturnTrueIfElementHasChildren() throws Exception {
        StructElement element = new StructElement(iddocKleiuniv);
        Assertions.assertTrue(element.isHasChildren());
    }

    /**
     * @see StructElement#getGroupLabel(String,String)
     * @verifies return altValue of no label was found
     */
    @Test
    void getGroupLabel_shouldReturnAltValueOfNoLabelWasFound() throws Exception {
        StructElement element = new StructElement("1");
        Assertions.assertEquals("alt", element.getGroupLabel("id10T", "alt"));
    }

    /**
     * @see StructElement#getPi()
     * @verifies return pi if topstruct
     */
    @Test
    void getPi_shouldReturnPiIfTopstruct() throws Exception {
        StructElement element = new StructElement(iddocKleiuniv);
        Assertions.assertEquals(PI_KLEIUNIV, element.getPi());
    }

    /**
     * @verifies retriveve pi from topstruct if not topstruct
     */
    @Test
    void getPi_shouldRetrivevePiFromTopstructIfNotTopstruct() throws Exception {
        String iddoc = DataManager.getInstance().getSearchIndex().getIddocByLogid(PI_KLEIUNIV, "LOG_0002");
        Assertions.assertNotNull(iddoc);

        StructElement element = new StructElement(iddoc);
        Assertions.assertEquals(PI_KLEIUNIV, element.getPi());
    }

    /**
     * @verifies populate groupMemberships even if field is also in ancestorIdentifierFields
     */
    @Test
    void init_shouldPopulateGroupMembershipsEvenIfFieldIsAlsoInAncestorIdentifierFields() throws Exception {
        // GROUPID_1 is configured in both ancestorIdentifierFields and recordGroupIdentifierFields
        // in config_viewer.test.xml, simulating the production setup for newspapers.
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.IDDOC, "99999");
        doc.setField("GROUPID_1", "group_pi_001");

        StructElement element = new StructElement(doc);

        Assertions.assertTrue(element.isGroupMember(),
                "Record with a field in both ancestorIdentifierFields and recordGroupIdentifierFields must be recognized as group member");
        Assertions.assertEquals("group_pi_001", element.getGroupMemberships().get("GROUPID_1"));
    }
}
