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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;

class ArchiveEntryTest extends AbstractTest {

    /**
     * @see SolrEADParser(ArchiveEntry)
     * @verifies clone entry correctly
     */
    @Test
    void ArchiveEntry_shouldCloneEntryCorrectly() {
        ArchiveEntry parent = new ArchiveEntry(0, 0, null);
        ArchiveEntry orig = new ArchiveEntry(1, 1, null);
        ArchiveEntry child = new ArchiveEntry(2, 2, null);
        child.setId("child1");
        orig.setParentNode(parent);
        orig.getSubEntryList().add(child);

        orig.setId("id123");
        orig.setAssociatedRecordPi("PPN456");
        orig.setContainsImage(true);
        orig.setValid(true);
        orig.setLabel("label");
        orig.setNodeType("type");
        orig.setDescriptionLevel("desclevel");
        orig.setTopstructPi("PPN123");
        orig.setLogId("LOG_0001");
        orig.getAccessConditions().add("restricted");

        ArchiveEntry copy = new ArchiveEntry(orig, parent);
        Assertions.assertEquals(parent, copy.getParentNode());
        Assertions.assertEquals(1, copy.getSubEntryList().size());
        Assertions.assertEquals(child.getId(), copy.getSubEntryList().get(0).getId());
        Assertions.assertNotEquals(child, copy.getSubEntryList().get(0)); // also a clone

        Assertions.assertEquals(orig.getId(), copy.getId());
        Assertions.assertEquals(orig.getAssociatedRecordPi(), copy.getAssociatedRecordPi());
        Assertions.assertEquals(orig.isContainsImage(), copy.isContainsImage());
        Assertions.assertEquals(orig.isValid(), copy.isValid());
        Assertions.assertEquals(orig.getLabel(), copy.getLabel());
        Assertions.assertEquals(orig.getNodeType(), copy.getNodeType());
        Assertions.assertEquals(orig.getDescriptionLevel(), copy.getDescriptionLevel());
        Assertions.assertEquals(orig.getTopstructPi(), copy.getTopstructPi());
        Assertions.assertEquals(orig.getLogId(), copy.getLogId());
        Assertions.assertEquals(1, copy.getAccessConditions().size());
        Assertions.assertEquals(orig.getAccessConditions().get(0), copy.getAccessConditions().get(0));
    }
}
