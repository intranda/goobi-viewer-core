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
package io.goobi.viewer.controller.imaging;

import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.ConfigurationTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrConstants.DocType;
import io.goobi.viewer.controller.SolrConstants.MetadataGroupType;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * @author Florian Alpers
 *
 */
public class ThumbnailHandlerTest extends AbstractTest {

    private static final String STATIC_IMAGES_PATH = "http://localhost:8080/viewer/resources/images";
    private ThumbnailHandler handler;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        DataManager.getInstance().injectConfiguration(new Configuration("src/test/resources/config_viewer.test.xml"));
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
    public void testPage() {
        PhysicalElement page =
                new PhysicalElement("PHYS_0001", "00000001.tif", 1, "Seite 1", "urn:234235:3423", "http://purl", "1234", "image/tiff", null);

        String url = handler.getThumbnailUrl(page, 200, 300);
        Assert.assertEquals(ConfigurationTest.APPLICATION_ROOT_URL + "api/v1/records/1234/files/images/00000001.tif/full/!200,300/0/default.jpg", url);
    }

    @Test
    public void testExternalIIIFImageUrl() {
        String fileUrl = "http://rosdok.uni-rostock.de/iiif/image-api/rosdok%252Fppn740913301%252Fphys_0001/full/full/0/native.jpg";
        PhysicalElement page = new PhysicalElement("PHYS_0001", fileUrl, 1, "Seite 1", "urn:234235:3423", "http://purl", "1234", "image/tiff", null);

        String url = handler.getThumbnailUrl(page, 200, 300);
        String refrenceUrl = "http://rosdok.uni-rostock.de/iiif/image-api/rosdok%252Fppn740913301%252Fphys_0001/full/!200,300/0/native.jpg";
        Assert.assertEquals(refrenceUrl, url);
    }

    @Test
    public void testExternalIIIFImageInfoUrl() {
        String fileUrl = "http://rosdok.uni-rostock.de/iiif/image-api/rosdok%252Fppn740913301%252Fphys_0001/info.json";
        PhysicalElement page = new PhysicalElement("PHYS_0001", fileUrl, 1, "Seite 1", "urn:234235:3423", "http://purl", "1234", "image/tiff", null);

        String url = handler.getThumbnailUrl(page, 200, 300);
        String refrenceUrl = "http://rosdok.uni-rostock.de/iiif/image-api/rosdok%252Fppn740913301%252Fphys_0001/full/!200,300/0/default.jpg";
        Assert.assertEquals(refrenceUrl, url);
    }

    @Test
    public void testGetFullImageUrl() {
        String fileUrl = "00000001.tif";
        PhysicalElement page = new PhysicalElement("PHYS_0001", fileUrl, 1, "Seite 1", "urn:234235:3423", "http://purl", "1234", "image/tiff", null);

        String urlMax = handler.getFullImageUrl(page, Scale.MAX);
        Assert.assertEquals(
                DataManager.getInstance().getConfiguration().getIIIFApiUrl() + "records/1234/files/images/00000001.tif/full/max/0/default.tif",
                urlMax);

        String urlBox = handler.getFullImageUrl(page, new Scale.ScaleToBox(1500, 1500));
        Assert.assertEquals(
                DataManager.getInstance().getConfiguration().getIIIFApiUrl() + "records/1234/files/images/00000001.tif/full/!1500,1500/0/default.tif",
                urlBox);

        String urlFraction = handler.getFullImageUrl(page, new Scale.ScaleToFraction(0.5));
        Assert.assertEquals(
                DataManager.getInstance().getConfiguration().getIIIFApiUrl() + "records/1234/files/images/00000001.tif/full/pct:50/0/default.tif",
                urlFraction);
    }

    @Test
    public void testThumbnailUrl() {
        String fileUrl = "00000001.tif";
        PhysicalElement page = new PhysicalElement("PHYS_0001", fileUrl, 1, "Seite 1", "urn:234235:3423", "http://purl", "1234", "image/tiff", null);

        String urlMax = handler.getThumbnailUrl(page, 0, 0);
        Assert.assertEquals(
                DataManager.getInstance().getConfiguration().getIIIFApiUrl() + "records/1234/files/images/00000001.tif/full/max/0/default.jpg",
                urlMax);

        String urlBox = handler.getThumbnailUrl(page, 1500, 1500);
        Assert.assertEquals(
                DataManager.getInstance().getConfiguration().getIIIFApiUrl() + "records/1234/files/images/00000001.tif/full/!1500,1500/0/default.jpg",
                urlBox);

    }

    @Test
    public void testDocLocal() throws IndexUnreachableException {

        SolrDocument solrDoc = new SolrDocument();
        solrDoc.setField(SolrConstants.MIMETYPE, "image/tiff");
        solrDoc.setField(SolrConstants.THUMBNAIL, "00000001.tif");
        solrDoc.setField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        solrDoc.setField(SolrConstants.METADATATYPE, MetadataGroupType.PERSON);
        solrDoc.setField(SolrConstants.PI, "1234");
        solrDoc.setField(SolrConstants.PI_TOPSTRUCT, "1234");

        StructElement doc = new StructElement(1, solrDoc);

        String url = handler.getThumbnailUrl(doc, 200, 300);
        Assert.assertEquals(ConfigurationTest.APPLICATION_ROOT_URL + "api/v1/records/1234/files/images/00000001.tif/full/!200,300/0/default.jpg", url);
    }

    /**
     * TODO: Calling the thumbnailUrl for the anchor should yield an url with the pi of the first child This is implemented, but I don't know how to
     * set up the test data ({@link io.goobi.viewer.controller.SolrSearchIndex#getFirstDoc(String, List, List) SolrSearchIndex#getFirstDoc} is used)
     */
    //    @Test
    public void testAnchorLocal() throws IndexUnreachableException {

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
        Assert.assertEquals("http://localhost:8080/viewer/rest/image/1234_1/00000001.tif/full/!200,300/0/default.jpg", url);
    }

    @Test
    public void testDocExternal() throws IndexUnreachableException {

        SolrDocument solrDoc = new SolrDocument();
        solrDoc.setField(SolrConstants.MIMETYPE, "image/tiff");
        solrDoc.setField(SolrConstants.THUMBNAIL, "http://external/iiif/image/00000001.tif");
        solrDoc.setField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        solrDoc.setField(SolrConstants.METADATATYPE, MetadataGroupType.PERSON);
        solrDoc.setField(SolrConstants.PI, "1234");
        solrDoc.setField(SolrConstants.PI_TOPSTRUCT, "1234");

        StructElement doc = new StructElement(1, solrDoc);

        String url = handler.getThumbnailUrl(doc, 200, 300);
        Assert.assertEquals(ConfigurationTest.APPLICATION_ROOT_URL
                + "api/v1/image/-/http:U002FU002FexternalU002FiiifU002FimageU002F00000001.tif/full/!200,300/0/default.jpg", url);
    }

    @Test
    public void testDocExternalIIIF() throws IndexUnreachableException {

        SolrDocument solrDoc = new SolrDocument();
        solrDoc.setField(SolrConstants.MIMETYPE, "image/tiff");
        solrDoc.setField(SolrConstants.THUMBNAIL, "http://external/iiif/image/00000001.tif/info.json");
        solrDoc.setField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        solrDoc.setField(SolrConstants.METADATATYPE, MetadataGroupType.PERSON);
        solrDoc.setField(SolrConstants.PI, "1234");
        solrDoc.setField(SolrConstants.PI_TOPSTRUCT, "1234");

        StructElement doc = new StructElement(1, solrDoc);

        String url = handler.getThumbnailUrl(doc, 200, 300);
        Assert.assertEquals("http://external/iiif/image/00000001.tif/full/!200,300/0/default.jpg", url);
    }

}
