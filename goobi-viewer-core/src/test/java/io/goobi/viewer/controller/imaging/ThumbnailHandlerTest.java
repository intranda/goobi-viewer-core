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
package io.goobi.viewer.controller.imaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.PhysicalElementBuilder;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrConstants.MetadataGroupType;

/**
 * @author Florian Alpers
 *
 */
class ThumbnailHandlerTest extends AbstractTest {

    private static final String STATIC_IMAGES_PATH = "http://localhost:8080/viewer/resources/images";
    private ThumbnailHandler handler;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        IIIFUrlHandler iiifHandler = new IIIFUrlHandler(new ApiUrls(ApiUrls.API));
        handler = new ThumbnailHandler(iiifHandler, STATIC_IMAGES_PATH);
    }

    @Test
    void testPage() {
        PhysicalElement page = new PhysicalElementBuilder().setPi("1234")
                .setPhysId("PHYS_0001")
                .setFilePath("00000001.tif")
                .setOrder(1)
                .setOrderLabel("Seite 1")
                .setUrn("urn:234235:3423")
                .setPurlPart("http://purl")
                .setMimeType("image/tiff")
                .setDataRepository(null)
                .build();

        String url = handler.getThumbnailUrl(page, 200, 300);
        Assertions.assertEquals("/api/v1/records/1234/files/images/00000001.tif/full/!200,300/0/default.jpg", url);
    }

    @Test
    void testExternalIIIFImageUrl() {
        String fileUrl = "http://rosdok.uni-rostock.de/iiif/image-api/rosdok%252Fppn740913301%252Fphys_0001/full/full/0/native.jpg";
        PhysicalElement page = new PhysicalElementBuilder().setPi("1234")
                .setPhysId("PHYS_0001")
                .setFilePath(fileUrl)
                .setOrder(1)
                .setOrderLabel("Seite 1")
                .setUrn("urn:234235:3423")
                .setPurlPart("http://purl")
                .setMimeType("image/tiff")
                .setDataRepository(null)
                .build();

        String url = handler.getThumbnailUrl(page, 200, 300);
        String refrenceUrl = "http://rosdok.uni-rostock.de/iiif/image-api/rosdok%252Fppn740913301%252Fphys_0001/full/!200,300/0/native.jpg";
        Assertions.assertEquals(refrenceUrl, url);
    }

    @Test
    void testExternalIIIFImageInfoUrl() {
        String fileUrl = "http://rosdok.uni-rostock.de/iiif/image-api/rosdok%252Fppn740913301%252Fphys_0001/info.json";
        PhysicalElement page = new PhysicalElementBuilder().setPi("1234")
                .setPhysId("PHYS_0001")
                .setFilePath(fileUrl)
                .setOrder(1)
                .setOrderLabel("Seite 1")
                .setUrn("urn:234235:3423")
                .setPurlPart("http://purl")
                .setMimeType("image/tiff")
                .setDataRepository(null)
                .build();

        String url = handler.getThumbnailUrl(page, 200, 300);
        String refrenceUrl = "http://rosdok.uni-rostock.de/iiif/image-api/rosdok%252Fppn740913301%252Fphys_0001/full/!200,300/0/default.jpg";
        Assertions.assertEquals(refrenceUrl, url);
    }

    @Test
    void testGetFullImageUrl() {
        String fileUrl = "00000001.tif";
        PhysicalElement page = new PhysicalElementBuilder().setPi("1234")
                .setPhysId("PHYS_0001")
                .setFilePath(fileUrl)
                .setOrder(1)
                .setOrderLabel("Seite 1")
                .setUrn("urn:234235:3423")
                .setPurlPart("http://purl")
                .setMimeType("image/tiff")
                .setDataRepository(null)
                .build();

        String urlMax = handler.getFullImageUrl(page, Scale.MAX);
        Assertions.assertEquals("/api/v1/records/1234/files/images/00000001.tif/full/max/0/default.tif",
                urlMax);

        String urlBox = handler.getFullImageUrl(page, new Scale.ScaleToBox(1500, 1500));
        Assertions.assertEquals("/api/v1/records/1234/files/images/00000001.tif/full/!1500,1500/0/default.tif",
                urlBox);

        String urlFraction = handler.getFullImageUrl(page, new Scale.ScaleToFraction(0.5));
        Assertions.assertEquals("/api/v1/records/1234/files/images/00000001.tif/full/pct:50/0/default.tif",
                urlFraction);
    }

    @Test
    void testThumbnailUrl() {
        String fileUrl = "00000001.tif";
        PhysicalElement page = new PhysicalElementBuilder().setPi("1234")
                .setPhysId("PHYS_0001")
                .setFilePath(fileUrl)
                .setOrder(1)
                .setOrderLabel("Seite 1")
                .setUrn("urn:234235:3423")
                .setPurlPart("http://purl")
                .setMimeType("image/tiff")
                .setDataRepository(null)
                .build();

        String urlMax = handler.getThumbnailUrl(page, 0, 0);
        Assertions.assertEquals("/api/v1/records/1234/files/images/00000001.tif/full/max/0/default.jpg",
                urlMax);

        String urlBox = handler.getThumbnailUrl(page, 1500, 1500);
        Assertions.assertEquals("/api/v1/records/1234/files/images/00000001.tif/full/!1500,1500/0/default.jpg",
                urlBox);

    }

    @Test
    void testDocLocal() throws IndexUnreachableException {

        SolrDocument solrDoc = new SolrDocument();
        solrDoc.setField(SolrConstants.MIMETYPE, "image/tiff");
        solrDoc.setField(SolrConstants.THUMBNAIL, "00000001.tif");
        solrDoc.setField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        solrDoc.setField(SolrConstants.METADATATYPE, MetadataGroupType.PERSON);
        solrDoc.setField(SolrConstants.PI, "1234");
        solrDoc.setField(SolrConstants.PI_TOPSTRUCT, "1234");

        StructElement doc = new StructElement(1, solrDoc);

        String url = handler.getThumbnailUrl(doc, 200, 300);
        Assertions.assertEquals("/api/v1/records/1234/files/images/00000001.tif/full/!200,300/0/default.jpg", url);
    }

    /**
     * TODO: Calling the thumbnailUrl for the anchor should yield an url with the pi of the first child This is implemented, but I don't know how to
     * set up the test data ({@link io.goobi.viewer.solr.SolrSearchIndex#getFirstDoc(String, List, List) SolrSearchIndex#getFirstDoc} is used)
     */
    //    @Test
    void testAnchorLocal() throws IndexUnreachableException {

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
        Assertions.assertEquals("http://localhost:8080/viewer/rest/image/1234_1/00000001.tif/full/!200,300/0/default.jpg", url);
    }

    @Test
    void testDocExternal() throws IndexUnreachableException {

        SolrDocument solrDoc = new SolrDocument();
        solrDoc.setField(SolrConstants.MIMETYPE, "image/tiff");
        solrDoc.setField(SolrConstants.THUMBNAIL, "http://external/iiif/image/00000001.tif");
        solrDoc.setField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        solrDoc.setField(SolrConstants.METADATATYPE, MetadataGroupType.PERSON);
        solrDoc.setField(SolrConstants.PI, "1234");
        solrDoc.setField(SolrConstants.PI_TOPSTRUCT, "1234");

        StructElement doc = new StructElement(1, solrDoc);

        String url = handler.getThumbnailUrl(doc, 200, 300);
        Assertions.assertEquals("/api/v1/images/external/http:U002FU002FexternalU002FiiifU002FimageU002F00000001.tif/full/!200,300/0/default.jpg", url);
    }

    @Test
    void testDocExternalIIIF() throws IndexUnreachableException {

        SolrDocument solrDoc = new SolrDocument();
        solrDoc.setField(SolrConstants.MIMETYPE, "image/tiff");
        solrDoc.setField(SolrConstants.THUMBNAIL, "http://external/iiif/image/00000001.tif/info.json");
        solrDoc.setField(SolrConstants.DOCTYPE, DocType.DOCSTRCT);
        solrDoc.setField(SolrConstants.METADATATYPE, MetadataGroupType.PERSON);
        solrDoc.setField(SolrConstants.PI, "1234");
        solrDoc.setField(SolrConstants.PI_TOPSTRUCT, "1234");

        StructElement doc = new StructElement(1, solrDoc);

        String url = handler.getThumbnailUrl(doc, 200, 300);
        Assertions.assertEquals("http://external/iiif/image/00000001.tif/full/!200,300/0/default.jpg", url);
    }

    @Test
    void testGetCMSMediaImageApiUrl_legacy() throws UnsupportedEncodingException {

        String legacyApiUrl = "https://viewer.goobi.io/rest/";

        String filename = "image.jpg";
        String viewerHomePath = DataManager.getInstance().getConfiguration().getViewerHome();
        String cmsMediaFolder = DataManager.getInstance().getConfiguration().getCmsMediaFolder();

        Path filepath = Paths.get(viewerHomePath).resolve(cmsMediaFolder).resolve(filename);
        //        String fileUrl = PathConverter.toURI(filepath).toString();
        String fileUrl = "file://" + viewerHomePath + cmsMediaFolder + "/" + filename;
        String encFilepath = BeanUtils.escapeCriticalUrlChracters(fileUrl);
        encFilepath = URLEncoder.encode(encFilepath, "utf-8");

        String thumbUrlLegacy = ThumbnailHandler.getCMSMediaImageApiUrl(filename, legacyApiUrl);
        assertEquals(legacyApiUrl + "image/-/" + encFilepath, thumbUrlLegacy);
    }

    @Test
    void testGetCMSMediaImageApiUrl() {

        String currentApiUrl = "https://viewer.goobi.io/api/v1";

        String filename = "image.jpg";

        String thumbUrlV1 = ThumbnailHandler.getCMSMediaImageApiUrl(filename, currentApiUrl);
        assertEquals(currentApiUrl + ApiUrls.CMS_MEDIA + ApiUrls.CMS_MEDIA_FILES_FILE.replace("{filename}", filename), thumbUrlV1);
    }

    @Test
    void testGetCMSMediaImageApiUrl_withSpaces() {

        String currentApiUrl = "https://viewer.goobi.io/api/v1";

        String filename = "Some PDF.pdf";
        String encFilename = StringTools.encodeUrl(filename);

        String thumbUrlV1 = ThumbnailHandler.getCMSMediaImageApiUrl(filename, currentApiUrl);
        assertEquals(currentApiUrl + ApiUrls.CMS_MEDIA + ApiUrls.CMS_MEDIA_FILES_FILE.replace("{filename}", encFilename), thumbUrlV1);
        assertNotNull(URI.create(thumbUrlV1));
    }

    @Test
    void testCMSMediaThumbnailUrl() {

        String currentApiUrl = "https://viewer.goobi.io/api/v1";

        String filename = "image 01.jpg";
        String escFilename = StringTools.encodeUrl(filename);

        CMSMediaItem item = new CMSMediaItem();
        item.setFileName(filename);

        String thumbUrlV1 = handler.getThumbnailUrl(item, 100, 200);
        thumbUrlV1 = thumbUrlV1.replaceAll("\\?.*", "");
        String iiifPath = ApiUrls.CMS_MEDIA_FILES_FILE_IMAGE_IIIF
                .replace("{region}", "full")
                .replace("{size}", "!100,200")
                .replace("{rotation}", "0")
                .replace("{quality}", "default")
                .replace("{format}", "jpg");
        assertEquals(currentApiUrl + ApiUrls.CMS_MEDIA +
                ApiUrls.CMS_MEDIA_FILES_FILE.replace("{filename}", escFilename) + iiifPath, thumbUrlV1);
    }

    /**
     * @see ThumbnailHandler#getSize(Integer,Integer)
     * @verifies use width only if height null or zero
     */
    @Test
    void getSize_shouldUseWidthOnlyIfHeightNullOrZero() throws Exception {
        Assertions.assertEquals("1,", ThumbnailHandler.getSize(1, null));
        Assertions.assertEquals("1,", ThumbnailHandler.getSize(1, 0));
    }

    /**
     * @see ThumbnailHandler#getSize(Integer,Integer)
     * @verifies use height only if width null or zero
     */
    @Test
    void getSize_shouldUseHeightOnlyIfWidthNullOrZero() throws Exception {
        Assertions.assertEquals(",1", ThumbnailHandler.getSize(null, 1));
        Assertions.assertEquals(",1", ThumbnailHandler.getSize(0, 1));
    }

    /**
     * @see ThumbnailHandler#getSize(Integer,Integer)
     * @verifies use width and height if both non zero
     */
    @Test
    void getSize_shouldUseWidthAndHeightIfBothNonZero() throws Exception {
        Assertions.assertEquals("!1,1", ThumbnailHandler.getSize(1, 1));
    }

    /**
     * @see ThumbnailHandler#getSize(Integer,Integer)
     * @verifies return max if both zero
     */
    @Test
    void getSize_shouldReturnMaxIfBothZero() throws Exception {
        Assertions.assertEquals("max", ThumbnailHandler.getSize(0, 0));
    }
    
    @Test
    void getSize_shouldReturnMaxIfBothNull() throws Exception {
        Assertions.assertEquals("max", ThumbnailHandler.getSize(null, null));
    }

    /**
     * @see ThumbnailHandler#getImagePath(PhysicalElement)
     * @verifies return image thumbnail path correctly
     */
    @Test
    void getImagePath_shouldReturnImageThumbnailPathCorrectly() throws Exception {
        Assertions.assertEquals("00000001.tif",
                new ThumbnailHandler(new IIIFUrlHandler(), "https://example/com/viewer/")
                        .getImagePath(new PhysicalElementBuilder().setFilePath("00000001.tif").setMimeType("image/tiff").build()));
    }

    /**
     * @see ThumbnailHandler#getImagePath(PhysicalElement)
     * @verifies return audio thumbnail path correctly
     */
    @Test
    void getImagePath_shouldReturnAudioThumbnailPathCorrectly() throws Exception {
        // Page thumbnail
        Assertions.assertEquals("00000001.tif",
                new ThumbnailHandler(new IIIFUrlHandler(), "https://example/com/viewer/")
                        .getImagePath(new PhysicalElementBuilder().setFilePath("00000001.tif").setMimeType("audio/mpeg3").build()));
        // Default thumbnail
        Assertions.assertEquals("https://example/com/viewer/thumbnail_audio.jpg",
                new ThumbnailHandler(new IIIFUrlHandler(), "https://example/com/viewer/")
                        .getImagePath(new PhysicalElementBuilder().setMimeType("audio").build()));
    }

    /**
     * @see ThumbnailHandler#getImagePath(PhysicalElement)
     * @verifies return video thumbnail path correctly
     */
    @Test
    void getImagePath_shouldReturnVideoThumbnailPathCorrectly() throws Exception {
        // Page thumbnail
        Assertions.assertEquals("00000001.tif",
                new ThumbnailHandler(new IIIFUrlHandler(), "https://example/com/viewer/")
                        .getImagePath(new PhysicalElementBuilder().setFilePath("00000001.tif").setMimeType("video").build()));
        // Default thumbnail
        Assertions.assertEquals("https://example/com/viewer/thumbnail_video.jpg",
                new ThumbnailHandler(new IIIFUrlHandler(), "https://example/com/viewer/")
                        .getImagePath(new PhysicalElementBuilder().setMimeType("video/webm").build()));
    }

    /**
     * @see ThumbnailHandler#getImagePath(PhysicalElement)
     * @verifies return pdf thumbnail path correctly
     */
    @Test
    void getImagePath_shouldReturnPdfThumbnailPathCorrectly() throws Exception {
        Assertions.assertEquals("https://example/com/viewer/thumbnail_epub.jpg",
                new ThumbnailHandler(new IIIFUrlHandler(), "https://example/com/viewer/")
                        .getImagePath(new PhysicalElementBuilder().setFilePath("00000001.tif").setMimeType("application/pdf").build()));
    }

    /**
     * @see ThumbnailHandler#getImagePath(PhysicalElement)
     * @verifies return 3d object thumbnail path correctly
     */
    @Test
    void getImagePath_shouldReturn3dObjectThumbnailPathCorrectly() throws Exception {
        Assertions.assertEquals("https://example/com/viewer/thumbnail_3d.png",
                new ThumbnailHandler(new IIIFUrlHandler(), "https://example/com/viewer/")
                        .getImagePath(new PhysicalElementBuilder().setFilePath("00000001.tif").setMimeType("application/object").build()));
        Assertions.assertEquals("https://example/com/viewer/thumbnail_3d.png",
                new ThumbnailHandler(new IIIFUrlHandler(), "https://example/com/viewer/")
                        .getImagePath(new PhysicalElementBuilder().setFilePath("00000001.tif").setMimeType("object").build()));
    }

}
