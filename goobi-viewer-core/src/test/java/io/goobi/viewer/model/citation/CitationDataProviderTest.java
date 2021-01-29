package io.goobi.viewer.model.citation;


import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import de.undercouch.citeproc.csl.CSLType;

public class CitationDataProviderTest {
    
    /**
    * @see CitationDataProvider#addItemData(String,Map,CSLType)
    * @verifies add item data correctly
    */
    @Test
    public void addItemData_shouldAddItemDataCorrectly() throws Exception {
        Map<String, String> fields = new HashMap<>();
        fields.put(Citation.AUTHOR, "Zahn, Timothy");
        fields.put(Citation.TITLE, "Thrawn");
        fields.put(Citation.ISSUED, "2017-04-11");
        fields.put(Citation.ISBN, "9780606412148");
        
        CitationDataProvider provider = new CitationDataProvider();
        provider.addItemData("id", fields, CSLType.BOOK);
        Assert.assertNotNull(provider.retrieveItem("id"));
    }
}