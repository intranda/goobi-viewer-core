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
package io.goobi.viewer.model.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.model.search.SearchQueryGroup.SearchQueryGroupOperator;
import io.goobi.viewer.model.search.SearchQueryItem.SearchItemOperator;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.termbrowsing.BrowseTerm;
import io.goobi.viewer.model.termbrowsing.BrowseTermComparator;
import io.goobi.viewer.model.termbrowsing.BrowsingMenuFieldConfig;
import io.goobi.viewer.model.viewer.StringPair;

public class SearchHelperTest extends AbstractDatabaseAndSolrEnabledTest {

    public static final String LOREM_IPSUM =
            "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    @BeforeClass
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();
        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("src/test/resources/config_viewer.test.xml"));
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        SearchHelper.collectionBlacklistFilterSuffix = null;
    }

    /**
     * @see SearchHelper#searchAutosuggestion(String)
     * @verifies return autosuggestions correctly
     */
    @Test
    public void searchAutosuggestion_shouldReturnAutosuggestionsCorrectly() throws Exception {
        List<String> values = SearchHelper.searchAutosuggestion("klein", null);
        Assert.assertFalse(values.isEmpty());
    }

    /**
     * @see SearchHelper#searchAutosuggestion(String,String)
     * @verifies filter by collection correctly
     */
    @Test
    public void searchAutosuggestion_shouldFilterByCollectionCorrectly() throws Exception {
        FacetItem item = new FacetItem(SolrConstants.FACET_DC + ":varia", true);
        List<String> values = SearchHelper.searchAutosuggestion("kartenundplaene", Collections.singletonList(item));
        Assert.assertTrue(values.isEmpty());
    }

    /**
     * @see SearchHelper#searchAutosuggestion(String,List,List)
     * @verifies filter by facet correctly
     */
    @Test
    public void searchAutosuggestion_shouldFilterByFacetCorrectly() throws Exception {
        FacetItem item = new FacetItem(SolrConstants.TITLE + ":something", false);
        List<String> values = SearchHelper.searchAutosuggestion("kartenundplaene", Collections.singletonList(item));
        Assert.assertTrue(values.isEmpty());
    }

    /**
     * @see SearchHelper#findAllCollectionsFromField(String,String,boolean,boolean,boolean,boolean)
     * @verifies find all collections
     */
    @Test
    public void findAllCollectionsFromField_shouldFindAllCollections() throws Exception {
        // First, make sure the collection blacklist always comes from the same config file;
        Map<String, CollectionResult> collections =
                SearchHelper.findAllCollectionsFromField(SolrConstants.DC, SolrConstants.DC, null, true, true, ".");
        Assert.assertEquals(51, collections.size());
        List<String> keys = new ArrayList<>(collections.keySet());
        // Collections.sort(keys);
        //        for (String key : keys) {
        //            switch (key) {
        //                case ("dc3d"):
        //                    Assert.assertEquals(Long.valueOf(2), collections.get(key).getCount());
        //                    break;
        //                case ("dcaccesscondition"):
        //                    Assert.assertEquals(Long.valueOf(6), collections.get(key).getCount());
        //                    break;
        //                case ("dcaccesscondition.fulltextlocked"):
        //                    Assert.assertEquals(Long.valueOf(2), collections.get(key).getCount());
        //                    break;
        //                case ("dcaccesscondition.movingwall"):
        //                    Assert.assertEquals(Long.valueOf(1), collections.get(key).getCount());
        //                    break;
        //                case ("dcaccesscondition.pdflocked"):
        //                    Assert.assertEquals(Long.valueOf(2), collections.get(key).getCount());
        //                    break;
        //                case ("dcannotations"):
        //                    Assert.assertEquals(Long.valueOf(15), collections.get(key).getCount());
        //                    break;
        //                case ("dcannotations.generated"):
        //                    Assert.assertEquals(Long.valueOf(15), collections.get(key).getCount());
        //                    break;
        //                case ("dcannotations.geocoordinates"):
        //                    Assert.assertEquals(Long.valueOf(3), collections.get(key).getCount());
        //                    break;
        //                case ("dcauthoritydata"):
        //                    Assert.assertEquals(Long.valueOf(12), collections.get(key).getCount());
        //                    break;
        //                case ("dcauthoritydata.gnd"):
        //                    Assert.assertEquals(Long.valueOf(5), collections.get(key).getCount());
        //                    break;
        //                case ("dcauthoritydata.provenance"):
        //                    Assert.assertEquals(Long.valueOf(1), collections.get(key).getCount());
        //                    break;
        //                case ("dcauthoritydata.viaf"):
        //                    Assert.assertEquals(Long.valueOf(4), collections.get(key).getCount());
        //                    break;
        //                case ("dcboarndigital"):
        //                    Assert.assertEquals(Long.valueOf(2), collections.get(key).getCount());
        //                    break;
        //                case ("dcconvolute"):
        //                    Assert.assertEquals(Long.valueOf(6), collections.get(key).getCount());
        //                    break;
        //                case ("dcdownload"):
        //                    Assert.assertEquals(Long.valueOf(3), collections.get(key).getCount());
        //                    break;
        //                // TODO others
        //                case ("dcnewspaper"):
        //                    Assert.assertEquals(Long.valueOf(18), collections.get(key).getCount());
        //                    break;
        //                case ("dcrelations"):
        //                    Assert.assertEquals(Long.valueOf(120), collections.get(key).getCount());
        //                    break;
        //                default:
        //                    //                    Assert.fail("Unknown collection name: " + key);
        //                    break;
        //            }
        //    }

    }

    /**
     * @see SearchHelper#getPersonalFilterQuerySuffix(User,String)
     * @verifies construct suffix correctly
     */
    @Test
    public void getPersonalFilterQuerySuffix_shouldConstructSuffixCorrectly() throws Exception {
        String suffix = SearchHelper.getPersonalFilterQuerySuffix(null, null);
        Assert.assertEquals(" -(" + SolrConstants.ACCESSCONDITION + ":\"license type 1 name\" AND YEAR:[* TO 3000]) -" + SolrConstants.ACCESSCONDITION
                + ":\"license type 3 name\" -" + SolrConstants.ACCESSCONDITION + ":\"license type 4 name\"", suffix);
    }

    /**
     * @see SearchHelper#getPersonalFilterQuerySuffix(User,String)
     * @verifies construct suffix correctly if user has license privilege
     */
    @Test
    public void getPersonalFilterQuerySuffix_shouldConstructSuffixCorrectlyIfUserHasLicensePrivilege() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(2);
        String suffix = SearchHelper.getPersonalFilterQuerySuffix(user, null);
        Assert.assertTrue(!suffix.contains("license type 1 name"));
    }

    /**
     * @see SearchHelper#getPersonalFilterQuerySuffix(User,String)
     * @verifies construct suffix correctly if user has overriding license privilege
     */
    @Test
    public void getPersonalFilterQuerySuffix_shouldConstructSuffixCorrectlyIfUserHasOverridingLicensePrivilege() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(2);
        String suffix = SearchHelper.getPersonalFilterQuerySuffix(user, null);
        Assert.assertTrue(!suffix.contains("license type 4 name"));
    }

    /**
     * @see SearchHelper#getPersonalFilterQuerySuffix(User,String)
     * @verifies construct suffix correctly if ip range has license privilege
     */
    @Test
    public void getPersonalFilterQuerySuffix_shouldConstructSuffixCorrectlyIfIpRangeHasLicensePrivilege() throws Exception {
        String suffix = SearchHelper.getPersonalFilterQuerySuffix(null, "127.0.0.1");
        Assert.assertEquals("", suffix);
    }

    /**
     * @see SearchHelper#truncateFulltext(List,String)
     * @verifies truncate string to 200 chars if no terms are given and add prefix and suffix
     */
    @Test
    public void truncateFulltext_shouldTruncateStringTo200CharsIfNoTermsAreGivenAndAddPrefixAndSuffix() throws Exception {
    }

    /**
     * @see SearchHelper#truncateFulltext(List,String)
     * @verifies make terms bold if found in text
     */
    @Test
    public void truncateFulltext_shouldMakeTermsBoldIfFoundInText() throws Exception {
        String original = LOREM_IPSUM;
        String[] terms = { "ipsum", "tempor", "labore" };
        List<String> truncated = SearchHelper.truncateFulltext(new HashSet<>(Arrays.asList(terms)), original, 200, true, true);
        Assert.assertFalse(truncated.isEmpty());
        //        Assert.assertTrue(truncated.get(0).contains("<span class=\"search-list--highlight\">ipsum</span>"));
        Assert.assertTrue(truncated.get(0).contains("<span class=\"search-list--highlight\">tempor</span>"));
        //        Assert.assertTrue(truncated.get(0).contains("<span class=\"search-list--highlight\">labore</span>"));
        // TODO The other two terms aren't highlighted when using random length phrase
    }

    /**
     * @see SearchHelper#truncateFulltext(List,String)
     * @verifies not add prefix and suffix to text
     */
    @Test
    public void truncateFulltext_shouldNotAddPrefixAndSuffixToText() throws Exception {
        String original = "text";
        List<String> truncated = SearchHelper.truncateFulltext(null, original, 200, true, true);
        Assert.assertFalse(truncated.isEmpty());
        Assert.assertEquals("text", truncated.get(0));
    }

    /**
     * @see SearchHelper#truncateFulltext(List,String)
     * @verifies truncate string to 200 chars if no terms are given
     */
    @Test
    public void truncateFulltext_shouldTruncateStringTo200CharsIfNoTermsAreGiven() throws Exception {
        String original = LOREM_IPSUM;
        List<String> truncated = SearchHelper.truncateFulltext(null, original, 200, true, true);
        Assert.assertFalse(truncated.isEmpty());
        Assert.assertEquals(original.substring(0, 200), truncated.get(0));
    }

    /**
     * @see SearchHelper#truncateFulltext(List,String)
     * @verifies truncate string to 200 chars if no term has been found
     */
    @Test
    public void truncateFulltext_shouldTruncateStringTo200CharsIfNoTermHasBeenFound() throws Exception {
        String original = LOREM_IPSUM;
        String[] terms = { "boogers" };
        {
            List<String> truncated = SearchHelper.truncateFulltext(new HashSet<>(Arrays.asList(terms)), original, 200, true, true);
            Assert.assertFalse(truncated.isEmpty());
            Assert.assertEquals(original.substring(0, 200), truncated.get(0));
        }
        {
            List<String> truncated = SearchHelper.truncateFulltext(new HashSet<>(Arrays.asList(terms)), original, 200, true, false);
            Assert.assertTrue(truncated.isEmpty());
        }
    }

    /**
     * @see SearchHelper#truncateFulltext(List,String)
     * @verifies remove unclosed HTML tags
     */
    @Test
    public void truncateFulltext_shouldRemoveUnclosedHTMLTags() throws Exception {
        List<String> truncated = SearchHelper.truncateFulltext(null, "Hello <a href", 200, true, true);
        Assert.assertFalse(truncated.isEmpty());
        Assert.assertEquals("Hello", truncated.get(0));
        truncated = SearchHelper.truncateFulltext(null, "Hello <a href ...> and then <b", 200, true, true);
        Assert.assertEquals("Hello and then", truncated.get(0));
    }

    /**
     * @see SearchHelper#truncateFulltext(Set,String,int,boolean)
     * @verifies return multiple match fragments correctly
     */
    @Test
    public void truncateFulltext_shouldReturnMultipleMatchFragmentsCorrectly() throws Exception {
        String original = LOREM_IPSUM;
        String[] terms = { "in" };
        List<String> truncated = SearchHelper.truncateFulltext(new HashSet<>(Arrays.asList(terms)), original, 50, false, true);
        Assert.assertEquals(7, truncated.size());
        for (String fragment : truncated) {
            Assert.assertTrue(fragment.contains("<span class=\"search-list--highlight\">in</span>"));
        }
    }

    /**
     * @see SearchHelper#truncateFulltext(Set,String,int,boolean)
     * @verifies replace line breaks with spaces
     */
    @Test
    public void truncateFulltext_shouldReplaceLineBreaksWithSpaces() throws Exception {
        String original = "one<br>two<br>three";
        String[] terms = { "two" };
        List<String> truncated = SearchHelper.truncateFulltext(new HashSet<>(Arrays.asList(terms)), original, 50, false, true);
        Assert.assertEquals(1, truncated.size());
        for (String fragment : truncated) {
            Assert.assertTrue(fragment.contains("<span class=\"search-list--highlight\">two</span>"));
        }
    }

    /**
     * @see SearchHelper#truncateFulltext(Set,String,int,boolean,boolean)
     * @verifies highlight multi word terms while removing stopwords
     */
    @Test
    public void truncateFulltext_shouldHighlightMultiWordTermsWhileRemovingStopwords() throws Exception {
        String original = "funky beats";
        String[] terms = { "two beats one" };
        List<String> truncated = SearchHelper.truncateFulltext(new HashSet<>(Arrays.asList(terms)), original, 50, false, true);
        Assert.assertEquals(1, truncated.size());
        for (String fragment : truncated) {
            Assert.assertTrue(fragment.contains("<span class=\"search-list--highlight\">beats</span>"));
        }
    }

    /**
     * @see SearchHelper#extractSearchTermsFromQuery(String)
     * @verifies extract all values from query except from NOT blocks
     */
    @Test
    public void extractSearchTermsFromQuery_shouldExtractAllValuesFromQueryExceptFromNOTBlocks() throws Exception {
        Map<String, Set<String>> result = SearchHelper.extractSearchTermsFromQuery(
                "(MD_X:value1 OR MD_X:value2 OR (SUPERDEFAULT:value3 AND :value4:)) AND SUPERFULLTEXT:\"hello-world\" AND SUPERUGCTERMS:\"comment\" AND NOT(MD_Y:value_not)",
                null);
        Assert.assertEquals(4, result.size());
        {
            Set<String> terms = result.get("MD_X");
            Assert.assertNotNull(terms);
            Assert.assertEquals(2, terms.size());
            Assert.assertTrue(terms.contains("value1"));
            Assert.assertTrue(terms.contains("value2"));
        }
        {
            Set<String> terms = result.get(SolrConstants.DEFAULT);
            Assert.assertNotNull(terms);
            Assert.assertEquals(2, terms.size());
            Assert.assertTrue(terms.contains("value3"));
            Assert.assertTrue(terms.contains(":value4:"));
        }
        {
            Set<String> terms = result.get(SolrConstants.FULLTEXT);
            Assert.assertNotNull(terms);
            Assert.assertEquals(1, terms.size());
            Assert.assertTrue(terms.contains("hello-world"));
        }
        {
            Set<String> terms = result.get(SolrConstants.UGCTERMS);
            Assert.assertNotNull(terms);
            Assert.assertEquals(1, terms.size());
            Assert.assertTrue(terms.contains("comment"));
        }
        Assert.assertNull(result.get("MD_Y"));

    }

    /**
     * @see SearchHelper#extractSearchTermsFromQuery(String)
     * @verifies handle multiple phrases in query correctly
     */
    @Test
    public void extractSearchTermsFromQuery_shouldHandleMultiplePhrasesInQueryCorrectly() throws Exception {
        Map<String, Set<String>> result =
                SearchHelper.extractSearchTermsFromQuery("(MD_A:\"value1\" OR MD_B:\"value1\" OR MD_C:\"value2\" OR MD_D:\"value2\")", null);
        Assert.assertEquals(4, result.size());
        {
            Set<String> terms = result.get("MD_A");
            Assert.assertNotNull(terms);
            Assert.assertEquals(1, terms.size());
            Assert.assertTrue(terms.contains("value1"));
        }
        {
            Set<String> terms = result.get("MD_B");
            Assert.assertNotNull(terms);
            Assert.assertEquals(1, terms.size());
            Assert.assertTrue(terms.contains("value1"));
        }
        {
            Set<String> terms = result.get("MD_C");
            Assert.assertNotNull(terms);
            Assert.assertEquals(1, terms.size());
            Assert.assertTrue(terms.contains("value2"));
        }
        {
            Set<String> terms = result.get("MD_D");
            Assert.assertNotNull(terms);
            Assert.assertEquals(1, terms.size());
            Assert.assertTrue(terms.contains("value2"));
        }
    }

    /**
     * @see SearchHelper#extractSearchTermsFromQuery(String,String)
     * @verifies skip discriminator value
     */
    @Test
    public void extractSearchTermsFromQuery_shouldSkipDiscriminatorValue() throws Exception {
        Map<String, Set<String>> result =
                SearchHelper.extractSearchTermsFromQuery("(MD_A:\"value1\" OR MD_B:\"value1\" OR MD_C:\"value2\" OR MD_D:\"value3\")", "value1");
        Assert.assertEquals(2, result.size());
        {
            Set<String> terms = result.get("MD_C");
            Assert.assertNotNull(terms);
            Assert.assertEquals(1, terms.size());
            Assert.assertTrue(terms.contains("value2"));
        }
        {
            Set<String> terms = result.get("MD_D");
            Assert.assertNotNull(terms);
            Assert.assertEquals(1, terms.size());
            Assert.assertTrue(terms.contains("value3"));
        }
    }

    /**
     * @see SearchHelper#extractSearchTermsFromQuery(String,String)
     * @verifies remove truncation
     */
    @Test
    public void extractSearchTermsFromQuery_shouldRemoveTruncation() throws Exception {
        Map<String, Set<String>> result = SearchHelper.extractSearchTermsFromQuery("MD_A:*foo*", null);
        Assert.assertEquals(1, result.size());
        {
            Set<String> terms = result.get("MD_A");
            Assert.assertNotNull(terms);
            Assert.assertEquals(1, terms.size());
            Assert.assertTrue(terms.contains("foo"));
        }
    }

    /**
     * @see SearchHelper#extractSearchTermsFromQuery(String)
     * @verifies throw IllegalArgumentException if query is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void extractSearchTermsFromQuery_shouldThrowIllegalArgumentExceptionIfQueryIsNull() throws Exception {
        SearchHelper.extractSearchTermsFromQuery(null, null);
    }

    /**
     * @see SearchHelper#generateCollectionBlacklistFilterSuffix()
     * @verifies construct suffix correctly
     */
    @Test
    public void generateCollectionBlacklistFilterSuffix_shouldConstructSuffixCorrectly() throws Exception {
        String suffix = SearchHelper.generateCollectionBlacklistFilterSuffix(SolrConstants.DC);
        Assert.assertEquals(" -" + SolrConstants.DC + ":collection1 -" + SolrConstants.DC + ":collection2", suffix);
    }

    /**
     * @see SearchHelper#populateCollectionBlacklistFilterSuffixes(String)
     * @verifies populate all mode correctly
     */
    @Test
    public void populateCollectionBlacklistFilterSuffixes_shouldPopulateAllModeCorrectly() throws Exception {
    }

    /**
     * @see SearchHelper#checkCollectionInBlacklist(String,List)
     * @verifies match simple collections correctly
     */
    @Test
    public void checkCollectionInBlacklist_shouldMatchSimpleCollectionsCorrectly() throws Exception {
        {
            Set<String> blacklist = new HashSet<>(Collections.singletonList("a"));
            Assert.assertTrue(SearchHelper.checkCollectionInBlacklist("a", blacklist, "."));
            Assert.assertFalse(SearchHelper.checkCollectionInBlacklist("z", blacklist, "."));
        }
        {
            Set<String> blacklist = new HashSet<>(Collections.singletonList("a.b.c.d"));
            Assert.assertTrue(SearchHelper.checkCollectionInBlacklist("a.b.c.d", blacklist, "."));
            Assert.assertFalse(SearchHelper.checkCollectionInBlacklist("a.b.c.z", blacklist, "."));
        }
    }

    /**
     * @see SearchHelper#checkCollectionInBlacklist(String,List)
     * @verifies match subcollections correctly
     */
    @Test
    public void checkCollectionInBlacklist_shouldMatchSubcollectionsCorrectly() throws Exception {
        Set<String> blacklist = new HashSet<>(Collections.singletonList("a.b"));
        Assert.assertTrue(SearchHelper.checkCollectionInBlacklist("a.b.c.d", blacklist, "."));
        Assert.assertFalse(SearchHelper.checkCollectionInBlacklist("a.z", blacklist, "."));
    }

    /**
     * @see SearchHelper#checkCollectionInBlacklist(String,List)
     * @verifies throw IllegalArgumentException if dc is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void checkCollectionInBlacklist_shouldThrowIllegalArgumentExceptionIfDcIsNull() throws Exception {
        SearchHelper.checkCollectionInBlacklist(null, new HashSet<>(Collections.singletonList("a*")), ".");
    }

    /**
     * @see SearchHelper#checkCollectionInBlacklist(String,List)
     * @verifies throw IllegalArgumentException if blacklist is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void checkCollectionInBlacklist_shouldThrowIllegalArgumentExceptionIfBlacklistIsNull() throws Exception {
        SearchHelper.checkCollectionInBlacklist("a", null, ".");
    }

    /**
     * @see SearchHelper#getDiscriminatorFieldFilterSuffix(String)
     * @verifies construct subquery correctly
     */
    @Test
    public void getDiscriminatorFieldFilterSuffix_shouldConstructSubqueryCorrectly() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        nh.setSubThemeDiscriminatorValue("val");
        Assert.assertEquals(" +fie:val", SearchHelper.getDiscriminatorFieldFilterSuffix(nh, "fie"));
    }

    /**
     * @see SearchHelper#getDiscriminatorFieldFilterSuffix(String)
     * @verifies return empty string if discriminator value is empty or hyphen
     */
    @Test
    public void getDiscriminatorFieldFilterSuffix_shouldReturnEmptyStringIfDiscriminatorValueIsEmptyOrHyphen() throws Exception {
        NavigationHelper nh = new NavigationHelper();
        Assert.assertEquals("", SearchHelper.getDiscriminatorFieldFilterSuffix(nh, "fie"));
        nh.setSubThemeDiscriminatorValue("-");
        Assert.assertEquals("", SearchHelper.getDiscriminatorFieldFilterSuffix(nh, "fie"));
    }

    /**
     * @see SearchHelper#defacetifyField(String)
     * @verifies defacetify correctly
     */
    @Test
    public void defacetifyField_shouldDefacetifyCorrectly() throws Exception {
        Assert.assertEquals(SolrConstants.DC, SearchHelper.defacetifyField(SolrConstants.FACET_DC));
        Assert.assertEquals(SolrConstants.DOCSTRCT, SearchHelper.defacetifyField("FACET_DOCSTRCT"));
        //        Assert.assertEquals(SolrConstants.DOCSTRCT, SearchHelper.defacetifyField("FACET_SUPERDOCSTRCT"));
        Assert.assertEquals("MD_TITLE", SearchHelper.defacetifyField("FACET_TITLE"));
    }

    /**
     * @see SearchHelper#facetifyField(String)
     * @verifies facetify correctly
     */
    @Test
    public void facetifyField_shouldFacetifyCorrectly() throws Exception {
        Assert.assertEquals(SolrConstants.FACET_DC, SearchHelper.facetifyField(SolrConstants.DC));
        Assert.assertEquals("FACET_DOCSTRCT", SearchHelper.facetifyField(SolrConstants.DOCSTRCT));
        //        Assert.assertEquals("FACET_SUPERDOCSTRCT", SearchHelper.facetifyField(SolrConstants.SUPERDOCSTRCT));
        Assert.assertEquals("FACET_TITLE", SearchHelper.facetifyField("MD_TITLE_UNTOKENIZED"));
    }

    /**
     * @see SearchHelper#facetifyList(List)
     * @verifies facetify correctly
     */
    @Test
    public void facetifyList_shouldFacetifyCorrectly() throws Exception {
        List<String> result = SearchHelper.facetifyList(Arrays.asList(new String[] { SolrConstants.DC, "MD_TITLE_UNTOKENIZED" }));
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(SolrConstants.FACET_DC, result.get(0));
        Assert.assertEquals("FACET_TITLE", result.get(1));
    }

    /**
     * @see SearchHelper#sortifyField(String)
     * @verifies sortify correctly
     */
    @Test
    public void sortifyField_shouldSortifyCorrectly() throws Exception {
        Assert.assertEquals("SORT_DC", SearchHelper.sortifyField(SolrConstants.DC));
        Assert.assertEquals("SORT_DOCSTRCT", SearchHelper.sortifyField(SolrConstants.DOCSTRCT));
        Assert.assertEquals("SORT_TITLE", SearchHelper.sortifyField("MD_TITLE_UNTOKENIZED"));
    }

    /**
     * @see SearchHelper#normalizeField(String)
     * @verifies normalize correctly
     */
    @Test
    public void normalizeField_shouldNormalizeCorrectly() throws Exception {
        Assert.assertEquals("MD_FOO", SearchHelper.normalizeField("MD_FOO_UNTOKENIZED"));
    }

    /**
     * @see SearchHelper#adaptField(String,String)
     * @verifies apply prefix correctly
     */
    @Test
    public void adaptField_shouldApplyPrefixCorrectly() throws Exception {
        Assert.assertEquals("SORT_DC", SearchHelper.adaptField(SolrConstants.DC, "SORT_"));
        Assert.assertEquals("SORT_FOO", SearchHelper.adaptField("MD_FOO", "SORT_"));
    }

    /**
     * @see SearchHelper#adaptField(String,String)
     * @verifies not apply prefix to regular fields if empty
     */
    @Test
    public void adaptField_shouldNotApplyPrefixToRegularFieldsIfEmpty() throws Exception {
        Assert.assertEquals("MD_FOO", SearchHelper.adaptField("MD_FOO", ""));
    }

    /**
     * @see SearchHelper#adaptField(String,String)
     * @verifies remove untokenized correctly
     */
    @Test
    public void adaptField_shouldRemoveUntokenizedCorrectly() throws Exception {
        Assert.assertEquals("SORT_FOO", SearchHelper.adaptField("MD_FOO_UNTOKENIZED", "SORT_"));
    }

    /**
     * @see SearchHelper#getAllSuffixes(HttpSession,boolean,boolean)
     * @verifies add static suffix
     */
    @Test
    public void getAllSuffixes_shouldAddStaticSuffix() throws Exception {
        String suffix = SearchHelper.getAllSuffixes(null, null, true, false, false);
        Assert.assertNotNull(suffix);
        Assert.assertTrue(suffix.contains(DataManager.getInstance().getConfiguration().getStaticQuerySuffix()));
    }

    /**
     * @see SearchHelper#getAllSuffixes(HttpServletRequest,boolean,boolean,boolean)
     * @verifies not add static suffix if not requested
     */
    @Test
    public void getAllSuffixes_shouldNotAddStaticSuffixIfNotRequested() throws Exception {
        String suffix = SearchHelper.getAllSuffixes(null, null, false, false, false);
        Assert.assertNotNull(suffix);
        Assert.assertFalse(suffix.contains(DataManager.getInstance().getConfiguration().getStaticQuerySuffix()));
    }

    /**
     * @see SearchHelper#getAllSuffixes(HttpSession,boolean,boolean)
     * @verifies add collection blacklist suffix
     */
    @Test
    public void getAllSuffixes_shouldAddCollectionBlacklistSuffix() throws Exception {

        String suffix = SearchHelper.getAllSuffixes(false, null);
        Assert.assertNotNull(suffix);
        Assert.assertTrue(suffix.contains(" -" + SolrConstants.DC + ":collection1 -" + SolrConstants.DC + ":collection2"));
    }

    //    /**
    //     * @see SearchHelper#getAllSuffixes(HttpSession,boolean,boolean)
    //     * @verifies add discriminator value suffix
    //     */
    //    @Test
    //    public void getAllSuffixes_shouldAddDiscriminatorValueSuffix() throws Exception {
    //        FacesContext facesContext = ContextMocker.mockFacesContext();
    //        ExternalContext externalContext = Mockito.mock(ExternalContext.class);
    //        Map<String, Object> sessionMap = new HashMap<>();
    //        Mockito.when(facesContext.getExternalContext())
    //                .thenReturn(externalContext);
    //        Mockito.when(externalContext.getSessionMap())
    //                .thenReturn(sessionMap);
    //
    //        try {
    //            NavigationHelper nh = new NavigationHelper();
    //            nh.setSubThemeDiscriminatorValue("dvalue");
    //            sessionMap.put("navigationHelper", nh);
    //
    //            String suffix = SearchHelper.getAllSuffixes(null, false, true);
    //            Assert.assertNotNull(suffix);
    //            Assert.assertTrue(suffix.contains(" AND " + DataManager.getInstance()
    //                    .getConfiguration()
    //                    .getSubthemeDiscriminatorField() + ":dvalue"));
    //        } finally {
    //            // Reset the mock because otherwise the discriminator value will persist for other tests
    //            Mockito.reset(externalContext);
    //        }
    //    }

    /**
     * @see SearchHelper#generateExpandQuery(List,Set)
     * @verifies generate query correctly
     */
    @Test
    public void generateExpandQuery_shouldGenerateQueryCorrectly() throws Exception {
        List<String> fields = Arrays.asList(new String[] { SolrConstants.DEFAULT, SolrConstants.FULLTEXT, SolrConstants.NORMDATATERMS,
                SolrConstants.UGCTERMS, SolrConstants.CMS_TEXT_ALL });
        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "one", "two" })));
        searchTerms.put(SolrConstants.FULLTEXT, new HashSet<>(Arrays.asList(new String[] { "two", "three" })));
        searchTerms.put(SolrConstants.NORMDATATERMS, new HashSet<>(Arrays.asList(new String[] { "four", "five" })));
        searchTerms.put(SolrConstants.UGCTERMS, new HashSet<>(Arrays.asList(new String[] { "six" })));
        searchTerms.put(SolrConstants.CMS_TEXT_ALL, new HashSet<>(Arrays.asList(new String[] { "seven" })));
        Assert.assertEquals(
                " +(" + SolrConstants.DEFAULT + ":(one OR two) OR " + SolrConstants.FULLTEXT + ":(two OR three) OR " + SolrConstants.NORMDATATERMS
                        + ":(four OR five) OR " + SolrConstants.UGCTERMS + ":six OR " + SolrConstants.CMS_TEXT_ALL + ":seven)",
                SearchHelper.generateExpandQuery(fields, searchTerms, false));
    }

    /**
     * @see SearchHelper#generateExpandQuery(List,Map)
     * @verifies return empty string if no fields match
     */
    @Test
    public void generateExpandQuery_shouldReturnEmptyStringIfNoFieldsMatch() throws Exception {
        List<String> fields = Arrays.asList(new String[] { SolrConstants.DEFAULT, SolrConstants.FULLTEXT, SolrConstants.NORMDATATERMS,
                SolrConstants.UGCTERMS, SolrConstants.CMS_TEXT_ALL });
        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put("MD_TITLE", new HashSet<>(Arrays.asList(new String[] { "one", "two" })));

        Assert.assertEquals("", SearchHelper.generateExpandQuery(fields, searchTerms, false));
    }

    /**
     * @see SearchHelper#generateExpandQuery(List,Map)
     * @verifies skip reserved fields
     */
    @Test
    public void generateExpandQuery_shouldSkipReservedFields() throws Exception {
        List<String> fields =
                Arrays.asList(new String[] { SolrConstants.DEFAULT, SolrConstants.FULLTEXT, SolrConstants.NORMDATATERMS, SolrConstants.UGCTERMS,
                        SolrConstants.CMS_TEXT_ALL, SolrConstants.PI_TOPSTRUCT, SolrConstants.PI_ANCHOR, SolrConstants.DC, SolrConstants.DOCSTRCT });
        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "one", "two" })));
        searchTerms.put(SolrConstants.FULLTEXT, new HashSet<>(Arrays.asList(new String[] { "two", "three" })));
        searchTerms.put(SolrConstants.NORMDATATERMS, new HashSet<>(Arrays.asList(new String[] { "four", "five" })));
        searchTerms.put(SolrConstants.UGCTERMS, new HashSet<>(Arrays.asList(new String[] { "six" })));
        searchTerms.put(SolrConstants.CMS_TEXT_ALL, new HashSet<>(Arrays.asList(new String[] { "seven" })));
        searchTerms.put(SolrConstants.PI_ANCHOR, new HashSet<>(Arrays.asList(new String[] { "eight" })));
        searchTerms.put(SolrConstants.PI_TOPSTRUCT, new HashSet<>(Arrays.asList(new String[] { "nine" })));
        Assert.assertEquals(
                " +(" + SolrConstants.DEFAULT + ":(one OR two) OR " + SolrConstants.FULLTEXT + ":(two OR three) OR " + SolrConstants.NORMDATATERMS
                        + ":(four OR five) OR " + SolrConstants.UGCTERMS + ":six OR " + SolrConstants.CMS_TEXT_ALL + ":seven)",
                SearchHelper.generateExpandQuery(fields, searchTerms, false));
    }

    /**
     * @see SearchHelper#generateExpandQuery(List,Map)
     * @verifies not escape asterisks
     */
    @Test
    public void generateExpandQuery_shouldNotEscapeAsterisks() throws Exception {
        List<String> fields = Arrays.asList(new String[] { SolrConstants._CALENDAR_DAY });
        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants._CALENDAR_DAY, new HashSet<>(Arrays.asList(new String[] { "*", })));
        Assert.assertEquals(" +(YEARMONTHDAY:*)", SearchHelper.generateExpandQuery(fields, searchTerms, false));
    }

    /**
     * @see SearchHelper#generateExpandQuery(List,Map)
     * @verifies escape reserved characters
     */
    @Test
    public void generateExpandQuery_shouldEscapeReservedCharacters() throws Exception {
        List<String> fields = Arrays.asList(new String[] { SolrConstants.DEFAULT });
        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "[one]", ":two:" })));
        Assert.assertEquals(" +(DEFAULT:(\\[one\\] OR \\:two\\:))", SearchHelper.generateExpandQuery(fields, searchTerms, false));
    }

    /**
     * @see SearchHelper#generateExpandQuery(List,Map,boolean)
     * @verifies add quotation marks if phraseSearch is true
     */
    @Test
    public void generateExpandQuery_shouldAddQuotationMarksIfPhraseSearchIsTrue() throws Exception {
        List<String> fields = Arrays.asList(new String[] { SolrConstants.DEFAULT });
        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "one two three" })));
        Assert.assertEquals(" +(DEFAULT:\"one\\ two\\ three\")", SearchHelper.generateExpandQuery(fields, searchTerms, true));
    }

    /**
     * @see SearchHelper#generateAdvancedExpandQuery(List,int)
     * @verifies generate query correctly
     */
    @Test
    public void generateAdvancedExpandQuery_shouldGenerateQueryCorrectly() throws Exception {
        List<SearchQueryGroup> groups = new ArrayList<>(2);
        {
            SearchQueryGroup group = new SearchQueryGroup(null, 2);
            group.setOperator(SearchQueryGroupOperator.AND);
            group.getQueryItems().get(0).setOperator(SearchItemOperator.AND);
            group.getQueryItems().get(0).setField("MD_FIELD");
            group.getQueryItems().get(0).setValue("val1");
            group.getQueryItems().get(1).setOperator(SearchItemOperator.AND);
            group.getQueryItems().get(1).setField(SolrConstants.TITLE);
            group.getQueryItems().get(1).setValue("foo bar");
            groups.add(group);
        }
        {
            SearchQueryGroup group = new SearchQueryGroup(null, 2);
            group.setOperator(SearchQueryGroupOperator.OR);
            group.getQueryItems().get(0).setField("MD_FIELD");
            group.getQueryItems().get(0).setValue("val2");
            group.getQueryItems().get(1).setOperator(SearchItemOperator.OR);
            group.getQueryItems().get(1).setField("MD_SHELFMARK");
            group.getQueryItems().get(1).setValue("bla blup");
            groups.add(group);
        }

        String result = SearchHelper.generateAdvancedExpandQuery(groups, 0);
        Assert.assertEquals(" +((MD_FIELD:val1 AND MD_TITLE:(foo AND bar)) AND (MD_FIELD:val2 OR MD_SHELFMARK:(bla OR blup)))", result);
    }

    /**
     * @see SearchHelper#generateAdvancedExpandQuery(List,int)
     * @verifies skip reserved fields
     */
    @Test
    public void generateAdvancedExpandQuery_shouldSkipReservedFields() throws Exception {
        List<SearchQueryGroup> groups = new ArrayList<>(1);

        SearchQueryGroup group = new SearchQueryGroup(null, 6);
        group.setOperator(SearchQueryGroupOperator.AND);
        group.getQueryItems().get(0).setOperator(SearchItemOperator.AND);
        group.getQueryItems().get(0).setField(SolrConstants.DOCSTRCT);
        group.getQueryItems().get(0).setValue("Monograph");
        group.getQueryItems().get(1).setOperator(SearchItemOperator.AND);
        group.getQueryItems().get(1).setField(SolrConstants.PI_TOPSTRUCT);
        group.getQueryItems().get(1).setValue("PPN123");
        group.getQueryItems().get(2).setOperator(SearchItemOperator.AND);
        group.getQueryItems().get(2).setField(SolrConstants.DC);
        group.getQueryItems().get(2).setValue("co1");
        group.getQueryItems().get(3).setOperator(SearchItemOperator.AND);
        group.getQueryItems().get(3).setField("MD_FIELD");
        group.getQueryItems().get(3).setValue("val");
        group.getQueryItems().get(4).setOperator(SearchItemOperator.AND);
        group.getQueryItems().get(4).setField(SolrConstants.BOOKMARKS);
        group.getQueryItems().get(4).setValue("bookmarklist");
        group.getQueryItems().get(5).setOperator(SearchItemOperator.AND);
        group.getQueryItems().get(5).setField(SolrConstants.PI_ANCHOR);
        group.getQueryItems().get(5).setValue("PPN000");
        groups.add(group);

        String result = SearchHelper.generateAdvancedExpandQuery(groups, 0);
        Assert.assertEquals(" +((MD_FIELD:val))", result);
    }

    /**
     * @see SearchHelper#exportSearchAsExcel(String,List,Map)
     * @verifies create excel workbook correctly
     */
    @Test
    public void exportSearchAsExcel_shouldCreateExcelWorkbookCorrectly() throws Exception {
        // TODO makes this more robust against changes to the index
        String query = "DOCSTRCT:monograph AND MD_YEARPUBLISH:18*";
        SXSSFWorkbook wb = SearchHelper.exportSearchAsExcel(query, query, Collections.singletonList(new StringPair("SORT_YEARPUBLISH", "asc")), null,
                null, new HashMap<String, Set<String>>(), Locale.ENGLISH, false, null);
        String[] cellValues0 =
                new String[] { "Persistent identifier", "13473260X", "AC08311001", "AC03343066", "PPN193910888" };
        String[] cellValues1 =
                new String[] { "Label", "Gedichte",
                        "Linz und seine Umgebungen", "Das Bücherwesen im Mittelalter",
                        "Das Stilisieren der Thier- und Menschen-Formen" };
        Assert.assertNotNull(wb);
        Assert.assertEquals(1, wb.getNumberOfSheets());
        SXSSFSheet sheet = wb.getSheetAt(0);
        Assert.assertEquals(6, sheet.getPhysicalNumberOfRows());
        {
            SXSSFRow row = sheet.getRow(0);
            Assert.assertEquals(2, row.getPhysicalNumberOfCells());
            Assert.assertEquals("Query:", row.getCell(0).getRichStringCellValue().toString());
            Assert.assertEquals(query, row.getCell(1).getRichStringCellValue().toString());
        }
        for (int i = 1; i < 4; ++i) {
            SXSSFRow row = sheet.getRow(i);
            Assert.assertEquals(2, row.getPhysicalNumberOfCells());
            Assert.assertEquals(cellValues0[i - 1], row.getCell(0).getRichStringCellValue().toString());
            Assert.assertEquals(cellValues1[i - 1], row.getCell(1).getRichStringCellValue().toString());
        }
    }

    /**
     * @see SearchHelper#getBrowseElement(String,int,List,Map,Set,Locale,boolean)
     * @verifies return correct hit for non-aggregated search
     */
    @Test
    public void getBrowseElement_shouldReturnCorrectHitForNonaggregatedSearch() throws Exception {
        String rawQuery = SolrConstants.IDDOC + ":*";
        List<SearchHit> hits = SearchHelper.searchWithFulltext(SearchHelper.buildFinalQuery(rawQuery, false), 0, 10, null, null, null, null, null,
                null, Locale.ENGLISH, null);
        Assert.assertNotNull(hits);
        Assert.assertEquals(10, hits.size());
        for (int i = 0; i < 10; ++i) {
            BrowseElement bi = SearchHelper.getBrowseElement(rawQuery, i, null, null, null, null, Locale.ENGLISH, false, null);
            Assert.assertEquals(hits.get(i).getBrowseElement().getIddoc(), bi.getIddoc());
        }
    }

    /**
     * @see SearchHelper#getBrowseElement(String,int,List,Map,Set,Locale,boolean)
     * @verifies return correct hit for aggregated search
     */
    @Test
    public void getBrowseElement_shouldReturnCorrectHitForAggregatedSearch() throws Exception {
        String rawQuery = SolrConstants.IDDOC + ":*";
        List<SearchHit> hits = SearchHelper.searchWithFulltext(SearchHelper.buildFinalQuery(rawQuery, true), 0, 10, null, null, null, null, null,
                null, Locale.ENGLISH, null);
        Assert.assertNotNull(hits);
        Assert.assertEquals(10, hits.size());
        for (int i = 0; i < 10; ++i) {
            BrowseElement bi = SearchHelper.getBrowseElement(rawQuery, i, null, null, null, null, Locale.ENGLISH, true, null);
            Assert.assertEquals(hits.get(i).getBrowseElement().getIddoc(), bi.getIddoc());
        }
    }

    /**
     * @see SearchHelper#applyHighlightingToPhrase(String,Set)
     * @verifies apply highlighting for all terms
     */
    @Test
    public void applyHighlightingToPhrase_shouldApplyHighlightingForAllTerms() throws Exception {
        {
            String phrase = "FOO BAR Foo Bar foo bar";
            Set<String> terms = new HashSet<>();
            terms.add("foo");
            terms.add("bar");
            String highlightedPhrase = SearchHelper.applyHighlightingToPhrase(phrase, terms);
            Assert.assertEquals(SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "FOO" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END + " "
                    + SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "BAR" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END + " "
                    + SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "Foo" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END + " "
                    + SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "Bar" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END + " "
                    + SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "foo" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END + " "
                    + SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "bar" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END, highlightedPhrase);
        }
        {
            String phrase = "Γ qu 4";
            Set<String> terms = new HashSet<>();
            terms.add("Γ qu 4");
            String highlightedPhrase = SearchHelper.applyHighlightingToPhrase(phrase, terms);
            Assert.assertEquals(SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "Γ qu 4" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END,
                    highlightedPhrase);
        }
    }

    /**
     * @see SearchHelper#applyHighlightingToPhrase(String,Set)
     * @verifies skip single character terms
     */
    @Test
    public void applyHighlightingToPhrase_shouldSkipSingleCharacterTerms() throws Exception {
        String phrase = "FOO BAR Foo Bar foo bar";
        Set<String> terms = new HashSet<>();
        terms.add("o");
        String highlightedPhrase = SearchHelper.applyHighlightingToPhrase(phrase, terms);
        Assert.assertEquals(phrase, highlightedPhrase);
    }

    /**
     * @see SearchHelper#applyHighlightingToPhrase(String,String)
     * @verifies apply highlighting to all occurrences of term
     */
    @Test
    public void applyHighlightingToPhrase_shouldApplyHighlightingToAllOccurrencesOfTerm() throws Exception {
        String phrase = "FOO BAR Foo Bar foo bar";
        String highlightedPhrase1 = SearchHelper.applyHighlightingToPhrase(phrase, "foo");
        Assert.assertEquals(
                SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "FOO" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END + " BAR "
                        + SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "Foo" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END + " Bar "
                        + SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "foo" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END + " bar",
                highlightedPhrase1);
        String highlightedPhrase2 = SearchHelper.applyHighlightingToPhrase(highlightedPhrase1, "bar");
        Assert.assertEquals(SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "FOO" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END + " "
                + SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "BAR" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END + " "
                + SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "Foo" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END + " "
                + SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "Bar" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END + " "
                + SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "foo" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END + " "
                + SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "bar" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END, highlightedPhrase2);
    }

    /**
     * @see SearchHelper#applyHighlightingToPhrase(String,String)
     * @verifies ignore special characters
     */
    @Test
    public void applyHighlightingToPhrase_shouldIgnoreSpecialCharacters() throws Exception {
        String phrase = "FOO BAR Foo Bar foo bar";
        String highlightedPhrase1 = SearchHelper.applyHighlightingToPhrase(phrase, "foo-bar");
        Assert.assertEquals(SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "FOO BAR" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END + " "
                + SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "Foo Bar" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END + " "
                + SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "foo bar" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END, highlightedPhrase1);
    }

    /**
     * @see SearchHelper#applyHighlightingToTerm(String)
     * @verifies add span correctly
     */
    @Test
    public void applyHighlightingToTerm_shouldAddSpanCorrectly() throws Exception {
        Assert.assertEquals(SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "foo" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END,
                SearchHelper.applyHighlightingToTerm("foo"));
    }

    /**
     * @see SearchHelper#replaceHighlightingPlaceholders(String)
     * @verifies replace placeholders with html tags
     */
    @Test
    public void replaceHighlightingPlaceholders_shouldReplacePlaceholdersWithHtmlTags() throws Exception {
        Assert.assertEquals("<span class=\"search-list--highlight\">foo</span>", SearchHelper
                .replaceHighlightingPlaceholders(SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "foo" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END));
    }

    /**
     * @see SearchHelper#prepareQuery(String,String)
     * @verifies prepare non-empty queries correctly
     */
    @Test
    public void prepareQuery_shouldPrepareNonemptyQueriesCorrectly() throws Exception {
        Assert.assertEquals("(FOO:bar)", SearchHelper.prepareQuery("FOO:bar", null));
    }

    /**
     * @see SearchHelper#prepareQuery(String,String)
     * @verifies prepare empty queries correctly
     */
    @Test
    public void prepareQuery_shouldPrepareEmptyQueriesCorrectly() throws Exception {
        Assert.assertEquals("(ISWORK:true OR ISANCHOR:true) AND BLA:blup",
                SearchHelper.prepareQuery(null, "(ISWORK:true OR ISANCHOR:true) AND BLA:blup"));
        Assert.assertEquals("+(ISWORK:true ISANCHOR:true)", SearchHelper.prepareQuery(null, ""));
    }

    /**
     * @see SearchHelper#parseSortString(String,NavigationHelper)
     * @verifies parse string correctly
     */
    @Test
    public void parseSortString_shouldParseStringCorrectly() throws Exception {
        String sortString = "!SORT_1;SORT_2;SORT_3";
        Assert.assertEquals(3, SearchHelper.parseSortString(sortString, null).size());
    }

    /**
     * @see SearchHelper#removeHighlightingTags(String)
     * @verifies remove html tags
     */
    @Test
    public void removeHighlightingTags_shouldRemoveHtmlTags() throws Exception {
        Assert.assertEquals("foo bar", SearchHelper
                .removeHighlightingTags("f<span class=\"search-list--highlight\">oo</span> <span class=\"search-list--highlight\">bar</span>"));
    }

    /**
     * @see SearchHelper#cleanUpSearchTerm(String)
     * @verifies remove illegal chars correctly
     */
    @Test
    public void cleanUpSearchTerm_shouldRemoveIllegalCharsCorrectly() throws Exception {
        Assert.assertEquals("a", SearchHelper.cleanUpSearchTerm("(a)"));
    }

    /**
     * @see SearchHelper#cleanUpSearchTerm(String)
     * @verifies preserve truncation
     */
    @Test
    public void cleanUpSearchTerm_shouldPreserveTruncation() throws Exception {
        Assert.assertEquals("*a*", SearchHelper.cleanUpSearchTerm("*a*"));
    }

    /**
     * @see SearchHelper#cleanUpSearchTerm(String)
     * @verifies preserve negation
     */
    @Test
    public void cleanUpSearchTerm_shouldPreserveNegation() throws Exception {
        Assert.assertEquals("-a", SearchHelper.cleanUpSearchTerm("-a"));
    }

    /**
     * @see SearchHelper#normalizeString(String)
     * @verifies preserve digits
     */
    @Test
    public void normalizeString_shouldPreserveDigits() throws Exception {
        Assert.assertEquals("1 2 3", SearchHelper.normalizeString("1*2*3"));
    }

    /**
     * @see SearchHelper#normalizeString(String)
     * @verifies preserve latin chars
     */
    @Test
    public void normalizeString_shouldPreserveLatinChars() throws Exception {
        Assert.assertEquals("f o obar", SearchHelper.normalizeString("F*O*Obar"));
    }

    /**
     * @see SearchHelper#normalizeString(String)
     * @verifies preserve hebrew chars
     */
    @Test
    public void normalizeString_shouldPreserveHebrewChars() throws Exception {
        Assert.assertEquals("דעה", SearchHelper.normalizeString("דעה"));
    }

    /**
     * Verify that a search for 'DC:dctei' yields 65 results overall, and 4 results within 'FACET_VIEWERSUBTHEME:subtheme1' This also checks that the
     * queries built by {@link SearchHelper#buildFinalQuery(String, boolean, NavigationHelper)} are valid SOLR queries
     * 
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    @Test
    public void testBuildFinalQuery() throws IndexUnreachableException, PresentationException {
        NavigationHelper nh = new NavigationHelper();
        String query = "DC:dctei";

        String finalQuery = SearchHelper.buildFinalQuery(query, false, nh);
        SolrDocumentList docs = DataManager.getInstance().getSearchIndex().search(finalQuery);
        Assert.assertEquals(65, docs.size());

        nh.setSubThemeDiscriminatorValue("subtheme1");
        finalQuery = SearchHelper.buildFinalQuery(query, false, nh);
        docs = DataManager.getInstance().getSearchIndex().search(finalQuery);
        Assert.assertEquals(4, docs.size());
    }

    /**
     * Checks whether counts for each term equal to the value from the last iteration.
     * 
     * @see SearchHelper#getFilteredTerms(BrowsingMenuFieldConfig,String,String,Comparator,boolean)
     * @verifies be thread safe when counting terms
     */
    @Test
    public void getFilteredTerms_shouldBeThreadSafeWhenCountingTerms() throws Exception {
        int previousSize = -1;
        Map<String, Long> previousCounts = new HashMap<>();
        BrowsingMenuFieldConfig bmfc = new BrowsingMenuFieldConfig("MD_LANGUAGE_UNTOKENIZED", null, null, false, null, false);
        for (int i = 0; i < 100; ++i) {
            List<BrowseTerm> terms = SearchHelper.getFilteredTerms(bmfc, null, null, new BrowseTermComparator(Locale.ENGLISH), true);
            Assert.assertFalse(terms.isEmpty());
            Assert.assertTrue(previousSize == -1 || terms.size() == previousSize);
            previousSize = terms.size();
            for (BrowseTerm term : terms) {
                if (previousCounts.containsKey(term.getTerm())) {
                    Assert.assertEquals("Token '" + term.getTerm() + "' - ", Long.valueOf(previousCounts.get(term.getTerm())),
                            Long.valueOf(term.getHitCount()));
                }
                previousCounts.put(term.getTerm(), term.getHitCount());
            }
        }
    }
}
