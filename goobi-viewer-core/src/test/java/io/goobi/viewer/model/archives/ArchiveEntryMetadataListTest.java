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
package io.goobi.viewer.model.archives;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataParameter;
import io.goobi.viewer.model.metadata.MetadataParameter.MetadataParameterType;
import io.goobi.viewer.solr.SolrConstants;

class ArchiveEntryMetadataListTest extends AbstractSolrEnabledTest {

    /**
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @see populateMetadata(SolrDocument,List)
     * @verifies populate metadata correctly
     */
    @Test
    void populateMetadata_shouldPopulateMetadataCorrectly() throws PresentationException, IndexUnreachableException {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":" + PI_KLEIUNIV, null);
        Assertions.assertNotNull(doc);
        doc.remove("MD_AUTHOR"); // remove from docstruct because archive nodes don't contain MD_VALUE of grouped metadata

        List<Metadata> metadataList = new ArrayList<>(2);
        metadataList.add(new Metadata(SolrConstants.PI, "",
                Collections.singletonList(new MetadataParameter(MetadataParameterType.FIELD, SolrConstants.PI))).setType(1));
        metadataList.add(new Metadata("MD_AUTHOR", "{1}{3}{5}",
                Collections.singletonList(new MetadataParameter(MetadataParameterType.FIELD, "MD_VALUE"))).setType(1).setGroup(true));

        ArchiveEntryMetadataList list = new ArchiveEntryMetadataList("123", doc, metadataList);
        Assertions.assertEquals(2, list.getIdentityStatementAreaList().size());
        Assertions.assertEquals(PI_KLEIUNIV, list.getIdentityStatementAreaList().get(0).getParamValue(SolrConstants.PI)); // regular
        Assertions.assertEquals("Klein, Felix", list.getIdentityStatementAreaList().get(1).getParamValue("MD_VALUE")); // grouped
    }
}
