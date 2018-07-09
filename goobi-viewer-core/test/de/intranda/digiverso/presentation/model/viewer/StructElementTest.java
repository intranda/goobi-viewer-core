/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.intranda.digiverso.presentation.model.viewer;

import java.util.List;
import java.util.Locale;

import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.AbstractSolrEnabledTest;
import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.managedbeans.ContextMocker;

public class StructElementTest extends AbstractSolrEnabledTest {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(StructElementTest.class);

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));

        FacesContext facesContext = ContextMocker.mockFacesContext();
        ExternalContext externalContext = Mockito.mock(ExternalContext.class);
        //        ServletContext servletContext = Mockito.mock(ServletContext.class);
        UIViewRoot viewRoot = Mockito.mock(UIViewRoot.class);

        Mockito.when(facesContext.getExternalContext()).thenReturn(externalContext);
        //        Mockito.when(externalContext.getContext()).thenReturn(servletContext);
        Mockito.when(facesContext.getViewRoot()).thenReturn(viewRoot);
        Mockito.when(viewRoot.getLocale()).thenReturn(Locale.GERMAN);
    }

    /**
     * @see StructElement#createStub()
     * @verifies create stub correctly
     */
    @Test
    public void createStub_shouldCreateStubCorrectly() throws Exception {
        StructElement element = new StructElement(1387459019070L);
        StructElementStub stub = element.createStub();
        Assert.assertEquals(element.getLuceneId(), stub.getLuceneId());
        Assert.assertEquals(element.getPi(), stub.getPi());
        Assert.assertEquals(element.getLogid(), stub.getLogid());
        Assert.assertEquals(element.getDocStructType(), stub.getDocStructType());
        Assert.assertEquals(element.getSourceDocFormat(), stub.getSourceDocFormat());
        Assert.assertEquals(element.getImageNumber(), stub.getImageNumber());
        Assert.assertEquals(element.isAnchor(), stub.isAnchor());
        Assert.assertNotNull(element.getLabel());
        Assert.assertEquals(element.getLabel(), stub.getLabel());
        Assert.assertEquals(element.getMetadataFields().size(), stub.getMetadataFields().size());

    }

    /**
     * @see StructElement#getChildren()
     * @verifies load all children
     */
    @Test
    @Deprecated
    public void getChildren_shouldLoadAllChildren() throws Exception {
        StructElement element = new StructElement(1387459019047L);
        List<StructElement> children = element.getChildren(null);
        Assert.assertEquals(5, children.size());

    }

    /**
     * @see StructElement#getParent()
     * @verifies return parent correctly
     */
    @Test
    public void getParent_shouldReturnParentCorrectly() throws Exception {
        StructElement element = new StructElement(1387459019066L);
        StructElement parent = element.getParent();
        Assert.assertNotNull(parent);
        Assert.assertEquals(1387459019047L, parent.getLuceneId());
    }

    /**
     * @see StructElement#isAnchorChild()
     * @verifies return true if current record is volume
     */
    @Test
    public void isAnchorChild_shouldReturnTrueIfCurrentRecordIsVolume() throws Exception {
        StructElement element = new StructElement(1387459017772L);
        Assert.assertTrue(element.isAnchorChild());
    }

    /**
     * @see StructElement#isAnchorChild()
     * @verifies return false if current record is not volume
     */
    @Test
    public void isAnchorChild_shouldReturnFalseIfCurrentRecordIsNotVolume() throws Exception {
        StructElement element = new StructElement(1387459016556L);
        Assert.assertFalse(element.isAnchorChild());
    }

    /**
     * @see StructElement#getImageUrl(int,int,int,boolean,boolean)
     * @verifies construct url correctly
     */
    @Test
    public void getImageUrl_shouldConstructUrlCorrectly() throws Exception {
        StructElement element = new StructElement(1387459019047L);
        Assert.assertEquals("http://localhost:8080/viewer/rest/image/PPN517154005/00000001.tif/full/!600,800/0/default.jpg",
                element.getImageUrl(600, 800));
    }

    /**
     * @see StructElement#getTopStruct()
     * @verifies retrieve top struct correctly
     */
    @Test
    public void getTopStruct_shouldRetrieveTopStructCorrectly() throws Exception {
        StructElement element = new StructElement(1387459019070L);
        StructElement topStruct = element.getTopStruct();
        Assert.assertNotEquals(element, topStruct);
        Assert.assertEquals(1387459019047L, topStruct.getLuceneId());
    }

    /**
     * @see StructElement#getFirstVolumeFieldValue(String)
     * @verifies return correct value
     */
    @Test
    public void getFirstVolumeFieldValue_shouldReturnCorrectValue() throws Exception {
        StructElement element = new StructElement(1342710876421L);
        Assert.assertEquals("1387459017772", element.getFirstVolumeFieldValue(SolrConstants.IDDOC));
    }

    /**
     * @see StructElement#getFirstVolumeFieldValue(String)
     * @verifies return null if StructElement not anchor
     */
    @Test
    public void getFirstVolumeFieldValue_shouldReturnNullIfStructElementNotAnchor() throws Exception {
        StructElement element = new StructElement(1387459017772L);
        Assert.assertNull(element.getFirstVolumeFieldValue(SolrConstants.MIMETYPE));
    }

    /**
     * @see StructElement#getFirstVolumeFieldValue(String)
     * @verifies throw IllegalArgumentException if field is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void getFirstVolumeFieldValue_shouldThrowIllegalArgumentExceptionIfFieldIsNull() throws Exception {
        StructElement element = new StructElement(1387459017772L);
        Assert.assertNull(element.getFirstVolumeFieldValue(null));
    }

    /**
     * @see StructElement#isHasChildren()
     * @verifies return true if element has children
     */
    @Test
    public void isHasChildren_shouldReturnTrueIfElementHasChildren() throws Exception {
        StructElement element = new StructElement(1342710876421L);
        Assert.assertTrue(element.isHasChildren());
    }
}