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
package io.goobi.viewer.model.misc;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * @author florian
 *
 */
class DCRecordWriterTest {

    private static final String RECORD_REFERENCE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<record xmlns:dc=\"http://purl.org/dc/elements/1.1/\">" +
            "<dc:title>Titel</dc:title>" +
            "<dc:identifier>ID</dc:identifier>" +
            "</record>";

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    public void tearDown() throws Exception {
    }

    @Test
    void testWrite() {
        DCRecordWriter writer = new DCRecordWriter();
        writer.addDCMetadata("title", "Titel");
        writer.addDCMetadata("identifier", "ID");

        String xml = writer.getAsString().replaceAll("[\n\r]+",  "").replaceAll("\\s+", " ");
        Assertions.assertEquals(RECORD_REFERENCE, xml);
    }

}
