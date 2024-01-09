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

import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.ConfigurationTest;
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
     * @verifies create stub correctly
     */
    @Test
    void createStub_shouldCreateStubCorrectly() throws Exception {
        long iddoc = DataManager.getInstance().getSearchIndex().getIddocByLogid(PI_KLEIUNIV, "LOG_0002");
        Assertions.assertNotEquals(-1, iddoc);

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
     * @see StructElement#getParent()
     * @verifies return parent correctly
     */
    @Test
    void getParent_shouldReturnParentCorrectly() throws Exception {
        long iddoc = DataManager.getInstance().getSearchIndex().getIddocByLogid(PI_KLEIUNIV, "LOG_0002");
        Assertions.assertNotEquals(-1, iddoc);

        StructElement element = new StructElement(iddoc);
        StructElement parent = element.getParent();
        Assertions.assertNotNull(parent);
        Assertions.assertEquals(iddocKleiuniv, parent.getLuceneId());
    }

    /**
     * @see StructElement#isAnchorChild()
     * @verifies return true if current record is volume
     */
    @Test
    public void isAnchorChild_shouldReturnTrueIfCurrentRecordIsVolume() throws Exception {
        long iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("306653648_1891");
        Assertions.assertNotEquals(-1, iddoc);

        StructElement element = new StructElement(iddoc);
        Assertions.assertTrue(element.isAnchorChild());
    }

    /**
     * @see StructElement#isAnchorChild()
     * @verifies return false if current record is not volume
     */
    @Test
    public void isAnchorChild_shouldReturnFalseIfCurrentRecordIsNotVolume() throws Exception {
        StructElement element = new StructElement(iddocKleiuniv);
        Assertions.assertFalse(element.isAnchorChild());
    }

    /**
     * @see StructElement#getImageUrl(int,int,int,boolean,boolean)
     * @verifies construct url correctly
     */
    @Test
    public void getImageUrl_shouldConstructUrlCorrectly() throws Exception {
        StructElement element = new StructElement(iddocKleiuniv);
        Assertions.assertEquals(
                ConfigurationTest.APPLICATION_ROOT_URL + "api/v1/records/" + PI_KLEIUNIV + "/files/images/00000001.tif/full/!600,800/0/default.jpg",
                element.getImageUrl(600, 800));
    }

    /**
     * @see StructElement#getTopStruct()
     * @verifies retrieve top struct correctly
     */
    @Test
    public void getTopStruct_shouldRetrieveTopStructCorrectly() throws Exception {
        long iddoc = DataManager.getInstance().getSearchIndex().getIddocByLogid(PI_KLEIUNIV, "LOG_0002");
        Assertions.assertNotEquals(-1, iddoc);

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
    public void getTopStruct_shouldReturnSelfIfTopstruct() throws Exception {
        StructElement element = new StructElement(iddocKleiuniv);
        Assertions.assertTrue(element.isWork());
        StructElement topStruct = element.getTopStruct();
        Assertions.assertEquals(element, topStruct);
        Assertions.assertEquals(iddocKleiuniv, topStruct.getLuceneId());
    }

    /**
     * @see StructElement#getTopStruct()
     * @verifies return self if anchor
     */
    @Test
    public void getTopStruct_shouldReturnSelfIfAnchor() throws Exception {
        long iddoc = 1593684706691L;
        StructElement element = new StructElement(iddoc);
        Assertions.assertTrue(element.isAnchor());
        StructElement topStruct = element.getTopStruct();
        Assertions.assertEquals(element, topStruct);
        Assertions.assertEquals(iddoc, topStruct.getLuceneId());
    }

    /**
     * @see StructElement#getTopStruct()
     * @verifies return self if group
     */
    @Test
    public void getTopStruct_shouldReturnSelfIfGroup() throws Exception {
        long iddoc = 1593684709030L;
        StructElement element = new StructElement(iddoc);
        Assertions.assertTrue(element.isGroup());
        StructElement topStruct = element.getTopStruct();
        Assertions.assertEquals(element, topStruct);
        Assertions.assertEquals(iddoc, topStruct.getLuceneId());
    }

    /**
     * @see StructElement#getFirstVolumeFieldValue(String)
     * @verifies return correct value
     */
    @Test
    public void getFirstVolumeFieldValue_shouldReturnCorrectValue() throws Exception {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":306653648", null);
        Assertions.assertNotNull(doc);
        StructElement element = new StructElement(Long.valueOf((String) doc.getFieldValue(SolrConstants.IDDOC)), doc);
        Assertions.assertEquals("306653648_1891", element.getFirstVolumeFieldValue(SolrConstants.PI));
    }

    /**
     * @see StructElement#getFirstVolumeFieldValue(String)
     * @verifies return null if StructElement not anchor
     */
    @Test
    public void getFirstVolumeFieldValue_shouldReturnNullIfStructElementNotAnchor() throws Exception {
        StructElement element = new StructElement(1387459017772L);
        Assertions.assertNull(element.getFirstVolumeFieldValue(SolrConstants.MIMETYPE));
    }

    /**
     * @see StructElement#getFirstVolumeFieldValue(String)
     * @verifies throw IllegalArgumentException if field is null
     */
    @Test
    void getFirstVolumeFieldValue_shouldThrowIllegalArgumentExceptionIfFieldIsNull() throws Exception {
        StructElement element = new StructElement(1387459017772L);
        Assertions.assertThrows(IllegalArgumentException.class, () -> element.getFirstVolumeFieldValue(null));
    }

    /**
     * @see StructElement#isHasChildren()
     * @verifies return true if element has children
     */
    @Test
    public void isHasChildren_shouldReturnTrueIfElementHasChildren() throws Exception {
        StructElement element = new StructElement(iddocKleiuniv);
        Assertions.assertTrue(element.isHasChildren());
    }

    /**
     * @see StructElement#getGroupLabel(String,String)
     * @verifies return altValue of no label was found
     */
    @Test
    public void getGroupLabel_shouldReturnAltValueOfNoLabelWasFound() throws Exception {
        StructElement element = new StructElement(1L);
        Assertions.assertEquals("alt", element.getGroupLabel("id10T", "alt"));
    }

    /**
     * @see StructElement#getPi()
     * @verifies return pi if topstruct
     */
    @Test
    public void getPi_shouldReturnPiIfTopstruct() throws Exception {
        StructElement element = new StructElement(iddocKleiuniv);
        Assertions.assertEquals(PI_KLEIUNIV, element.getPi());
    }

    /**
     * @see StructElement#getPi()
     * @verifies retriveve pi from topstruct if not topstruct
     */
    @Test
    public void getPi_shouldRetrivevePiFromTopstructIfNotTopstruct() throws Exception {
        long iddoc = DataManager.getInstance().getSearchIndex().getIddocByLogid(PI_KLEIUNIV, "LOG_0002");
        Assertions.assertNotEquals(-1, iddoc);

        StructElement element = new StructElement(iddoc);
        Assertions.assertEquals(PI_KLEIUNIV, element.getPi());
    }
}
