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
package io.goobi.viewer.managedbeans;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.archives.ArchiveEntry;
import io.goobi.viewer.model.archives.SolrEADParser;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataParameter;
import io.goobi.viewer.model.metadata.MetadataParameter.MetadataParameterType;
import io.goobi.viewer.solr.SolrConstants;

class ArchiveMetadataBeanTest {

    @Test
    void test() throws PresentationException, IndexUnreachableException {
        SolrEADParser parser = new SolrEADParser();
        parser.updateAssociatedRecordMap();
        SolrDocument doc = new SolrDocument(Map.of("MD_TITLE", "Mein Titel", SolrConstants.IDDOC, "0"));
        ArchiveEntry entry = parser.loadNode(1, 0, doc, Collections.emptySet(), false);
        MetadataParameter param = new MetadataParameter();
        param.setKey("MD_TITLE");
        param.setType(MetadataParameterType.FIELD);
        Metadata metadata = new Metadata("Title", "{0}", List.of(param));
        metadata.setType(1);
        ArchiveMetadataBean bean = new ArchiveMetadataBean(List.of(metadata));
        Assertions.assertFalse(bean.isMetadataLoaded(entry.getId()));
        bean.getMetadata(entry);
        Assertions.assertTrue(bean.isMetadataLoaded(entry.getId()));
        Assertions.assertEquals(1, bean.getMetadata(entry).getAllAreaLists().size());
        Assertions.assertEquals("Mein Titel", bean.getMetadata(entry).getAllAreaLists().get(0).getFirstValue());
    }

}
