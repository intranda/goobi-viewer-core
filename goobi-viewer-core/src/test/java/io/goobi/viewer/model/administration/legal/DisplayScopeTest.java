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
package io.goobi.viewer.model.administration.legal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.administration.legal.DisplayScope.PageScope;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.solr.SolrSearchIndex;

public class DisplayScopeTest extends AbstractSolrEnabledTest {

    @Test
    void testClone() {
        {
            DisplayScope scope = new DisplayScope(PageScope.ALL, "PI:abc");
            String json = scope.getAsJson();
            DisplayScope clone = new DisplayScope(json);
            assertEquals(scope.getFilterQuery(), clone.getFilterQuery());
            assertEquals(scope.getPageScope(), clone.getPageScope());
        }
        {
            DisplayScope scope = new DisplayScope(PageScope.RECORD, "");
            String json = scope.getAsJson();
            DisplayScope clone = new DisplayScope(json);
            assertEquals(scope.getFilterQuery(), clone.getFilterQuery());
            assertEquals(scope.getPageScope(), clone.getPageScope());
        }
    }

    @Test
    void testAppliesToPage() throws PresentationException, IndexUnreachableException {
        {
            PageType pageType = PageType.search;
            DisplayScope scope = new DisplayScope(PageScope.ALL, "");
            assertTrue(scope.appliesToPage(pageType, null, null));
        }
        {
            PageType pageType = PageType.admin;
            DisplayScope scope = new DisplayScope(PageScope.ALL, "");
            assertFalse(scope.appliesToPage(pageType, null, null));
        }
        {
            PageType pageType = PageType.search;
            DisplayScope scope = new DisplayScope(PageScope.RECORD, "");
            assertFalse(scope.appliesToPage(pageType, null, null));
        }
        {
            PageType pageType = PageType.viewImage;
            DisplayScope scope = new DisplayScope(PageScope.RECORD, "");
            assertTrue(scope.appliesToPage(pageType, "AC1234", null));
        }
        {
            SolrSearchIndex searchIndex = Mockito.mock(SolrSearchIndex.class);
            Mockito.when(searchIndex.getHitCount(Mockito.anyString())).thenAnswer(inv -> {
               String arg = inv.getArgument(0, String.class);
               if(arg.contains("PI1")) {
                   return 1l;
               } else {
                   return 0l;
               }
            });
            PageType pageType = PageType.viewImage;
            DisplayScope scope = new DisplayScope(PageScope.RECORD, "PI:1");
            assertTrue(scope.appliesToPage(pageType, "PI1", searchIndex));
            assertFalse(scope.appliesToPage(pageType, "PI2", searchIndex));
        }
    }
    

    @Test
    void testAppliesToPage_emptyArgs() throws PresentationException, IndexUnreachableException {
        DisplayScope scope = new DisplayScope(PageScope.RECORD, "PI:1");
        PageType pageType = PageType.viewImage;
        SolrSearchIndex searchIndex = DataManager.getInstance().getSearchIndex();
        assertFalse(scope.appliesToPage(pageType, "-", searchIndex));
    }

}
