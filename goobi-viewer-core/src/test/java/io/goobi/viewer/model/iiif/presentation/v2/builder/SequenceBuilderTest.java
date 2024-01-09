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

import java.awt.Canvas;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.presentation.enums.AnnotationType;
import de.intranda.api.iiif.presentation.v2.AnnotationList;
import de.intranda.api.iiif.presentation.v2.Canvas2;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * @author Florian
 *
 */
public class SequenceBuilderTest extends AbstractDatabaseAndSolrEnabledTest {

    public static final String PI = PI_KLEIUNIV;
    public static final int ORDER = 1;

    @Test
    public void testAddOtherContent() throws PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException,
            DAOException, ContentNotFoundException, IOException {

        ManifestBuilder manifestBuilder = new ManifestBuilder(new ApiUrls("https://viewer.goobi.io/rest/"));
        SequenceBuilder sequenceBuilder = new SequenceBuilder(new ApiUrls("https://viewer.goobi.io/rest/"));

        List<StructElement> docs = manifestBuilder.getDocumentWithChildren(PI);
        if (docs.isEmpty()) {
            throw new ContentNotFoundException("No document found for pi " + PI);
        }
        StructElement mainDoc = docs.get(0);
        IPresentationModelElement manifest = manifestBuilder.generateManifest(mainDoc);

        PhysicalElement page = sequenceBuilder.getPage(mainDoc, ORDER);

        Canvas2 canvas = sequenceBuilder.generateCanvas(mainDoc.getPi(), page);

        Map<AnnotationType, AnnotationList> annoMap = sequenceBuilder.addOtherContent(mainDoc, page, canvas, true);
        AnnotationList fulltext = annoMap.get(AnnotationType.FULLTEXT);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        ObjectWriter writer = mapper.writer().forType(AnnotationList.class);
        String json = writer.writeValueAsString(fulltext);
        Assertions.assertTrue(StringUtils.isNotBlank(json));
    }

}
