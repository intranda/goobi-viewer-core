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
package io.goobi.viewer.model.iiif.presentation.v2.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.presentation.content.LinkingContent;
import de.intranda.api.iiif.presentation.enums.DcType;
import de.intranda.api.iiif.presentation.v2.Canvas2;
import de.intranda.api.iiif.presentation.v2.Manifest2;
import de.intranda.api.iiif.presentation.v2.Range2;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.api.rest.filters.IIIFPresentationResponseFilter;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;

/**
 * @author Florian
 *
 */
public class ManifestBuilderTest extends AbstractDatabaseAndSolrEnabledTest {


    public static final String PI = "74241";

    @Test
    void test() throws PresentationException, IndexUnreachableException, ViewerConfigurationException, DAOException, URISyntaxException,
            ContentNotFoundException, IOException {

        ManifestBuilder builder = new ManifestBuilder(new ApiUrls("https://viewer.goobi.io/rest/"));
        SequenceBuilder sequenceBuilder = new SequenceBuilder(new ApiUrls("https://viewer.goobi.io/rest/"));
        StructureBuilder structureBuilder = new StructureBuilder(new ApiUrls("https://viewer.goobi.io/rest/"));

        //        SolrDocumentList allDocs = DataManager.getInstance().getSearchIndex().search("PI:*");
        //        for (SolrDocument solrDocument : allDocs) {
        //            String pi = SolrTools.getSingleFieldStringValue(solrDocument, "PI");
        //        }

        List<StructElement> docs = builder.getDocumentWithChildren(PI);
        if (docs.isEmpty()) {
            throw new ContentNotFoundException("No document found for pi " + PI);
        }
        StructElement mainDoc = docs.get(0);
        IPresentationModelElement manifest = builder.generateManifest(mainDoc);
        ((Manifest2) manifest).setContext(IIIFPresentationResponseFilter.CONTEXT_PRESENTATION_2);
        sequenceBuilder.addBaseSequence((Manifest2) manifest, mainDoc, manifest.getId().toString(), null);

        String topLogId = mainDoc.getMetadataValue(SolrConstants.LOGID);
        if (StringUtils.isNotBlank(topLogId)) {
            List<Range2> ranges = structureBuilder.generateStructure(docs, PI, false);
            ranges.forEach(range -> {
                ((Manifest2) manifest).addStructure(range);
            });
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        ObjectWriter writer = mapper.writer().forType(Manifest2.class);
        String json = writer.writeValueAsString(manifest);
        Assertions.assertTrue(StringUtils.isNotBlank(json));
        //            File jsonFile = new File("C:\\opt\\digiverso\\viewer\\manifest.json");
        //            FileUtils.write(jsonFile, json);

    }

    @Test
    void testDeserializeCanvas() throws URISyntaxException, JsonProcessingException {
        Range2 range = new Range2("http://viewer/manifest/1/ranges/1");
        Canvas2 canvas = new Canvas2("http://viewer/manifest/1/canvas/1");
        range.addCanvas(canvas);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        ObjectWriter writer = mapper.writer().forType(Range2.class);
        String json = writer.writeValueAsString(range);
        Assertions.assertTrue(StringUtils.isNotBlank(json));
    }

    @Test
    void getValidViewerRenderingUrl() {
        DataManager.getInstance().getConfiguration().overrideValue("webapi.iiif.rendering.viewer[@enabled]", true);
        Assertions.assertTrue(DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingViewer());

        ApiUrls urls = new ApiUrls("https://viewer.goobi.io/api/v1/");
        ManifestBuilder builder = new ManifestBuilder(urls);
        Manifest2 manifest = new Manifest2(URI.create(urls.getApiUrl() + "/" + PI + "/manifest"));
        StructElement ele = new StructElement();
        ele.setPi(PI);
        ele.setImageNumber(1);
        ele.setLogid("LOG_0003");
        builder.addRenderings(manifest, ele);
        LinkingContent viewerRendering = manifest.getRendering()
                .stream()
                .filter(rend -> rend.getType().equals(DcType.INTERACTIVE_RESOURCE.getLabel()))
                .findFirst()
                .orElse(null);
        assertNotNull(viewerRendering);
        assertEquals("https://viewer.goobi.io/metadata/" + PI + "/1/LOG_0003/", viewerRendering.getId().toString());
    }

}
