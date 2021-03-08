package io.goobi.viewer.model.citation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

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

            CitationDataProvider provider = new CitationDataProvider();
            provider.addItemData("id", fields, CSLType.BOOK);
            CSLItemData itemData =provider.retrieveItem("id");
            Assert.assertNotNull(itemData);
            Assert.assertNotNull(itemData.getAuthor());
            Assert.assertEquals(1, itemData.getAuthor().length);
            Assert.assertEquals("Zahn", itemData.getAuthor()[0].getFamily());
            Assert.assertEquals("Timothy", itemData.getAuthor()[0].getGiven());
        }
        {
            Map<String, List<String>> fields = new HashMap<>();
            fields.put(CitationDataProvider.AUTHOR, Arrays.asList(new String[] { "Timothy Zahn" }));

            CitationDataProvider provider = new CitationDataProvider();
            provider.addItemData("id", fields, CSLType.BOOK);
            CSLItemData itemData =provider.retrieveItem("id");
            Assert.assertNotNull(itemData);
            Assert.assertNotNull(itemData.getAuthor());
            Assert.assertEquals(1, itemData.getAuthor().length);
            Assert.assertEquals("Zahn", itemData.getAuthor()[0].getFamily());
            Assert.assertEquals("Timothy", itemData.getAuthor()[0].getGiven());
        }
    }
}