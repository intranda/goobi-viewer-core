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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;

class ArchiveParserTest {

    /**
     * @see ArchiveParser#getAssociatedRecordPis(SolrSearchIndex)
     * @verifies keep first record if several records are linked to the same node
     */
    @Test
    void getAssociatedRecordPis_shouldKeepFirstRecordIfSeveralRecordsAreLinkedToTheSameNode() throws Exception {
        SolrDocumentList docs = new SolrDocumentList();
        docs.add(new SolrDocument(Map.of(SolrConstants.EAD_NODE_ID, "node1", SolrConstants.PI, "PPN1", SolrConstants.BOOL_IMAGEAVAILABLE, true)));
        docs.add(new SolrDocument(Map.of(SolrConstants.EAD_NODE_ID, "node1", SolrConstants.PI, "PPN2", SolrConstants.BOOL_IMAGEAVAILABLE, false)));
        docs.add(new SolrDocument(Map.of(SolrConstants.EAD_NODE_ID, "node2", SolrConstants.PI, "PPN3", SolrConstants.BOOL_IMAGEAVAILABLE, false)));
        SolrSearchIndex searchIndex = Mockito.mock(SolrSearchIndex.class);
        Mockito.when(searchIndex.search(ArgumentMatchers.anyString(), ArgumentMatchers.<List<String>> any())).thenReturn(docs);

        Map<String, Entry<String, Boolean>> result = ArchiveParser.getAssociatedRecordPis(searchIndex);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("PPN1", result.get("node1").getKey());
        Assertions.assertTrue(result.get("node1").getValue());
        Assertions.assertEquals("PPN3", result.get("node2").getKey());
    }

    /**
     * @see ArchiveParser#updateAssociatedRecordMapIfChanged(SolrSearchIndex)
     * @verifies load map if not yet loaded
     */
    @Test
    void updateAssociatedRecordMapIfChanged_shouldLoadMapIfNotYetLoaded() throws Exception {
        SolrSearchIndex searchIndex = mockLinkedRecords(1, 1000L);
        ArchiveParser parser = new SolrEADParser();

        Assertions.assertTrue(parser.updateAssociatedRecordMapIfChanged(searchIndex));
        Assertions.assertEquals("PPN1", parser.associatedRecordMap.get("node1").getKey());
    }

    /**
     * @see ArchiveParser#updateAssociatedRecordMapIfChanged(SolrSearchIndex)
     * @verifies not reload map if linked records are unchanged
     */
    @Test
    void updateAssociatedRecordMapIfChanged_shouldNotReloadMapIfLinkedRecordsAreUnchanged() throws Exception {
        SolrSearchIndex searchIndex = mockLinkedRecords(1, 1000L);
        ArchiveParser parser = new SolrEADParser();
        parser.updateAssociatedRecordMapIfChanged(searchIndex);

        Assertions.assertFalse(parser.updateAssociatedRecordMapIfChanged(searchIndex));
        Mockito.verify(searchIndex, Mockito.times(1)).search(ArgumentMatchers.anyString(), ArgumentMatchers.<List<String>> any());
    }

    /**
     * @see ArchiveParser#updateAssociatedRecordMapIfChanged(SolrSearchIndex)
     * @verifies reload map if linked records changed
     */
    @Test
    void updateAssociatedRecordMapIfChanged_shouldReloadMapIfLinkedRecordsChanged() throws Exception {
        ArchiveParser parser = new SolrEADParser();
        parser.updateAssociatedRecordMapIfChanged(mockLinkedRecords(1, 1000L));

        // Record reindexed
        Assertions.assertTrue(parser.updateAssociatedRecordMapIfChanged(mockLinkedRecords(1, 2000L)));
        // Record added or removed
        Assertions.assertTrue(parser.updateAssociatedRecordMapIfChanged(mockLinkedRecords(2, 2000L)));
    }

    private static SolrSearchIndex mockLinkedRecords(long numFound, long latestDateIndexed) throws Exception {
        SolrDocumentList fingerprintDocs = new SolrDocumentList();
        fingerprintDocs.add(new SolrDocument(Map.of(SolrConstants.DATEINDEXED, List.of(latestDateIndexed - 1, latestDateIndexed))));
        fingerprintDocs.setNumFound(numFound);
        SolrDocumentList recordDocs = new SolrDocumentList();
        recordDocs.add(new SolrDocument(Map.of(SolrConstants.EAD_NODE_ID, "node1", SolrConstants.PI, "PPN1", SolrConstants.BOOL_IMAGEAVAILABLE, true)));
        SolrSearchIndex searchIndex = Mockito.mock(SolrSearchIndex.class);
        Mockito.when(searchIndex.search(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(fingerprintDocs);
        Mockito.when(searchIndex.search(ArgumentMatchers.anyString(), ArgumentMatchers.<List<String>> any())).thenReturn(recordDocs);
        return searchIndex;
    }
}
