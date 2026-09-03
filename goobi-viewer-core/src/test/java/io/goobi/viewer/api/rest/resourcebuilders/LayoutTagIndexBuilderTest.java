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
package io.goobi.viewer.api.rest.resourcebuilders;

import java.util.List;
import java.util.Locale;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.api.rest.model.LayoutTagIndex;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.solr.SolrConstants;

class LayoutTagIndexBuilderTest extends AbstractTest {

    private static final String PI = "PPN123";

    private static SolrDocument page(int order, String... labels) {
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.ORDER, order);
        for (String label : labels) {
            doc.addField(SolrConstants.MD_LAYOUTTAG, label);
        }
        return doc;
    }

    private static SolrDocumentList docs(SolrDocument... pages) {
        SolrDocumentList list = new SolrDocumentList();
        list.addAll(List.of(pages));
        return list;
    }

    private static LayoutTagIndexBuilder builder() {
        return new LayoutTagIndexBuilder(DataManager.getInstance().getConfiguration(), Locale.GERMAN);
    }

    private static LayoutTagIndex.Tag tag(LayoutTagIndex index, String label) {
        return index.getTags().stream().filter(t -> label.equals(t.getLabel())).findFirst().orElse(null);
    }

    /**
     * @see LayoutTagIndexBuilder#build(String,int,SolrDocumentList)
     * @verifies drop the printed label
     */
    @Test
    void build_shouldDropThePrintedLabel() {
        LayoutTagIndex index = builder().build(PI, 10, docs(page(1, "gr_printed_test", "gr_jacob_grimm"), page(2, "gr_printed_test")));
        Assertions.assertEquals(PI, index.getPi());
        Assertions.assertEquals(10, index.getPageCount());
        Assertions.assertEquals(1, index.getTags().size());
        Assertions.assertEquals("gr_jacob_grimm", index.getTags().get(0).getLabel());
    }

    /**
     * @see LayoutTagIndexBuilder#build(String,int,SolrDocumentList)
     * @verifies collect pages ascending without duplicates
     */
    @Test
    void build_shouldCollectPagesAscendingWithoutDuplicates() {
        LayoutTagIndex index = builder().build(PI, 10, docs(page(7, "gr_jacob_grimm", "gr_jacob_grimm"), page(3, "gr_jacob_grimm"), page(5, "gr_jacob_grimm")));
        Assertions.assertEquals(List.of(3, 5, 7), tag(index, "gr_jacob_grimm").getPages());
    }

    /**
     * @see LayoutTagIndexBuilder#build(String,int,SolrDocumentList)
     * @verifies sort tags by descending page count then label
     */
    @Test
    void build_shouldSortTagsByDescendingPageCountThenLabel() {
        LayoutTagIndex index = builder().build(PI, 10,
                docs(page(1, "zz_rare", "aa_rare", "gr_unterstrichen"), page(2, "gr_unterstrichen"), page(3, "gr_unterstrichen")));
        Assertions.assertEquals(List.of("gr_unterstrichen", "aa_rare", "zz_rare"), index.getTags().stream().map(LayoutTagIndex.Tag::getLabel).toList());
    }

    /**
     * @see LayoutTagIndexBuilder#build(String,int,SolrDocumentList)
     * @verifies resolve display names in configured order
     */
    @Test
    void build_shouldResolveDisplayNamesInConfiguredOrder() {
        LayoutTagIndex index = builder().build(PI, 10, docs(page(1, "gr_jacob_grimm", "gr_unterstrichen", "MD_TITLE", "xx_custom")));
        // configured annotationTag name
        Assertions.assertEquals("Jacob (test)", tag(index, "gr_jacob_grimm").getName());
        // translated wordTag type
        Assertions.assertEquals("Unterstreichungen", tag(index, "gr_unterstrichen").getName());
        // raw label that is a message key
        Assertions.assertEquals("Titel", tag(index, "MD_TITLE").getName());
        // raw label without message key
        Assertions.assertEquals("xx_custom", tag(index, "xx_custom").getName());
    }

    /**
     * @see LayoutTagIndexBuilder#build(String,int,SolrDocumentList)
     * @verifies deliver style classes for configured annotation tags only
     */
    @Test
    void build_shouldDeliverStyleClassesForConfiguredAnnotationTagsOnly() {
        LayoutTagIndex index = builder().build(PI, 10, docs(page(1, "gr_jacob_grimm", "gr_unterstrichen", "xx_custom")));
        Assertions.assertEquals("test-jacob", tag(index, "gr_jacob_grimm").getStyleClass());
        Assertions.assertNull(tag(index, "gr_unterstrichen").getStyleClass());
        Assertions.assertNull(tag(index, "xx_custom").getStyleClass());
    }

    /**
     * @see LayoutTagIndexBuilder#build(String,int,SolrDocumentList)
     * @verifies skip documents without order
     */
    @Test
    void build_shouldSkipDocumentsWithoutOrder() {
        SolrDocument noOrder = new SolrDocument();
        noOrder.addField(SolrConstants.MD_LAYOUTTAG, "gr_jacob_grimm");
        SolrDocument noTags = new SolrDocument();
        noTags.setField(SolrConstants.ORDER, 4);
        LayoutTagIndex index = builder().build(PI, 10, docs(noOrder, noTags, page(2, "gr_jacob_grimm")));
        Assertions.assertEquals(1, index.getTags().size());
        Assertions.assertEquals(List.of(2), tag(index, "gr_jacob_grimm").getPages());
        Assertions.assertTrue(builder().build(PI, 10, null).getTags().isEmpty());
    }
}
