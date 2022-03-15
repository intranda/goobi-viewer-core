package io.goobi.viewer.model.administration.legal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mockito.Mockito;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.administration.legal.DisplayScope.PageScope;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.solr.SolrSearchIndex;

public class DisplayScopeTest {

    @Test
    public void testClone() {
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
    public void testAppliesToPage() throws PresentationException, IndexUnreachableException {
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
            Mockito.when(searchIndex.count(Mockito.anyString())).thenAnswer(inv -> {
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

}
