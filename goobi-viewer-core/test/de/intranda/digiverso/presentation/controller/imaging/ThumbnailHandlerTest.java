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
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;
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
    public void testPage() {
        PhysicalElement page = new PhysicalElement("PHYS_0001", "00000001.tif", 1, "Seite 1", "urn:234235:3423", "http://purl", "1234", "image/tiff", null);

        String url = handler.getThumbnailUrl(page, 200, 300);
        System.out.println(url);
        Assert.assertEquals("http://localhost:8080/viewer/rest/image/1234/00000001.tif/full/!200,300/0/default.jpg?compression=30", url);
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
        System.out.println(url);
        Assert.assertEquals("http://localhost:8080/viewer/rest/image/1234/00000001.tif/full/!200,300/0/default.jpg?compression=30", url);
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
        System.out.println(url);
        Assert.assertEquals("http://localhost:8080/viewer/rest/image/-/http:U002FU002FexternalU002FiiifU002FimageU002F00000001.tif/full/!200,300/0/default.jpg?compression=30", url);
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
        System.out.println(url);
        Assert.assertEquals("http://external/iiif/image/00000001.tif/full/!200,300/0/default.jpg", url);
   }


}
