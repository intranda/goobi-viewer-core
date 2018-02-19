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

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.io.FileUtils;
import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.AbstractDatabaseAndSolrEnabledTest;
import de.intranda.digiverso.presentation.controller.ALTOTools;
import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.managedbeans.ContextMocker;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement.CoordsFormat;

public class PhysicalElementTest extends AbstractDatabaseAndSolrEnabledTest {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(PhysicalElementTest.class);

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));

        FacesContext facesContext = ContextMocker.mockFacesContext();
        ExternalContext externalContext = Mockito.mock(ExternalContext.class);
        UIViewRoot viewRoot = Mockito.mock(UIViewRoot.class);
        Mockito.when(facesContext.getExternalContext()).thenReturn(externalContext);
    }

//    /**
//     * @see PhysicalElement#getWordCoords(List)
//     * @verifies load XML document if none yet set
//     */
//    @Test
//    public void getWordCoords_shouldLoadXMLDocumentIfNoneYetSet() throws Exception {
//        PhysicalElement pe = new PhysicalElement("PHYS_0000", "00000001.tif", 0, "1", null, null, "PPN517154005", PhysicalElement.MIME_TYPE_IMAGE,
//                null);
//        Assert.assertNull(pe.getAltoText());
//        Assert.assertEquals(CoordsFormat.UNCHECKED, pe.getWordCoordsFormat());
//        pe.getWordCoords(new HashSet<>(Collections.singletonList("test")));
//        Assert.assertNotNull(pe.getAltoText());
//        Assert.assertEquals(CoordsFormat.ALTO, pe.getWordCoordsFormat());
//    }

//    /**
//     * @see PhysicalElement#loadFullText()
//     * @verifies load full-text correctly if not yet loaded
//     */
//    @Test
//    public void loadFullText_shouldLoadFulltextCorrectlyIfNotYetLoaded() throws Exception {
//        PhysicalElement pe = new PhysicalElement("PHYS_0000", "00000001.tif", 1, "1", null, null, "PPN517154005", PhysicalElement.MIME_TYPE_IMAGE,
//                null);
//        Assert.assertTrue(pe.loadFullText());
//        Assert.assertNotNull(pe.getFullText());
//    }

//    /**
//     * @see PhysicalElement#loadFullText()
//     * @verifies return false if already loaded
//     */
//    @Test
//    public void loadFullText_shouldReturnFalseIfAlreadyLoaded() throws Exception {
//        PhysicalElement pe = new PhysicalElement("PHYS_0000", "00000001.tif", 1, "1", null, null, "PPN517154005", PhysicalElement.MIME_TYPE_IMAGE,
//                null);
//        File file = new File("resources/test/METS/kleiuniv_PPN517154005/kleiuniv_PPN517154005_txt/00000001.txt");
//        Assert.assertTrue(file.isFile());
//        FileUtils.copyFile(file, new File("resources/test/data/viewer"));
//        pe.setFulltextFileName("00000001.txt");
//        Assert.assertTrue(pe.loadFullText());
//        Assert.assertFalse(pe.loadFullText());
//    }

    /**
     * @see PhysicalElement#handleAltoComposedBlock(Element)
     * @verifies return all words from nested ComposedBlocks
     */
    @Test
    public void handleAltoComposedBlock_shouldReturnAllWordsFromNestedComposedBlocks() throws Exception {
        Element eleComposedBlock1 = new Element("ComposedBlock");
        {
            Element eleTextBlock = new Element("TextBlock");
            Element eleTextLine = new Element("TextLine");
            Element eleString = new Element("String");
            eleString.setAttribute("CONTENT", "word1");
            eleTextLine.addContent(eleString);
            eleTextBlock.addContent(eleTextLine);
            eleComposedBlock1.addContent(eleTextBlock);
        }

        Element eleComposedBlock2 = new Element("ComposedBlock");
        {
            Element eleTextBlock = new Element("TextBlock");
            Element eleTextLine = new Element("TextLine");
            Element eleString = new Element("String");
            eleString.setAttribute("CONTENT", "word2");
            eleTextLine.addContent(eleString);
            eleTextBlock.addContent(eleTextLine);
            eleComposedBlock2.addContent(eleTextBlock);
        }
        eleComposedBlock1.addContent(eleComposedBlock2);

        Element eleComposedBlock3 = new Element("ComposedBlock");
        {
            Element eleTextBlock = new Element("TextBlock");
            Element eleTextLine = new Element("TextLine");
            Element eleString = new Element("String");
            eleString.setAttribute("CONTENT", "word3");
            eleTextLine.addContent(eleString);
            eleTextBlock.addContent(eleTextLine);
            eleComposedBlock3.addContent(eleTextBlock);
        }
        eleComposedBlock2.addContent(eleComposedBlock3);

        List<Element> words = ALTOTools.handleAltoComposedBlock(eleComposedBlock1);
        Assert.assertEquals(3, words.size());
        Assert.assertEquals("word1", words.get(0).getAttributeValue("CONTENT"));
        Assert.assertEquals("word2", words.get(1).getAttributeValue("CONTENT"));
        Assert.assertEquals("word3", words.get(2).getAttributeValue("CONTENT"));
    }

    /**
     * @see PhysicalElement#determineFileName(String)
     * @verifies cut off everything but the file name for normal file paths
     */
    @Test
    public void determineFileName_shouldCutOffEverythingButTheFileNameForNormalFilePaths() throws Exception {
        Assert.assertEquals("image.jpg", PhysicalElement.determineFileName("image.jpg"));
        Assert.assertEquals("image.jpg", PhysicalElement.determineFileName("/opt/digiverso/viewer/media/123/image.jpg"));
    }

    /**
     * @see PhysicalElement#determineFileName(String)
     * @verifies leave external urls intact
     */
    @Test
    public void determineFileName_shouldLeaveExternalUrlsIntact() throws Exception {
        Assert.assertEquals("http://www.example.com/image.jpg", PhysicalElement.determineFileName("http://www.example.com/image.jpg"));
    }

//    /**
//     * @see PhysicalElement#getModifiedIIIFFUrl(String,int,int)
//     * @verifies replace dimensions correctly
//     */
//    @Test
//    public void getModifiedIIIFFUrl_shouldReplaceDimensionsCorrectly() throws Exception {
//        Assert.assertEquals("http://rosdok.uni-rostock.de/iiif/image-api/rosdok/ppn750542047/phys_0001/full/!200,220/0/native.jpg", PhysicalElement
//                .getModifiedIIIFFUrl("http://rosdok.uni-rostock.de/iiif/image-api/rosdok/ppn750542047/phys_0001/full/full/0/native.jpg", 200, 220));
//    }
//
//    /**
//     * @see PhysicalElement#getModifiedIIIFFUrl(String,int,int)
//     * @verifies do nothing if not iiif url
//     */
//    @Test
//    public void getModifiedIIIFFUrl_shouldDoNothingIfNotIiifUrl() throws Exception {
//        Assert.assertEquals("http://rosdok.uni-rostock.de/random/url/image.jpg", PhysicalElement.getModifiedIIIFFUrl(
//                "http://rosdok.uni-rostock.de/random/url/image.jpg", 200, 220));
//    }
//
//    /**
//     * @see PhysicalElement#isExternalURI(String)
//     * @verifies return true for external urls
//     */
//    @Test
//    public void isExternalURI_shouldReturnTrueForExternalUrls() throws Exception {
//        Assert.assertTrue(PhysicalElement.isExternalUrl("http://www.example.com/image.jpg"));
//        Assert.assertTrue(PhysicalElement.isExternalUrl("https://www.example.com/image.jpg"));
//    }
//
//    /**
//     * @see PhysicalElement#isExternalURI(String)
//     * @verifies return false for local paths
//     */
//    @Test
//    public void isExternalURI_shouldReturnFalseForLocalPaths() throws Exception {
//        Assert.assertFalse(PhysicalElement.isExternalUrl("/opt/digiverso/viewer/media/123/image.jpg"));
//    }
}