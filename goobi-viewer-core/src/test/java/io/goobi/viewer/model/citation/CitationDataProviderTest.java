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
package io.goobi.viewer.model.citation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLType;
import io.goobi.viewer.AbstractTest;

public class CitationDataProviderTest extends AbstractTest {

    /**
     * @see CitationDataProvider#addItemData(String,Map,CSLType)
     * @verifies add item data correctly
     */
    @Test
    public void addItemData_shouldAddItemDataCorrectly() throws Exception {
        {
            Map<String, List<String>> fields = new HashMap<>();
            fields.put(CitationDataProvider.AUTHOR, Arrays.asList(new String[] { "Zahn, Timothy" }));
            fields.put(CitationDataProvider.TITLE, Collections.singletonList("Thrawn"));
            fields.put(CitationDataProvider.ISSUED, Collections.singletonList("2017-04-11"));
            fields.put(CitationDataProvider.ISBN, Collections.singletonList("9780606412148"));
            fields.put(CitationDataProvider.URL, Collections.singletonList("https://example.com/9780606412148"));

            CitationDataProvider provider = new CitationDataProvider();
            provider.addItemData("id", fields, CSLType.BOOK);
            CSLItemData itemData = provider.retrieveItem("id");
            Assertions.assertNotNull(itemData);
            Assertions.assertNotNull(itemData.getAuthor());
            Assertions.assertEquals(1, itemData.getAuthor().length);
            Assertions.assertEquals("Zahn", itemData.getAuthor()[0].getFamily());
            Assertions.assertEquals("Timothy", itemData.getAuthor()[0].getGiven());
            Assertions.assertEquals("2017-04-11", itemData.getIssued().getRaw());
            Assertions.assertNull(itemData.getIssued().getDateParts());
            Assertions.assertEquals("https://example.com/9780606412148", itemData.getURL());

        }
        {
            //            Map<String, List<String>> fields = new HashMap<>();
            //            fields.put(CitationDataProvider.AUTHOR, Arrays.asList(new String[] { "Timothy Zahn" }));
            //
            //            CitationDataProvider provider = new CitationDataProvider();
            //            provider.addItemData("id", fields, CSLType.BOOK);
            //            CSLItemData itemData =provider.retrieveItem("id");
            //            Assertions.assertNotNull(itemData);
            //            Assertions.assertNotNull(itemData.getAuthor());
            //            Assertions.assertEquals(1, itemData.getAuthor().length);
            //            Assertions.assertEquals("Zahn", itemData.getAuthor()[0].getFamily());
            //            Assertions.assertEquals("Timothy", itemData.getAuthor()[0].getGiven());
        }
    }

    /**
     * @see CitationDataProvider#addItemData(String,Map,CSLType)
     * @verifies parse years correctly
     */
    @Test
    public void addItemData_shouldParseYearsCorrectly() throws Exception {
        Map<String, List<String>> fields = new HashMap<>();
        fields.put(CitationDataProvider.AUTHOR, Arrays.asList(new String[] { "Zahn, Timothy" }));
        fields.put(CitationDataProvider.TITLE, Collections.singletonList("Thrawn"));
        fields.put(CitationDataProvider.ISSUED, Collections.singletonList("2017"));
        fields.put(CitationDataProvider.ISBN, Collections.singletonList("9780606412148"));

        CitationDataProvider provider = new CitationDataProvider();
        provider.addItemData("id", fields, CSLType.BOOK);
        CSLItemData itemData = provider.retrieveItem("id");
        Assertions.assertNotNull(itemData);
        Assertions.assertNotNull(itemData.getAuthor());
        Assertions.assertEquals(1, itemData.getAuthor().length);
        Assertions.assertEquals("Zahn", itemData.getAuthor()[0].getFamily());
        Assertions.assertEquals("Timothy", itemData.getAuthor()[0].getGiven());
        Assertions.assertNull(itemData.getIssued().getRaw());
        Assertions.assertEquals(2017, itemData.getIssued().getDateParts()[0][0]);

    }
}
