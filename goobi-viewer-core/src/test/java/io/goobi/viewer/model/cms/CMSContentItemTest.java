/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.cms;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;

import javax.faces.context.ExternalContext;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.goobi.viewer.model.cms.CMSContentItem.CMSContentItemType;

/**
 * @author florian
 *
 */
public class CMSContentItemTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test if the "cloning" constructor, which takes another CMSContentItem as argument, 
     * faithfully reproduces the argument as a deep copy
     */
    @Test
    public void testClone() {
        
        CMSMediaItem item1 = new CMSMediaItem();
        
        CMSContentItem original = new CMSContentItem(CMSContentItemType.HTML);
        original.setBaseCollection("base-collection");
        original.setCategories(new ArrayList(Collections.singleton(new CMSCategory("category"))));
        original.setCollectionBaseLevels(2);
        original.setCollectionDisplayParents(false);
        original.setCollectionField("collection-field");
        original.setCollectionOpenExpanded(true);
        original.setComponent("component");
        original.setDisplayEmptySearchResults(true);
        original.setElementsPerPage(3);
        original.setGlossaryName("glossary-name");
        original.setHtmlFragment("<h1>fragment</h1>");
        original.setId(1l);
        original.setIgnoreCollections("ignore-collection");
        original.setItemId("item-id");
        original.setMediaItem(item1);
        original.setMetadataFields("MD_*");
        original.setNestedPagesCount(5);
        original.setNumberOfImportantTiles(6);
        original.setNumberOfTiles(7);
        original.setOwnerPageLanguageVersion(new CMSPageLanguageVersion("de"));
        original.setSearchPrefix("DC:all");
        original.setSearchType(2);
        original.setSolrQuery("PI:*");
        original.setTocPI("PPN1");
        
        CMSContentItem copy = new CMSContentItem(original, null);
        
        Assert.assertEquals(original.getBaseCollection(), copy.getBaseCollection());
       
        Assert.assertEquals(original.getCategories().size(), copy.getCategories().size());
        original.getCategories().add(new CMSCategory("other-category"));
        Assert.assertNotEquals(original.getCategories().size(), copy.getCategories().size());
        
        Assert.assertEquals(original.getCollectionBaseLevels(), copy.getCollectionBaseLevels());
        
        Assert.assertEquals(original.isCollectionDisplayParents(), copy.isCollectionDisplayParents());
        
        Assert.assertEquals(original.getCollectionField(), copy.getCollectionField());

        Assert.assertEquals(original.isCollectionOpenExpanded(), copy.isCollectionOpenExpanded());
        
        Assert.assertEquals(original.getComponent(), copy.getComponent());
        
        Assert.assertEquals(original.getCollectionField(), copy.getCollectionField());
        
        Assert.assertEquals(original.isDisplayEmptySearchResults(), copy.isDisplayEmptySearchResults());
         
        Assert.assertEquals(original.getElementsPerPage(), copy.getElementsPerPage());
        
        Assert.assertEquals(original.getGlossaryName(), copy.getGlossaryName());
        
        Assert.assertEquals(original.getHtmlFragment(), copy.getHtmlFragment());

        Assert.assertEquals(original.getId(), copy.getId());

        Assert.assertEquals(original.getIgnoreCollections(), copy.getIgnoreCollections());

        Assert.assertEquals(original.getItemId(), copy.getItemId());

        Assert.assertEquals(original.getMediaItem(), copy.getMediaItem());  //no deep copy for media items

        Assert.assertEquals(original.getMetadataFields(), copy.getMetadataFields());
        
        Assert.assertEquals(original.getNumberOfImportantTiles(), copy.getNumberOfImportantTiles());

        Assert.assertEquals(original.getNumberOfTiles(), copy.getNumberOfTiles());

        Assert.assertNotEquals(original.getOwnerPageLanguageVersion(), copy.getOwnerPageLanguageVersion()); //owner is not passed by original

        Assert.assertEquals(original.getSearchPrefix(), copy.getSearchPrefix());

        Assert.assertEquals(original.getSearchType(), copy.getSearchType());

        Assert.assertEquals(original.getSolrQuery(), copy.getSolrQuery());

        Assert.assertEquals(original.getTocPI(), copy.getTocPI());

    }
    
    /**
     * Test that label, order, mandatory, mediaFilter, mode, inlineHelp and preview are taken from associated {@link CMSContentItemTemplate}
     * 
     */
    @Test
    public void testGetTemplateProperties() {
        
        CMSContentItemTemplate template = new CMSContentItemTemplate(CMSContentItemType.HTML);
        template.setInlineHelp("inline help");
        template.setItemLabel("label");
        template.setMandatory(true);
        template.setMediaFilter("all");
        template.setMode(ContentItemMode.expanded);
        template.setOrder(6);
        template.setPreview(true);
        
        CMSContentItem item = Mockito.spy(CMSContentItem.class);
        Mockito.doReturn(template).when(item).getItemTemplate();
        
        Assert.assertEquals("inline help", item.getInlineHelp());
        Assert.assertEquals("label", item.getItemLabel());
        Assert.assertEquals(true, item.isMandatory());
        Assert.assertEquals("all", item.getMediaFilter());
        Assert.assertEquals(ContentItemMode.expanded, item.getMode());
        Assert.assertEquals(6, item.getOrder());
        Assert.assertEquals(true, item.isPreview());

        
    }

}
