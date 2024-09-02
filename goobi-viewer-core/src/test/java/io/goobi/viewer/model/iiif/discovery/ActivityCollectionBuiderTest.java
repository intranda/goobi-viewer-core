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
package io.goobi.viewer.model.iiif.discovery;

import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.intranda.api.iiif.discovery.Activity;
import de.intranda.api.iiif.discovery.ActivityType;
import de.intranda.api.iiif.discovery.OrderedCollection;
import de.intranda.api.iiif.discovery.OrderedCollectionPage;
import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

class ActivityCollectionBuiderTest extends AbstractSolrEnabledTest {

    /**
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @see ActivityCollectionBuider#getDocs(Long,Long)
     * @verifies only return topstructs
     */
    @Test
    void getDocs_shouldOnlyReturnTopstructs() throws PresentationException, IndexUnreachableException {
        ActivityCollectionBuilder builder = new ActivityCollectionBuilder(
                DataManager.getInstance().getRestApiManager().getDataApiManager().orElse(null), DataManager.getInstance().getSearchIndex(), 10);
        SolrDocumentList docs = builder.getDocs(null, null);
        Assertions.assertNotNull(docs);
        Assertions.assertFalse(docs.isEmpty());
        for (SolrDocument doc : docs) {
            Assertions.assertNotNull(SolrTools.getSingleFieldValue(doc, SolrConstants.PI));
            Assertions.assertNotNull(SolrTools.getSingleFieldValue(doc, SolrConstants.DATECREATED));
        }
    }

    @Test
    void buildCollection_countAllEvents() throws PresentationException, IndexUnreachableException {

        FacetField field1 = new FacetField("DATEUPDATED");
        field1.add("200", 2);
        FacetField field2 = new FacetField("DATECREATED");
        field2.add("100", 1);
        field2.add("150", 1);
        QueryResponse queryResponseMock = Mockito.mock(QueryResponse.class);
        Mockito.when(queryResponseMock.getFacetFields()).thenReturn(List.of(field1, field2));

        SolrDocument doc1 = new SolrDocument(Map.of("IDDOC", "1", "PI", "A", "DATECREATED", 100l, "DATEUPDATED", List.of(200l)));
        SolrDocument doc2 = new SolrDocument(Map.of("IDDOC", "2", "PI", "B", "DATECREATED", 150l, "DATEUPDATED", List.of(200l)));
        SolrDocumentList documentList = new SolrDocumentList();
        documentList.add(doc1);
        documentList.add(doc2);
        QueryResponse queryResponseMock2 = Mockito.mock(QueryResponse.class);
        Mockito.when(queryResponseMock2.getResults()).thenReturn(documentList);

        SolrSearchIndex searchIndexMock = Mockito.mock(SolrSearchIndex.class);
        Mockito.when(searchIndexMock.searchFacetsAndStatistics(Mockito.anyString(), Mockito.anyList(), Mockito.anyList(), Mockito.anyInt(),
                Mockito.anyBoolean())).thenReturn(queryResponseMock);
        Mockito.when(searchIndexMock.search(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyList(), Mockito.anyList(),
                Mockito.anyList(),
                Mockito.anyList(), Mockito.anyMap()))
                .thenReturn(queryResponseMock2);

        ActivityCollectionBuilder builder =
                new ActivityCollectionBuilder(DataManager.getInstance().getRestApiManager().getDataApiManager().orElse(null), searchIndexMock, 10);
        OrderedCollection<Activity> collection = builder.buildCollection();
        Assertions.assertEquals(4, collection.getTotalItems());
        Assertions.assertEquals(collection.getFirst().getId(), collection.getLast().getId());
        OrderedCollectionPage<Activity> pageFirst = builder.buildPage(0);
        Assertions.assertEquals(4, pageFirst.getOrderedItems().size());

        Assertions.assertEquals(ActivityType.CREATE, pageFirst.getOrderedItems().get(0).getType());
        Assertions.assertEquals(DateTools.getLocalDateTimeFromMillis(100, false), pageFirst.getOrderedItems().get(0).getEndTime());

        Assertions.assertEquals(ActivityType.UPDATE, pageFirst.getOrderedItems().get(3).getType());
        Assertions.assertEquals(DateTools.getLocalDateTimeFromMillis(200, false), pageFirst.getOrderedItems().get(3).getEndTime());

    }

    @Test
    void buildCollection_countAllEventsAfterDate() throws PresentationException, IndexUnreachableException {

        FacetField field1 = new FacetField("DATEUPDATED");
        field1.add("200", 2);
        FacetField field2 = new FacetField("DATECREATED");
        field2.add("100", 1);
        field2.add("150", 1);
        QueryResponse queryResponseMock = Mockito.mock(QueryResponse.class);
        Mockito.when(queryResponseMock.getFacetFields()).thenReturn(List.of(field1, field2));

        SolrDocument doc1 = new SolrDocument(Map.of("IDDOC", "1", "PI", "A", "DATECREATED", 100l, "DATEUPDATED", List.of(200l)));
        SolrDocument doc2 = new SolrDocument(Map.of("IDDOC", "2", "PI", "B", "DATECREATED", 150l, "DATEUPDATED", List.of(200l)));
        SolrDocumentList documentList = new SolrDocumentList();
        documentList.add(doc1);
        documentList.add(doc2);
        QueryResponse queryResponseMock2 = Mockito.mock(QueryResponse.class);
        Mockito.when(queryResponseMock2.getResults()).thenReturn(documentList);

        SolrSearchIndex searchIndexMock = Mockito.mock(SolrSearchIndex.class);
        Mockito.when(searchIndexMock.searchFacetsAndStatistics(Mockito.anyString(), Mockito.anyList(), Mockito.anyList(), Mockito.anyInt(),
                Mockito.anyBoolean())).thenReturn(queryResponseMock);
        Mockito.when(searchIndexMock.search(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyList(), Mockito.anyList(),
                Mockito.anyList(),
                Mockito.anyList(), Mockito.anyMap()))
                .thenReturn(queryResponseMock2);

        ActivityCollectionBuilder builder =
                new ActivityCollectionBuilder(DataManager.getInstance().getRestApiManager().getDataApiManager().orElse(null), searchIndexMock, 10);
        builder.setStartDate(DateTools.getLocalDateTimeFromMillis(150, false));
        OrderedCollection<Activity> collection = builder.buildCollection();
        Assertions.assertEquals(3, collection.getTotalItems());
        Assertions.assertEquals(collection.getFirst().getId(), collection.getLast().getId());
        OrderedCollectionPage<Activity> pageFirst = builder.buildPage(0);
        Assertions.assertEquals(3, pageFirst.getOrderedItems().size());

        Assertions.assertEquals(ActivityType.CREATE, pageFirst.getOrderedItems().get(0).getType());
        Assertions.assertEquals(DateTools.getLocalDateTimeFromMillis(150, false), pageFirst.getOrderedItems().get(0).getEndTime());

        Assertions.assertEquals(ActivityType.UPDATE, pageFirst.getOrderedItems().get(2).getType());
        Assertions.assertEquals(DateTools.getLocalDateTimeFromMillis(200, false), pageFirst.getOrderedItems().get(2).getEndTime());

    }
}
