package io.goobi.viewer.model.translations.admin;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.model.translations.admin.TranslationGroup.TranslationGroupType;
import io.goobi.viewer.solr.SolrConstants;

public class SolrFieldValueTranslationGroupItemTest extends AbstractSolrEnabledTest {

    /**
     * @see SolrFieldValueTranslationGroupItem#loadEntries()
     * @verifies load hierarchical entries correctly
     */
    @Test
    public void loadEntries_shouldLoadHierarchicalEntriesCorrectly() throws Exception {
        TranslationGroupItem item = TranslationGroupItem.create(TranslationGroupType.SOLR_FIELD_VALUES, SolrConstants.DC, false);
        item.loadEntries();
        Assert.assertFalse(item.getEntries().isEmpty());
        Map<String, Integer> keys = new HashMap<>();
        for (MessageEntry entry : item.getEntries()) {
            Integer count = keys.get(entry.getKey());
            if (count == null) {
                keys.put(entry.getKey(), 1);
            } else {
                keys.put(entry.getKey(), count + 1);
            }
        }
        Assert.assertEquals(Integer.valueOf(1), keys.get("dcimage")); // Parent keys should only be added once
        Assert.assertEquals(Integer.valueOf(1), keys.get("dcimage.many"));
        Assert.assertEquals(Integer.valueOf(1), keys.get("dcimage.png"));
    }
}