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
package io.goobi.viewer.model.iiif.search;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.solr.common.SolrDocument;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.intranda.api.annotation.oa.TextQuoteSelector;
import de.intranda.api.iiif.search.SearchHit;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.model.annotation.Comment;
import io.goobi.viewer.model.iiif.search.model.AnnotationResultList;
import io.goobi.viewer.model.iiif.search.parser.AbstractSearchParser;

/**
 * @author florian
 *
 */
public class SearchResultConverterTest {

    String text = "A bird in the hand is worth\ntwo in the bush.";
    SearchResultConverter converter;
    String pi = "12345";
    int pageNo = 1;
    String restUrl;
    
    Path altoFile = Paths.get("resources/test/data/sample_alto.xml");
    AltoDocument doc;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));
        restUrl = DataManager.getInstance().getConfiguration().getRestApiUrl();
        converter = new SearchResultConverter(URI.create(restUrl + "iiif/search"), URI.create(restUrl), pi, pageNo);
        doc = AltoDocument.getDocumentFromFile(altoFile.toFile());

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link io.goobi.viewer.model.iiif.search.SearchResultConverter#convertCommentToHit(java.lang.String, java.lang.String, io.goobi.viewer.model.annotation.Comment)}.
     */
    @Test
    public void testConvertCommentToHit() {
        Comment comment = new Comment(pi, pageNo, null, text, null);
        comment.setId(1l);
        
        String query = "in";
        String queryRegex = AbstractSearchParser.getQueryRegex(query);
        
        SearchHit hit = converter.convertCommentToHit(queryRegex, pi, comment);
        
        Assert.assertNotNull(hit);
        Assert.assertEquals(restUrl + "webannotation/comments/12345/1/1/", hit.getAnnotations().get(0).getId().toString());
        Assert.assertEquals("in", hit.getMatch());
        TextQuoteSelector selector1 = (TextQuoteSelector) hit.getSelectors().get(0);
        TextQuoteSelector selector2 = (TextQuoteSelector) hit.getSelectors().get(1);
        Assert.assertEquals("A bird ", selector1.getPrefix());
        Assert.assertEquals(" the hand is worth\ntwo in the bush.", selector1.getSuffix());
        Assert.assertEquals("A bird in the hand is worth\ntwo ", selector2.getPrefix());
        Assert.assertEquals(" the bush.", selector2.getSuffix());
    }

    /**
     * Test method for {@link io.goobi.viewer.model.iiif.search.SearchResultConverter#convertUGCToHit(java.lang.String, org.apache.solr.common.SolrDocument)}.
     */
    @Test
    public void testConvertUGCToHit() {
        SolrDocument ugc = new SolrDocument();
        ugc.setField(SolrConstants.UGCTERMS, text);
        ugc.setField(SolrConstants.UGCTYPE, "ADDRESS");
        ugc.setField(SolrConstants.PI_TOPSTRUCT, pi);
        ugc.setField(SolrConstants.ORDER, pageNo);
        ugc.setField(SolrConstants.IDDOC, 123456789);
        String query = "in";
        String queryRegex = AbstractSearchParser.getQueryRegex(query);
        SearchHit hit = converter.convertUGCToHit(queryRegex, ugc);
        Assert.assertNotNull(hit);
        Assert.assertEquals(restUrl + "annotations/123456789", hit.getAnnotations().get(0).getId().toString());
        Assert.assertEquals("in", hit.getMatch());
        TextQuoteSelector selector1 = (TextQuoteSelector) hit.getSelectors().get(0);
        TextQuoteSelector selector2 = (TextQuoteSelector) hit.getSelectors().get(1);
        Assert.assertEquals("A bird ", selector1.getPrefix());
        Assert.assertEquals(" the hand is worth\ntwo in the bush.", selector1.getSuffix());
        Assert.assertEquals("A bird in the hand is worth\ntwo ", selector2.getPrefix());
        Assert.assertEquals(" the bush.", selector2.getSuffix());
    }

    /**
     * Test method for {@link io.goobi.viewer.model.iiif.search.SearchResultConverter#convertMetadataToHit(java.lang.String, java.lang.String, org.apache.solr.common.SolrDocument)}.
     */
    @Test
    public void testConvertMetadataToHit() {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.TITLE, text);
        doc.setField(SolrConstants.PI_TOPSTRUCT, pi);
        doc.setField(SolrConstants.PI, pi);
        doc.setField(SolrConstants.THUMBPAGENO, pageNo);
        doc.setField(SolrConstants.IDDOC, 123456789);
        
        String query = "in";
        String queryRegex = AbstractSearchParser.getQueryRegex(query);
        SearchHit hit = converter.convertMetadataToHit(queryRegex, SolrConstants.TITLE, doc);
        
        Assert.assertNotNull(hit);
        Assert.assertEquals(restUrl + "iiif/manifests/12345/METADATA/12345/MD_TITLE/", hit.getAnnotations().get(0).getId().toString());
        Assert.assertEquals("in", hit.getMatch());
        TextQuoteSelector selector1 = (TextQuoteSelector) hit.getSelectors().get(0);
        TextQuoteSelector selector2 = (TextQuoteSelector) hit.getSelectors().get(1);
        Assert.assertEquals("A bird ", selector1.getPrefix());
        Assert.assertEquals(" the hand is worth\ntwo in the bush.", selector1.getSuffix());
        Assert.assertEquals("A bird in the hand is worth\ntwo ", selector2.getPrefix());
        Assert.assertEquals(" the bush.", selector2.getSuffix());
    }

    /**
     * Test method for {@link io.goobi.viewer.model.iiif.search.SearchResultConverter#getAnnotationsFromAlto(de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument, java.lang.String)}.
     */
    @Test
    public void testGetAnnotationsFromAlto() {
        
        String query = "Hollywood";
        String queryRegex = AbstractSearchParser.getQueryRegex(query);
        
        AnnotationResultList results = converter.getAnnotationsFromAlto(doc, queryRegex);
        Assert.assertEquals(9, results.hits.size());
        
        SearchHit hit1 = results.hits.get(0);
        Assert.assertEquals("Hollywood!", hit1.getMatch());
        Assert.assertEquals(restUrl + "iiif/manifests/12345/list/1/ALTO/Word_14", hit1.getAnnotations().get(0).getId().toString());

    }

    /**
     * Test method for {@link io.goobi.viewer.model.iiif.search.SearchResultConverter#getAnnotationsFromFulltext(java.lang.String, java.lang.String, java.lang.Integer, java.lang.String, long, int, int)}.
     */
    @Test
    public void testGetAnnotationsFromFulltext() {
        
        String query = "in";
        String queryRegex = AbstractSearchParser.getQueryRegex(query);
        
        AnnotationResultList results = converter.getAnnotationsFromFulltext(text, pi, pageNo, queryRegex, 0, 0, 1000);
        Assert.assertEquals(1, results.hits.size());
        Assert.assertEquals(2, results.hits.get(0).getSelectors().size());
    }

}
