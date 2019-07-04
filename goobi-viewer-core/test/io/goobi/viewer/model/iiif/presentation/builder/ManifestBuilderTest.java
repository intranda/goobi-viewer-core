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
package io.goobi.viewer.model.iiif.presentation.builder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Assert;
import org.junit.Test;

import com.ctc.wstx.util.StringUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import de.intranda.api.iiif.presentation.Collection;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.presentation.Manifest;
import de.intranda.api.iiif.presentation.Range;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * @author Florian
 *
 */
public class ManifestBuilderTest {

    public static final String PI = "1019375248";
    
    @Test
    public void test() throws PresentationException, IndexUnreachableException, ViewerConfigurationException, DAOException, URISyntaxException, ContentNotFoundException, IOException {
        DataManager.getInstance().injectConfiguration(new Configuration("src/config_viewer.xml"));
        
        
        ManifestBuilder builder = new ManifestBuilder(URI.create("https://viewer.goobi.io"), URI.create("https://viewer.goobi.io/rest/"));
        SequenceBuilder sequenceBuilder = new SequenceBuilder(URI.create("https://viewer.goobi.io"), URI.create("https://viewer.goobi.io/rest/"));
        StructureBuilder structureBuilder = new StructureBuilder(URI.create("https://viewer.goobi.io"), URI.create("https://viewer.goobi.io/rest/"));

        
        List<StructElement> docs = builder.getDocumentWithChildren(PI);
        if (docs.isEmpty()) {
            throw new ContentNotFoundException("No document found for pi " + PI);
        }
        StructElement mainDoc = docs.get(0);
        IPresentationModelElement manifest = builder.generateManifest(mainDoc);

        sequenceBuilder.addBaseSequence((Manifest) manifest, mainDoc, manifest.getId().toString());

            String topLogId = mainDoc.getMetadataValue(SolrConstants.LOGID);
            if (StringUtils.isNotBlank(topLogId)) {
                List<Range> ranges = structureBuilder.generateStructure(docs, PI, false);
                ranges.forEach(range -> {
                    ((Manifest) manifest).addStructure(range);
                });
            }
    
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            ObjectWriter writer = mapper.writer().forType(Manifest.class);
            String json = writer.writeValueAsString(manifest);
            Assert.assertTrue(StringUtils.isNotBlank(json));
//            File jsonFile = new File("C:\\opt\\digiverso\\viewer\\manifest.json");
//            FileUtils.write(jsonFile, json);
            
    }

}
