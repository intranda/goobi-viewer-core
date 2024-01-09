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
package io.goobi.viewer.model.cms;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.cms.recordnotes.CMSMultiRecordNote;
import io.goobi.viewer.solr.SolrConstants;

/**
 * @author florian
 *
 */
public class CMSMultiRecordNoteTest extends AbstractDatabaseAndSolrEnabledTest {

    @Test
    void testGetRecords() throws DAOException {
        CMSMultiRecordNote note = (CMSMultiRecordNote) DataManager.getInstance().getDao().getRecordNote(5l);
        assertNotNull(note);
        assertTrue(note.getRecords().size() > 0);
    }

    @Test
    void testMatchesRecord() throws DAOException, PresentationException, IndexUnreachableException {
        CMSMultiRecordNote note = (CMSMultiRecordNote) DataManager.getInstance().getDao().getRecordNote(5l);
        assertNotNull(note);
        String containedPI = note.getRecords().get(0);
        //Search for a pi not containted in the records matching the note: Get more PIs from SOLR than matching records exist, then find a pi within that doesn't match the note
        String otherPI = DataManager.getInstance().getSearchIndex().search("PI:*", note.getRecords().size()+1, null, Collections.singletonList(SolrConstants.PI))
        .stream().map(doc -> (String)doc.getFieldValue(SolrConstants.PI))
        .filter(pi -> !note.getRecords().contains(pi))
        .findAny().orElse(null);
        assertNotNull(otherPI);
        assertTrue(note.matchesRecord(containedPI));
        assertFalse(note.matchesRecord(otherPI));
    }

}
