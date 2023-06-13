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
package io.goobi.viewer.model.search;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.SolrDocument;
import org.junit.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrConstants;

public class SearchHitFactoryTest extends AbstractSolrEnabledTest {

    @Test
    public void createSearchHit_findWithUmlaut() throws PresentationException, IndexUnreachableException {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.IDDOC, Long.toString(1l));
        doc.setField("MD_CREATOR", "Norden");
        doc.setField("MD_PUBLISHER", "Nørre");
        Map<String, Set<String>> searchTerms = Collections.singletonMap(SolrConstants.DEFAULT, Collections.singleton("Nörde~1"));
        SearchHit hit = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.GERMAN).createSearchHit(doc, null, null, null, null);
        assertEquals(1, hit.getFoundMetadata().size());
    }

    @Test
    public void createSearchHit_findUmlaute() throws PresentationException, IndexUnreachableException {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.IDDOC, Long.toString(1l));
        doc.setField("MD_CREATOR", "Nörden");
        doc.setField("MD_PUBLISHER", "Nørre");
        Map<String, Set<String>> searchTerms = Collections.singletonMap(SolrConstants.DEFAULT, Collections.singleton("Norde~1"));
        SearchHit hit = new SearchHitFactory(searchTerms, null, null, 0, null, Locale.GERMAN).createSearchHit(doc, null, null, null, null);
        assertEquals(1, hit.getFoundMetadata().size());
    }
}