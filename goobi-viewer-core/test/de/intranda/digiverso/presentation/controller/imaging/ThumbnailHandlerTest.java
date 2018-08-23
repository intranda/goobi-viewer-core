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
package de.intranda.digiverso.presentation.controller.imaging;

import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrConstants.DocType;
import de.intranda.digiverso.presentation.controller.SolrConstants.MetadataGroupType;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;
import de.intranda.digiverso.presentation.model.viewer.StringPair;
import de.intranda.digiverso.presentation.model.viewer.StructElement;

/**
 * @author Florian Alpers
 *
 */
public class ThumbnailHandlerTest {

    private static final String STATIC_IMAGES_PATH = "http://localhost:8080/viewer/resources/images";
    private ThumbnailHandler handler;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));
        Configuration configuration = DataManager.getInstance().getConfiguration();
        handler = new ThumbnailHandler(new IIIFUrlHandler(), configuration, STATIC_IMAGES_PATH);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testPage() throws ViewerConfigurationException {
        PhysicalElement page =
                new PhysicalElement("PHYS_0001", "00000001.tif", 1, "Seite 1", "urn:234235:3423", "http://purl", "1234", "image/tiff", null);

        String url = handler.getThumbnailUrl(page, 200, 300);
        System.out.println(url);
        Assert.assertEquals("http://localhost:8080/viewer/rest/image/1234/00000001.tif/full/!200,300/0/default.jpg", url);
    }

    @Test
    public void testExternalIIIFImageUrl() throws ViewerConfigurationException {
        String fileUrl = "http://rosdok.uni-rostock.de/iiif/image-api/rosdok%252Fppn740913301%252Fphys_0001/full/full/0/native.jpg";
        PhysicalElement page = new PhysicalElement("PHYS_0001", fileUrl, 1, "Seite 1", "urn:234235:3423", "http://purl", "1234", "image/tiff", null);

        String url = handler.getThumbnailUrl(page, 200, 300);
        System.out.println(url);
        String refrenceUrl = "http://rosdok.uni-rostock.de/iiif/image-api/rosdok%252Fppn740913301%252Fphys_0001/full/!200,300/0/native.jpg";
        Assert.assertEquals(refrenceUrl, url);
    }

    @Test
    public void testExternalIIIFImageInfoUrl() throws ViewerConfigurationException {
        String fileUrl = "http://rosdok.uni-rostock.de/iiif/image-api/rosdok%252Fppn740913301%252Fphys_0001/info.json";
        PhysicalElement page = new PhysicalElement("PHYS_0001", fileUrl, 1, "Seite 1", "urn:234235:3423", "http://purl", "1234", "image/tiff", null);

        String url = handler.getThumbnailUrl(page, 200, 300);
        System.out.println(url);
        String refrenceUrl = "http://rosdok.uni-rostock.de/iiif/image-api/rosdok%252Fppn740913301%252Fphys_0001/full/!200,300/0/default.jpg";
        Assert.assertEquals(refrenceUrl, url);
    }

    @Test
    public void testDocLocal() throws IndexUnreachableException, ViewerConfigurationException {

        SolrDocument solrDoc = new SolrDocument();
        solrDoc.setField(SolrConstants.MIMETYPE, "image/tiff");
        solrDoc.setField(SolrConstants.THUMBNAIL, "00000001.tif");
        solrDoc.setField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        solrDoc.setField(SolrConstants.METADATATYPE, MetadataGroupType.PERSON);
        solrDoc.setField(SolrConstants.PI, "1234");
        solrDoc.setField(SolrConstants.PI_TOPSTRUCT, "1234");

        StructElement doc = new StructElement(1, solrDoc);

        String url = handler.getThumbnailUrl(doc, 200, 300);
        System.out.println(url);
        Assert.assertEquals("http://localhost:8080/viewer/rest/image/1234/00000001.tif/full/!200,300/0/default.jpg", url);
    }
    
    /**
     * TODO: Calling the thumbnailUrl for the anchor should yield an url with the pi of the first child
     * This is implemented, but I don't know how to set up the test data 
     * ({@link de.intranda.digiverso.presentation.controller.SolrSearchIndex#getFirstDoc(String, List, List) SolrSearchIndex#getFirstDoc} is used)
     */
//    @Test
    public void testAnchorLocal() throws IndexUnreachableException, ViewerConfigurationException {

        SolrDocument solrDoc = new SolrDocument();
        solrDoc.setField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        solrDoc.setField(SolrConstants.DOCSTRCT, "periodical");
        solrDoc.setField(SolrConstants.ISANCHOR, true);
        solrDoc.setField(SolrConstants.PI, "1234");
        solrDoc.setField(SolrConstants.PI_TOPSTRUCT, "1234");
        
        SolrDocument solrDocVolume = new SolrDocument();
        solrDocVolume.setField(SolrConstants.MIMETYPE, "image/tiff");
        solrDocVolume.setField(SolrConstants.THUMBNAIL, "00000001.tif");
        solrDocVolume.setField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        solrDocVolume.setField(SolrConstants.DOCSTRCT, "periodical_volume");
        solrDocVolume.setField(SolrConstants.PI, "1234_1");
        solrDocVolume.setField(SolrConstants.PI_TOPSTRUCT, "1234");
        solrDocVolume.setField(SolrConstants.PI_ANCHOR, "1234");
        solrDocVolume.setField(SolrConstants.PI_PARENT, "1234");
        
        StructElement doc = new StructElement(1, solrDoc);

        String url = handler.getThumbnailUrl(doc, 200, 300);
        System.out.println(url);
        Assert.assertEquals("http://localhost:8080/viewer/rest/image/1234_1/00000001.tif/full/!200,300/0/default.jpg", url);
    }

    @Test
    public void testDocExternal() throws IndexUnreachableException, ViewerConfigurationException {

        SolrDocument solrDoc = new SolrDocument();
        solrDoc.setField(SolrConstants.MIMETYPE, "image/tiff");
        solrDoc.setField(SolrConstants.THUMBNAIL, "http://external/iiif/image/00000001.tif");
        solrDoc.setField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        solrDoc.setField(SolrConstants.METADATATYPE, MetadataGroupType.PERSON);
        solrDoc.setField(SolrConstants.PI, "1234");
        solrDoc.setField(SolrConstants.PI_TOPSTRUCT, "1234");

        StructElement doc = new StructElement(1, solrDoc);

        String url = handler.getThumbnailUrl(doc, 200, 300);
        System.out.println(url);
        Assert.assertEquals(
                "http://localhost:8080/viewer/rest/image/-/http:U002FU002FexternalU002FiiifU002FimageU002F00000001.tif/full/!200,300/0/default.jpg",
                url);
    }

    @Test
    public void testDocExternalIIIF() throws IndexUnreachableException, ViewerConfigurationException {

        SolrDocument solrDoc = new SolrDocument();
        solrDoc.setField(SolrConstants.MIMETYPE, "image/tiff");
        solrDoc.setField(SolrConstants.THUMBNAIL, "http://external/iiif/image/00000001.tif/info.json");
        solrDoc.setField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        solrDoc.setField(SolrConstants.METADATATYPE, MetadataGroupType.PERSON);
        solrDoc.setField(SolrConstants.PI, "1234");
        solrDoc.setField(SolrConstants.PI_TOPSTRUCT, "1234");

        StructElement doc = new StructElement(1, solrDoc);

        String url = handler.getThumbnailUrl(doc, 200, 300);
        System.out.println(url);
        Assert.assertEquals("http://external/iiif/image/00000001.tif/full/!200,300/0/default.jpg", url);
    }

}
