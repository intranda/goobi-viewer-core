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
package io.goobi.viewer.model.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.model.search.SearchQueryGroup.SearchQueryGroupOperator;
import io.goobi.viewer.model.search.SearchQueryItem.SearchItemOperator;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.security.LicenseType;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.termbrowsing.BrowseTerm;
import io.goobi.viewer.model.termbrowsing.BrowseTermComparator;
import io.goobi.viewer.model.termbrowsing.BrowsingMenuFieldConfig;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;

public class SearchHelperTest extends AbstractDatabaseAndSolrEnabledTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
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
                SearchHelper.findAllCollectionsFromField(SolrConstants.DC, null, null, true, true, ".");
        Assert.assertTrue(collections.size() > 40);
    }

    @Test
    public void findAllCollectionsFromField_shouldGroupCorrectly() throws Exception {
        // First, make sure the collection blacklist always comes from the same config file;
        Map<String, CollectionResult> collections =
                SearchHelper.findAllCollectionsFromField(SolrConstants.DC, SolrConstants.DOCSTRCT, null, true, true, ".");
        //        for (String collection : collections.keySet()) {
        //            System.out.println("collection " + collection + " with facets " + collections.get(collection).getFacetValues().stream().collect(Collectors.joining(", ")));
        //        }
        assertTrue(collections.get("dcmultimedia").getFacetValues().containsAll(Arrays.asList("video", "Audio")));
        assertTrue(collections.get("dcauthoritydata.provenance").getFacetValues().containsAll(Arrays.asList("monograph")));
        assertTrue(collections.get("dcauthoritydata")
                .getFacetValues()
                .containsAll(Arrays.asList("item", "musical_notation", "monograph", "letter", "3dobject", "video")));
        assertTrue(collections.get("dcimage.many").getFacetValues().containsAll(Arrays.asList("volume")));

    }

    /**
     * @see SearchHelper#getPersonalFilterQuerySuffix(User,String)
     * @verifies construct suffix correctly
     */
    @Test
    public void getPersonalFilterQuerySuffix_shouldConstructSuffixCorrectly() throws Exception {
        String suffix =
                SearchHelper.getPersonalFilterQuerySuffix(DataManager.getInstance().getDao().getRecordLicenseTypes(), null, null, Optional.empty(),
                        IPrivilegeHolder.PRIV_LIST);
        Assert.assertEquals(
                " +(ACCESSCONDITION:\"OPENACCESS\""
                        + " ACCESSCONDITION:\"license type 2 name\""
                        + " (+ACCESSCONDITION:\"restriction on access\" +" + SearchHelper.getMovingWallQuery() + "))",
                suffix);
    }

    /**
     * @see SearchHelper#getPersonalFilterQuerySuffix(User,String)
     * @verifies construct suffix correctly if user has license privilege
     */
    @Test
    public void getPersonalFilterQuerySuffix_shouldConstructSuffixCorrectlyIfUserHasLicensePrivilege() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(2);
        String suffix =
                SearchHelper.getPersonalFilterQuerySuffix(DataManager.getInstance().getDao().getRecordLicenseTypes(), user, null, Optional.empty(),
                        IPrivilegeHolder.PRIV_LIST);
        // User has listing privilege for 'license type 1 name'
        Assert.assertTrue(suffix, suffix.contains("ACCESSCONDITION:\"license type 1 name\""));
    }

    /**
     * @see SearchHelper#getPersonalFilterQuerySuffix(User,String)
     * @verifies construct suffix correctly if ip range has license privilege
     */
    @Test
    public void getPersonalFilterQuerySuffix_shouldConstructSuffixCorrectlyIfIpRangeHasLicensePrivilege() throws Exception {
        {
            // Localhost with full access enabled
            String suffix = SearchHelper.getPersonalFilterQuerySuffix(DataManager.getInstance().getDao().getRecordLicenseTypes(), null, "127.0.0.1",
                    Optional.empty(), IPrivilegeHolder.PRIV_LIST);
            Assert.assertEquals("", suffix);
        }
        {
            // Regular IP address (has listing privilege for 'restriction on access')
            String suffix = SearchHelper.getPersonalFilterQuerySuffix(DataManager.getInstance().getDao().getRecordLicenseTypes(), null, "1.2.3.4",
                    Optional.empty(), IPrivilegeHolder.PRIV_LIST);
            Assert.assertEquals(
                    " +(ACCESSCONDITION:\"OPENACCESS\""
                            + " ACCESSCONDITION:\"license type 2 name\""
                            + " (+ACCESSCONDITION:\"restriction on access\" +" + SearchHelper.getMovingWallQuery() + ")"
                            + " ACCESSCONDITION:\"restriction on access\")",
                    suffix);
        }
    }

    /**
     * @see SearchHelper#getPersonalFilterQuerySuffix(User,String)
     * @verifies construct suffix correctly if moving wall license
     */
    @Test
    public void getPersonalFilterQuerySuffix_shouldConstructSuffixCorrectlyIfMovingWallLicense() throws Exception {
        String suffix =
                SearchHelper.getPersonalFilterQuerySuffix(DataManager.getInstance().getDao().getRecordLicenseTypes(), null, null, Optional.empty(),
                        IPrivilegeHolder.PRIV_LIST);
        Assert.assertEquals(
                " +(ACCESSCONDITION:\"OPENACCESS\""
                        + " ACCESSCONDITION:\"license type 2 name\""
                        + " (+ACCESSCONDITION:\"restriction on access\" +" + SearchHelper.getMovingWallQuery() + "))",
                suffix);
    }

    /**
     * @see SearchHelper#getPersonalFilterQuerySuffix(User,String,Optional,String)
     * @verifies add overridden license types from user privilege
     */
    @Test
    public void getPersonalFilterQuerySuffix_shouldAddOverriddenLicenseTypesFromUserPrivilege() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(2);
        String suffix =
                SearchHelper.getPersonalFilterQuerySuffix(DataManager.getInstance().getDao().getRecordLicenseTypes(), user, null, Optional.empty(),
                        IPrivilegeHolder.PRIV_LIST);
        Assert.assertTrue(suffix.contains("license type 1 name"));
        Assert.assertTrue(suffix.contains("license type 4 name"));
    }

    /**
     * @see SearchHelper#getPersonalFilterQuerySuffix(User,String,Optional,String)
     * @verifies add overridden license types from license type privilege
     */
    @Test
    public void getPersonalFilterQuerySuffix_shouldAddOverriddenLicenseTypesFromLicenseTypePrivilege() throws Exception {
        List<LicenseType> licenseTypes = new ArrayList<>(3);
        LicenseType lt = new LicenseType("lt1");
        Assert.assertTrue(lt.getPrivileges().add(IPrivilegeHolder.PRIV_LIST));
        licenseTypes.add(lt);
        for (int i = 2; i <= 3; ++i) {
            LicenseType lt2 = new LicenseType("lt" + i);
            licenseTypes.add(lt2);
            lt.getOverriddenLicenseTypes().add(lt2);
        }

        String suffix = SearchHelper.getPersonalFilterQuerySuffix(licenseTypes, null, null, Optional.empty(), IPrivilegeHolder.PRIV_LIST);
        Assert.assertTrue(suffix.contains("ACCESSCONDITION:\"lt1\""));
        Assert.assertTrue(suffix.contains("ACCESSCONDITION:\"lt2\""));
        Assert.assertTrue(suffix.contains("ACCESSCONDITION:\"lt3\""));
    }

    /**
     * @see SearchHelper#getPersonalFilterQuerySuffix(User,String,Optional,String)
     * @verifies add overridden license types from open access license
     */
    @Test
    public void getPersonalFilterQuerySuffix_shouldAddOverriddenLicenseTypesFromOpenAccessLicense() throws Exception {
        List<LicenseType> licenseTypes = new ArrayList<>(3);
        LicenseType lt = new LicenseType("lt1");
        lt.setOpenAccess(true);
        Assert.assertTrue(lt.isOpenAccess());
        licenseTypes.add(lt);
        for (int i = 2; i <= 3; ++i) {
            LicenseType lt2 = new LicenseType("lt" + i);
            licenseTypes.add(lt2);
            lt.getOverriddenLicenseTypes().add(lt2);
        }

        String suffix = SearchHelper.getPersonalFilterQuerySuffix(licenseTypes, null, null, Optional.empty(), IPrivilegeHolder.PRIV_LIST);
        Assert.assertTrue(suffix.contains("ACCESSCONDITION:\"lt1\""));
        Assert.assertTrue(suffix.contains("ACCESSCONDITION:\"lt2\""));
        Assert.assertTrue(suffix.contains("ACCESSCONDITION:\"lt3\""));
    }

    /**
     * @see SearchHelper#getPersonalFilterQuerySuffix(User,String,String)
     * @verifies construct suffix correctly for alternate privilege
     */
    @Test
    public void getPersonalFilterQuerySuffix_shouldConstructSuffixCorrectlyForAlternatePrivilege() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(2);
        // User has metadata download privilege for 'license type 3 name', but not listing
        Assert.assertFalse(SearchHelper
                .getPersonalFilterQuerySuffix(DataManager.getInstance().getDao().getRecordLicenseTypes(), user, null, Optional.empty(),
                        IPrivilegeHolder.PRIV_LIST)
                .contains("ACCESSCONDITION:\"license type 3 name\""));
        Assert.assertTrue(SearchHelper
                .getPersonalFilterQuerySuffix(DataManager.getInstance().getDao().getRecordLicenseTypes(), user, null, Optional.empty(),
                        IPrivilegeHolder.PRIV_DOWNLOAD_METADATA)
                .contains("ACCESSCONDITION:\"license type 3 name\""));
    }

    /**
     * @see SearchHelper#truncateFulltext(List,String)
     * @verifies make terms bold if found in text
     */
    @Test
    public void truncateFulltext_shouldMakeTermsBoldIfFoundInText() throws Exception {
        String original = StringConstants.LOREM_IPSUM;
        String[] terms = { "ipsum", "tempor", "labore" };
        List<String> truncated = SearchHelper.truncateFulltext(new HashSet<>(Arrays.asList(terms)), original, 200, true, true, 0);
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
        List<String> truncated = SearchHelper.truncateFulltext(null, original, 200, true, true, 0);
        Assert.assertFalse(truncated.isEmpty());
        Assert.assertEquals("text", truncated.get(0));
    }

    /**
     * @see SearchHelper#truncateFulltext(List,String)
     * @verifies truncate string to 200 chars if no terms are given
     */
    @Test
    public void truncateFulltext_shouldTruncateStringTo200CharsIfNoTermsAreGiven() throws Exception {
        String original = StringConstants.LOREM_IPSUM;
        List<String> truncated = SearchHelper.truncateFulltext(null, original, 200, true, true, 0);
        Assert.assertFalse(truncated.isEmpty());
        Assert.assertEquals(original.substring(0, 200), truncated.get(0));
    }

    /**
     * @see SearchHelper#truncateFulltext(List,String)
     * @verifies truncate string to 200 chars if no term has been found
     */
    @Test
    public void truncateFulltext_shouldTruncateStringTo200CharsIfNoTermHasBeenFound() throws Exception {
        String original = StringConstants.LOREM_IPSUM;
        String[] terms = { "boogers" };
        {
            List<String> truncated = SearchHelper.truncateFulltext(new HashSet<>(Arrays.asList(terms)), original, 200, true, true, 0);
            Assert.assertFalse(truncated.isEmpty());
            Assert.assertEquals(original.substring(0, 200), truncated.get(0));
        }
        {
            List<String> truncated = SearchHelper.truncateFulltext(new HashSet<>(Arrays.asList(terms)), original, 200, true, false, 0);
            Assert.assertTrue(truncated.isEmpty());
        }
    }

    /**
     * @see SearchHelper#truncateFulltext(List,String)
     * @verifies remove unclosed HTML tags
     */
    @Test
    public void truncateFulltext_shouldRemoveUnclosedHTMLTags() throws Exception {
        List<String> truncated = SearchHelper.truncateFulltext(null, "Hello <a href", 200, true, true, 0);
        Assert.assertFalse(truncated.isEmpty());
        Assert.assertEquals("Hello", truncated.get(0));
        truncated = SearchHelper.truncateFulltext(null, "Hello <a href ...> and then <b", 200, true, true, 0);
        Assert.assertEquals("Hello and then", truncated.get(0));
    }

    /**
     * @see SearchHelper#truncateFulltext(Set,String,int,boolean)
     * @verifies return multiple match fragments correctly
     */
    @Test
    public void truncateFulltext_shouldReturnMultipleMatchFragmentsCorrectly() throws Exception {
        String original = StringConstants.LOREM_IPSUM;
        String[] terms = { "in" };
        List<String> truncated = SearchHelper.truncateFulltext(new HashSet<>(Arrays.asList(terms)), original, 50, false, true, 0);
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
        List<String> truncated = SearchHelper.truncateFulltext(new HashSet<>(Arrays.asList(terms)), original, 50, false, true, 0);
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
        List<String> truncated = SearchHelper.truncateFulltext(new HashSet<>(Arrays.asList(terms)), original, 50, false, true, 0);
        Assert.assertEquals(1, truncated.size());
        for (String fragment : truncated) {
            Assert.assertTrue(fragment.contains("<span class=\"search-list--highlight\">beats</span>"));
        }
    }

    @Test
    public void truncateFulltext_shouldFindFuzzySearchTermsCorrectly() throws Exception {
        String original = StringConstants.LOREM_IPSUM;
        String[] terms = { "dolor~1" };
        List<String> truncated = SearchHelper.truncateFulltext(new HashSet<>(Arrays.asList(terms)), original, 50, false, true, 0);
        Assert.assertEquals(4, truncated.size());
        Assert.assertEquals(2, truncated.stream().filter(t -> t.contains("<span class=\"search-list--highlight\">dolor</span>")).count());
        Assert.assertEquals(2, truncated.stream().filter(t -> t.contains("<span class=\"search-list--highlight\">dolore</span>")).count());
    }

    /**
     * @see SearchHelper#extractSearchTermsFromQuery(String)
     * @verifies extract all values from query except from NOT blocks
     */
    @Test
    public void extractSearchTermsFromQuery_shouldExtractAllValuesFromQueryExceptFromNOTBlocks() throws Exception {
        {
            // NOT with brackets
            Map<String, Set<String>> result = SearchHelper.extractSearchTermsFromQuery(
                    "(MD_X:value1 OR MD_X:value2 OR (SUPERDEFAULT:value3 AND :value4:)) AND SUPERFULLTEXT:\"hello-world\" AND SUPERUGCTERMS:\"comment\" AND NOT(MD_Y:value_not)",
                    null);
            Assert.assertEquals(5, result.size());
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
        {
            // NOT without brackets
            Map<String, Set<String>> result = SearchHelper.extractSearchTermsFromQuery(
                    "(MD_X:value1 OR MD_X:value2 OR (SUPERDEFAULT:value3 AND :value4:)) AND SUPERFULLTEXT:\"hello-world\" AND SUPERUGCTERMS:\"comment\" AND NOT MD_Y:value_not ",
                    null);
            Assert.assertEquals(5, result.size());
            Assert.assertNull(result.get("MD_Y"));
        }
    }

    /**
     * @see SearchHelper#extractSearchTermsFromQuery(String)
     * @verifies handle multiple phrases in query correctly
     */
    @Test
    public void extractSearchTermsFromQuery_shouldHandleMultiplePhrasesInQueryCorrectly() throws Exception {
        Map<String, Set<String>> result =
                SearchHelper.extractSearchTermsFromQuery("(MD_A:\"value1\" OR MD_B:\"value1\" OR MD_C:\"value2\" OR MD_D:\"value2\")", null);
        Assert.assertEquals(5, result.size());
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
        Assert.assertEquals(3, result.size());
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
     * @see SearchHelper#extractSearchTermsFromQuery(String)
     * @verifies throw IllegalArgumentException if query is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void extractSearchTermsFromQuery_shouldThrowIllegalArgumentExceptionIfQueryIsNull() throws Exception {
        SearchHelper.extractSearchTermsFromQuery(null, null);
    }

    /**
     * @see SearchHelper#extractSearchTermsFromQuery(String,String)
     * @verifies add title terms field
     */
    @Test
    public void extractSearchTermsFromQuery_shouldAddTitleTermsField() throws Exception {
        Map<String, Set<String>> result = SearchHelper.extractSearchTermsFromQuery(
                "(MD_X:value1 OR MD_X:value2 OR (SUPERDEFAULT:value3 AND :value4:)) AND SUPERFULLTEXT:\"hello-world\" AND SUPERUGCTERMS:\"comment\" AND NOT(MD_Y:value_not)",
                null);
        Set<String> terms = result.get(SearchHelper.TITLE_TERMS);
        Assert.assertNotNull(terms);
        Assert.assertEquals(6, terms.size());
        Assert.assertTrue(terms.contains("(value1)"));
        Assert.assertTrue(terms.contains("(value2)"));
        Assert.assertTrue(terms.contains("(value3)"));
        Assert.assertTrue(terms.contains("(:value4:)"));
        Assert.assertTrue(terms.contains("\"hello-world\""));
        Assert.assertTrue(terms.contains("\"comment\""));
    }

    /**
     * @see SearchHelper#extractSearchTermsFromQuery(String,String)
     * @verifies not remove truncation
     */
    @Test
    public void extractSearchTermsFromQuery_shouldNotRemoveTruncation() throws Exception {
        Map<String, Set<String>> result = SearchHelper.extractSearchTermsFromQuery("MD_A:*foo*", null);
        Assert.assertEquals(2, result.size());
        {
            Set<String> terms = result.get("MD_A");
            Assert.assertNotNull(terms);
            Assert.assertEquals(1, terms.size());
            Assert.assertTrue(terms.contains("*foo*"));
        }
    }

    /**
     * @see SearchHelper#extractSearchTermsFromQuery(String,String)
     * @verifies remove proximity search tokens
     */
    @Test
    public void extractSearchTermsFromQuery_shouldRemoveProximitySearchTokens() throws Exception {
        Map<String, Set<String>> result = SearchHelper.extractSearchTermsFromQuery(
                "(MD_X:value1 OR MD_X:value2 OR (SUPERDEFAULT:value3 AND :value4:)) AND SUPERFULLTEXT:\"hello world\"~10 AND SUPERUGCTERMS:\"comment\" AND NOT(MD_Y:value_not)",
                null);
        Set<String> terms = result.get(SolrConstants.FULLTEXT);
        Assert.assertNotNull(terms);
        Assert.assertEquals(1, terms.size());
        Assert.assertTrue(terms.contains("hello world"));
    }

    /**
     * @see SearchHelper#extractSearchTermsFromQuery(String,String)
     * @verifies remove range values
     */
    @Test
    public void extractSearchTermsFromQuery_shouldRemoveRangeValues() throws Exception {
        Map<String, Set<String>> result =
                SearchHelper.extractSearchTermsFromQuery("+(ISWORK:true ISANCHOR:true DOCTYPE:UGC) +MD_YEARPUBLISH:[2020 TO 2022]", null);
        {
            Set<String> terms = result.get("MD_YEARPUBLISH");
            Assert.assertNull(terms);
        }
    }

    /**
     * @see SearchHelper#extractSearchTermsFromQuery(String,String)
     * @verifies remove operators from field names
     */
    @Test
    public void extractSearchTermsFromQuery_shouldRemoveOperatorsFromFieldNames() throws Exception {
        Map<String, Set<String>> result =
                SearchHelper.extractSearchTermsFromQuery(
                        " (+(SUPERDEFAULT:(berlin) SUPERFULLTEXT:(berlin) SUPERUGCTERMS:(berlin)) +(MD_AUTHOR:(karl)))",
                        null);
        Assert.assertTrue(result.containsKey("MD_AUTHOR"));

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
        Assert.assertEquals(SolrConstants.CALENDAR_YEAR, SearchHelper.defacetifyField("FACET_YEAR"));
        Assert.assertEquals(SolrConstants.CALENDAR_MONTH, SearchHelper.defacetifyField("FACET_YEARMONTH"));
        Assert.assertEquals(SolrConstants.CALENDAR_DAY, SearchHelper.defacetifyField("FACET_YEARMONTHDAY"));
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
        Assert.assertEquals("FACET_MD2_FOO", SearchHelper.facetifyField("MD2_FOO_UNTOKENIZED"));
        Assert.assertEquals("MDNUM_NUMBER", SearchHelper.facetifyField("MDNUM_NUMBER"));
    }

    /**
     * @see SearchHelper#facetifyField(String)
     * @verifies leave bool fields unaltered
     */
    @Test
    public void facetifyField_shouldLeaveBoolFieldsUnaltered() throws Exception {
        Assert.assertEquals("BOOL_FOO", SearchHelper.facetifyField("BOOL_FOO"));
    }

    /**
     * @see SearchHelper#facetifyField(String)
     * @verifies leave year month day fields unaltered
     */
    @Test
    public void facetifyField_shouldLeaveYearMonthDayFieldsUnaltered() throws Exception {
        Assert.assertEquals(SolrConstants.CALENDAR_YEAR, SearchHelper.facetifyField(SolrConstants.CALENDAR_YEAR));
        Assert.assertEquals(SolrConstants.CALENDAR_MONTH, SearchHelper.facetifyField(SolrConstants.CALENDAR_MONTH));
        Assert.assertEquals(SolrConstants.CALENDAR_DAY, SearchHelper.facetifyField(SolrConstants.CALENDAR_DAY));
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
        Assert.assertEquals("SORTNUM_YEAR", SearchHelper.sortifyField(SolrConstants.YEAR));
        Assert.assertEquals("SORTNUM_FOO", SearchHelper.sortifyField("MDNUM_FOO"));
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
        Assert.assertEquals("SORT_FOO", SearchHelper.adaptField("MD2_FOO", "SORT_"));
        Assert.assertEquals("SORTNUM_FOO", SearchHelper.adaptField("MDNUM_FOO", "SORT_"));
        Assert.assertEquals("SORT_FOO", SearchHelper.adaptField("NE_FOO", "SORT_"));
        Assert.assertEquals("SORT_FOO", SearchHelper.adaptField("BOOL_FOO", "SORT_"));
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
     * @see SearchHelper#adaptField(String,String)
     * @verifies not apply facet prefix to calendar fields
     */
    @Test
    public void adaptField_shouldNotApplyFacetPrefixToCalendarFields() throws Exception {
        Assert.assertEquals(SolrConstants.CALENDAR_YEAR, SearchHelper.adaptField(SolrConstants.CALENDAR_YEAR, "FACET_"));
        Assert.assertEquals(SolrConstants.CALENDAR_MONTH, SearchHelper.adaptField(SolrConstants.CALENDAR_MONTH, "FACET_"));
        Assert.assertEquals(SolrConstants.CALENDAR_DAY, SearchHelper.adaptField(SolrConstants.CALENDAR_DAY, "FACET_"));
    }

    /**
     * @see SearchHelper#getAllSuffixes(HttpSession,boolean,boolean)
     * @verifies add static suffix
     */
    @Test
    public void getAllSuffixes_shouldAddStaticSuffix() throws Exception {
        String suffix = SearchHelper.getAllSuffixes(null, true, false);
        Assert.assertNotNull(suffix);
        Assert.assertTrue(suffix.contains(DataManager.getInstance().getConfiguration().getStaticQuerySuffix()));
    }

    /**
     * @see SearchHelper#getAllSuffixes(HttpServletRequest,boolean,boolean,boolean)
     * @verifies not add static suffix if not requested
     */
    @Test
    public void getAllSuffixes_shouldNotAddStaticSuffixIfNotRequested() throws Exception {
        String suffix = SearchHelper.getAllSuffixes(null, false, false);
        Assert.assertNotNull(suffix);
        Assert.assertFalse(suffix.contains(DataManager.getInstance().getConfiguration().getStaticQuerySuffix()));
    }

    /**
     * @see SearchHelper#getAllSuffixes(HttpSession,boolean,boolean)
     * @verifies add collection blacklist suffix
     */
    @Test
    public void getAllSuffixes_shouldAddCollectionBlacklistSuffix() throws Exception {

        String suffix = SearchHelper.getAllSuffixes();
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
                SearchHelper.generateExpandQuery(fields, searchTerms, false, 0));
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

        Assert.assertEquals("", SearchHelper.generateExpandQuery(fields, searchTerms, false, 0));
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
                SearchHelper.generateExpandQuery(fields, searchTerms, false, 0));
    }

    /**
     * @see SearchHelper#generateExpandQuery(List,Map)
     * @verifies not escape asterisks
     */
    @Test
    public void generateExpandQuery_shouldNotEscapeAsterisks() throws Exception {
        List<String> fields = Arrays.asList(new String[] { SolrConstants.CALENDAR_DAY });
        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.CALENDAR_DAY, new HashSet<>(Arrays.asList(new String[] { "*", })));
        Assert.assertEquals(" +(YEARMONTHDAY:*)", SearchHelper.generateExpandQuery(fields, searchTerms, false, 0));
    }

    /**
     * @see SearchHelper#generateExpandQuery(List,Map,boolean)
     * @verifies not escape truncation
     */
    @Test
    public void generateExpandQuery_shouldNotEscapeTruncation() throws Exception {
        List<String> fields = Arrays.asList(new String[] { SolrConstants.DEFAULT });
        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "foo*", })));
        Assert.assertEquals(" +(DEFAULT:foo*)", SearchHelper.generateExpandQuery(fields, searchTerms, false, 0));
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
        Assert.assertEquals(" +(DEFAULT:(\\[one\\] OR \\:two\\:))", SearchHelper.generateExpandQuery(fields, searchTerms, false, 0));
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
        Assert.assertEquals(" +(DEFAULT:\"one\\ two\\ three\")", SearchHelper.generateExpandQuery(fields, searchTerms, true, 0));
    }

    /**
     * @see SearchHelper#generateExpandQuery(List,Map,boolean,int)
     * @verifies add proximity search token correctly
     */
    @Test
    public void generateExpandQuery_shouldAddProximitySearchTokenCorrectly() throws Exception {
        List<String> fields =
                Arrays.asList(new String[] { SolrConstants.DEFAULT, SolrConstants.FULLTEXT, SolrConstants.NORMDATATERMS, SolrConstants.UGCTERMS,
                        SolrConstants.CMS_TEXT_ALL, SolrConstants.PI_TOPSTRUCT, SolrConstants.PI_ANCHOR, SolrConstants.DC, SolrConstants.DOCSTRCT });
        Map<String, Set<String>> searchTerms = new HashMap<>();
        searchTerms.put(SolrConstants.DEFAULT, new HashSet<>(Arrays.asList(new String[] { "one", "two" })));
        searchTerms.put(SolrConstants.FULLTEXT, new HashSet<>(Arrays.asList(new String[] { "\"two three\"" })));
        searchTerms.put(SolrConstants.NORMDATATERMS, new HashSet<>(Arrays.asList(new String[] { "four", "five" })));
        searchTerms.put(SolrConstants.UGCTERMS, new HashSet<>(Arrays.asList(new String[] { "six" })));
        searchTerms.put(SolrConstants.CMS_TEXT_ALL, new HashSet<>(Arrays.asList(new String[] { "seven" })));
        searchTerms.put(SolrConstants.PI_ANCHOR, new HashSet<>(Arrays.asList(new String[] { "eight" })));
        searchTerms.put(SolrConstants.PI_TOPSTRUCT, new HashSet<>(Arrays.asList(new String[] { "nine" })));
        Assert.assertEquals(
                " +(" + SolrConstants.DEFAULT + ":(one OR two) OR " + SolrConstants.FULLTEXT + ":\"two\\ three\"~10 OR " + SolrConstants.NORMDATATERMS
                        + ":(four OR five) OR " + SolrConstants.UGCTERMS + ":six OR " + SolrConstants.CMS_TEXT_ALL + ":seven)",
                SearchHelper.generateExpandQuery(fields, searchTerms, false, 10));

        searchTerms.clear();
        searchTerms.put(SolrConstants.FULLTEXT, new HashSet<>(Arrays.asList(new String[] { "\"two three\"" })));
        Assert.assertEquals(
                " +(" + SolrConstants.FULLTEXT + ":\"two\\ three\"~10)",
                SearchHelper.generateExpandQuery(fields, searchTerms, false, 10));
    }

    /**
     * @see SearchHelper#generateAdvancedExpandQuery(List,int)
     * @verifies generate query correctly
     */
    @Test
    public void generateAdvancedExpandQuery_shouldGenerateQueryCorrectly() throws Exception {
        SearchQueryGroup group = new SearchQueryGroup(DataManager.getInstance().getConfiguration().getAdvancedSearchFields(null, true, "en"), null);
        group.setOperator(SearchQueryGroupOperator.AND);
        group.getQueryItems().get(0).setOperator(SearchItemOperator.AND);
        group.getQueryItems().get(0).setField("MD_FIELD");
        group.getQueryItems().get(0).setValue("val1");
        group.getQueryItems().get(1).setOperator(SearchItemOperator.AND);
        group.getQueryItems().get(1).setField(SolrConstants.TITLE);
        group.getQueryItems().get(1).setValue("foo bar");

        String result = SearchHelper.generateAdvancedExpandQuery(group, false);
        Assert.assertEquals(" +(+(MD_FIELD:(val1)) +(MD_TITLE:(foo AND bar)))", result);
    }

    @Test
    public void generateAdvancedExpandQuery_shouldGenerateQueryCorrectly_fuzzySearch() throws Exception {
        SearchQueryGroup group = new SearchQueryGroup(DataManager.getInstance().getConfiguration().getAdvancedSearchFields(null, true, "en"), null);
        group.getQueryItems().get(0).setField("MD_FIELD");
        group.getQueryItems().get(0).setValue("val2");
        group.getQueryItems().get(1).setOperator(SearchItemOperator.OR);
        group.getQueryItems().get(1).setField("MD_SHELFMARK");
        group.getQueryItems().get(1).setValue("bla blup");

        String result = SearchHelper.generateAdvancedExpandQuery(group, true);
        Assert.assertEquals(" +(+(MD_FIELD:((val2 val2~1))) (MD_SHELFMARK:((bla) AND (blup blup~1))))", result);
    }

    /**
     * @see SearchHelper#generateAdvancedExpandQuery(List,int)
     * @verifies skip reserved fields
     */
    @Test
    public void generateAdvancedExpandQuery_shouldSkipReservedFields() throws Exception {
        SearchQueryGroup group = new SearchQueryGroup(DataManager.getInstance().getConfiguration().getAdvancedSearchFields(null, true, "en"), null);
        group.getQueryItems().add(new SearchQueryItem());
        group.getQueryItems().add(new SearchQueryItem());
        group.getQueryItems().add(new SearchQueryItem());
        Assert.assertEquals(6, group.getQueryItems().size());

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

        String result = SearchHelper.generateAdvancedExpandQuery(group, false);
        Assert.assertEquals(" +(+(MD_FIELD:(val)))", result);
    }

    /**
     * @see SearchHelper#generateAdvancedExpandQuery(SearchQueryGroup,boolean)
     * @verifies switch to OR operator on fulltext items
     */
    @Test
    public void generateAdvancedExpandQuery_shouldSwitchToOROperatorOnFulltextItems() throws Exception {
        SearchQueryGroup group = new SearchQueryGroup(DataManager.getInstance().getConfiguration().getAdvancedSearchFields(null, true, "en"), null);
        group.setOperator(SearchQueryGroupOperator.AND);
        group.getQueryItems().get(0).setOperator(SearchItemOperator.AND);
        group.getQueryItems().get(0).setField("MD_FIELD");
        group.getQueryItems().get(0).setValue("val1");
        group.getQueryItems().get(1).setOperator(SearchItemOperator.AND);
        group.getQueryItems().get(1).setField(SolrConstants.FULLTEXT);
        group.getQueryItems().get(1).setValue("foo bar");

        String result = SearchHelper.generateAdvancedExpandQuery(group, false);
        Assert.assertEquals(" +((MD_FIELD:(val1)) (FULLTEXT:(foo AND bar)))", result);
    }

    /**
     * @see SearchHelper#exportSearchAsExcel(String,List,Map)
     * @verifies create excel workbook correctly
     */
    @Test
    public void exportSearchAsExcel_shouldCreateExcelWorkbookCorrectly() throws Exception {
        // TODO makes this more robust against changes to the index
        String query = "DOCSTRCT:monograph AND MD_YEARPUBLISH:18*";
        try (SXSSFWorkbook wb = new SXSSFWorkbook(25)) {
            SearchHelper.exportSearchAsExcel(wb, query, query, Collections.singletonList(new StringPair("SORT_YEARPUBLISH", "asc")), null,
                    null, new HashMap<String, Set<String>>(), Locale.ENGLISH, 0);
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
    }

    //    /**
    //     * @see SearchHelper#getBrowseElement(String,int,List,Map,Set,Locale,boolean)
    //     * @verifies return correct hit for non-aggregated search
    //     */
    //    @Test
    //    public void getBrowseElement_shouldReturnCorrectHitForNonaggregatedSearch() throws Exception {
    //        String rawQuery = SolrConstants.IDDOC + ":*";
    //        List<SearchHit> hits = SearchHelper.searchWithFulltext(SearchHelper.buildFinalQuery(rawQuery, false), 0, 10, null, null, null, null, null,
    //                null, Locale.ENGLISH, null);
    //        Assert.assertNotNull(hits);
    //        Assert.assertEquals(10, hits.size());
    //        for (int i = 0; i < 10; ++i) {
    //            BrowseElement bi = SearchHelper.getBrowseElement(rawQuery, i, null, null, null, null, Locale.ENGLISH, false, null);
    //            Assert.assertEquals(hits.get(i).getBrowseElement().getIddoc(), bi.getIddoc());
    //        }
    //    }

    /**
     * @see SearchHelper#getBrowseElement(String,int,List,Map,Set,Locale,boolean)
     * @verifies return correct hit for aggregated search
     */
    @Test
    public void getBrowseElement_shouldReturnCorrectHitForAggregatedSearch() throws Exception {
        String rawQuery = SolrConstants.IDDOC + ":*";
        List<SearchHit> hits =
                SearchHelper.searchWithAggregation(SearchHelper.buildFinalQuery(rawQuery, false, SearchAggregationType.AGGREGATE_TO_TOPSTRUCT),
                        0, 10, null, null, null, null, null, null, null, Locale.ENGLISH, false, 0);
        Assert.assertNotNull(hits);
        Assert.assertEquals(10, hits.size());
        for (int i = 0; i < 10; ++i) {
            BrowseElement bi = SearchHelper.getBrowseElement(rawQuery, i, null, null, null, null, Locale.ENGLISH, 0);
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

    @Test
    public void applyHighlightingToPhrase_shouldIgnoreDiacriticsForHightlighting() throws Exception {
        String phrase = "Širvintos";
        Set<String> terms = new HashSet<>();
        terms.add("sirvintos");
        String highlightedPhrase = SearchHelper.applyHighlightingToPhrase(phrase, terms);
        //        System.out.println(highlightedPhrase);
        Assert.assertEquals(SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + phrase + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END, highlightedPhrase);
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
     * @see SearchHelper#applyHighlightingToPhrase(String,String)
     * @verifies not add highlighting to hyperlink urls
     */
    @Test
    public void applyHighlightingToPhrase_shouldNotAddHighlightingToHyperlinkUrls() throws Exception {
        String phrase = "foo <a href=\"https://example.com/foo\">foo</a> foo";
        String highlightedPhrase1 = SearchHelper.applyHighlightingToPhrase(phrase, "foo");
        Assert.assertEquals(SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "foo" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END
                + " <a href=\"https://example.com/foo\">foo</a> "
                + SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "foo" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END, highlightedPhrase1);
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
     * @see SearchHelper#removeHighlightingPlaceholders(String)
     * @verifies replace placeholders with empty strings
     */
    @Test
    public void removeHighlightingPlaceholders_shouldReplacePlaceholdersWithEmptyStrings() throws Exception {
        Assert.assertEquals("foo", SearchHelper
                .removeHighlightingPlaceholders(SearchHelper.PLACEHOLDER_HIGHLIGHTING_START + "foo" + SearchHelper.PLACEHOLDER_HIGHLIGHTING_END));
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
     * @see SearchHelper#cleanUpSearchTerm(String)
     * @verifies remove illegal chars correctly
     */
    @Test
    public void cleanUpSearchTerm_shouldRemoveIllegalCharsCorrectly() throws Exception {
        Assert.assertEquals("a", SearchHelper.cleanUpSearchTerm("(a)"));
    }

    /**
     * @see SearchHelper#cleanUpSearchTerm(String)
     * @verifies remove trailing punctuation
     */
    @Test
    public void cleanUpSearchTerm_shouldRemoveTrailingPunctuation() throws Exception {
        Assert.assertEquals("a", SearchHelper.cleanUpSearchTerm("a,:;"));
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
     * @see SearchHelper#normalizeString(String)
     * @verifies remove hyperlink html elements including terms
     */
    @Test
    public void normalizeString_shouldRemoveHyperlinkHtmlElementsIncludingTerms() throws Exception {
        Assert.assertEquals("one                                                                                    two",
                SearchHelper.normalizeString("one <a href=\"https://example.com/foo\">foo</a><a href=\"https://example.com/bar\">bar</a> two"));
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
        String query = "DC:dctei";

        String finalQuery = SearchHelper.buildFinalQuery(query, false, SearchAggregationType.NO_AGGREGATION);
        SolrDocumentList docs = DataManager.getInstance().getSearchIndex().search(finalQuery);
        Assert.assertEquals(65, docs.size());

        finalQuery = SearchHelper.buildFinalQuery(query, false, SearchAggregationType.NO_AGGREGATION);
        docs = DataManager.getInstance().getSearchIndex().search(finalQuery);
        Assert.assertEquals(65, docs.size());
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
        BrowsingMenuFieldConfig bmfc = new BrowsingMenuFieldConfig("MD_CREATOR_UNTOKENIZED", null, null, false, false, false);
        for (int i = 0; i < 10; ++i) {
            List<BrowseTerm> terms =
                    SearchHelper.getFilteredTerms(bmfc, null, null, 0, SolrSearchIndex.MAX_HITS, new BrowseTermComparator(Locale.ENGLISH));
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

    /**
     * @see SearchHelper#getFilteredTermsFromIndex(BrowsingMenuFieldConfig,String,String,List,int,int)
     * @verifies contain facets for the main field
     */
    @Test
    public void getFilteredTermsFromIndex_shouldContainFacetsForTheMainField() throws Exception {
        BrowsingMenuFieldConfig bmfc = new BrowsingMenuFieldConfig("MD_CREATOR_UNTOKENIZED", null, null, false, false, false);
        QueryResponse resp = SearchHelper.getFilteredTermsFromIndex(bmfc, "", null, null, 0, SolrSearchIndex.MAX_HITS);
        Assert.assertNotNull(resp);
        Assert.assertNotNull(resp.getFacetField(SearchHelper.facetifyField(bmfc.getField())));
    }

    /**
     * @see SearchHelper#getQueryForAccessCondition(String,boolean)
     * @verifies build escaped query correctly
     */
    @Test
    public void getQueryForAccessCondition_shouldBuildEscapedQueryCorrectly() throws Exception {
        Assert.assertEquals(
                "+(ISWORK:true ISANCHOR:true DOCTYPE:UGC) +" + SolrConstants.ACCESSCONDITION + ":\"foo" + StringTools.SLASH_REPLACEMENT + "bar\"",
                SearchHelper.getQueryForAccessCondition("foo/bar", true));
    }

    /**
     * @see SearchHelper#getQueryForAccessCondition(String,boolean)
     * @verifies build not escaped query correctly
     */
    @Test
    public void getQueryForAccessCondition_shouldBuildNotEscapedQueryCorrectly() throws Exception {
        Assert.assertEquals("+(ISWORK:true ISANCHOR:true DOCTYPE:UGC) +" + SolrConstants.ACCESSCONDITION + ":\"foo/bar\"",
                SearchHelper.getQueryForAccessCondition("foo/bar", false));
    }

    /**
     * @see SearchHelper#buildFinalQuery(String,boolean,HttpServletRequest)
     * @verifies add join statement if aggregateHits true
     */
    @Test
    public void buildFinalQuery_shouldAddJoinStatementIfAggregateHitsTrue() throws Exception {
        String finalQuery = SearchHelper.buildFinalQuery("DEFAULT:*", false, null, SearchAggregationType.AGGREGATE_TO_TOPSTRUCT);
        Assert.assertEquals(SearchHelper.AGGREGATION_QUERY_PREFIX + "+(DEFAULT:*) -BOOL_HIDE:true -DC:collection1 -DC:collection2", finalQuery);
    }

    /**
     * @see SearchHelper#buildFinalQuery(String,boolean,HttpServletRequest)
     * @verifies not add join statement if aggregateHits false
     */
    @Test
    public void buildFinalQuery_shouldNotAddJoinStatementIfAggregateHitsFalse() throws Exception {
        String finalQuery = SearchHelper.buildFinalQuery("DEFAULT:*", false, null, SearchAggregationType.NO_AGGREGATION);
        Assert.assertEquals("+(DEFAULT:*) -BOOL_HIDE:true -DC:collection1 -DC:collection2", finalQuery);
    }

    /**
     * @see SearchHelper#buildFinalQuery(String,boolean,HttpServletRequest)
     * @verifies remove existing join statement
     */
    @Test
    public void buildFinalQuery_shouldRemoveExistingJoinStatement() throws Exception {
        String finalQuery = SearchHelper.buildFinalQuery(SearchHelper.AGGREGATION_QUERY_PREFIX + "DEFAULT:*", false, null,
                SearchAggregationType.AGGREGATE_TO_TOPSTRUCT);
        Assert.assertEquals(SearchHelper.AGGREGATION_QUERY_PREFIX + "+(DEFAULT:*) -BOOL_HIDE:true -DC:collection1 -DC:collection2", finalQuery);
    }

    /**
     * @see SearchHelper#buildFinalQuery(String,String,boolean,boolean,HttpServletRequest)
     * @verifies add embedded query template if boostTopLevelDocstructs true
     */
    @Test
    public void buildFinalQuery_shouldAddEmbeddedQueryTemplateIfBoostTopLevelDocstructsTrue() throws Exception {
        String finalQuery =
                SearchHelper.buildFinalQuery(SearchHelper.AGGREGATION_QUERY_PREFIX + "DEFAULT:(foo bar)", true, null,
                        SearchAggregationType.AGGREGATE_TO_TOPSTRUCT);
        Assert.assertEquals("+("
                + SearchHelper.EMBEDDED_QUERY_TEMPLATE.replace("{0}", SearchHelper.AGGREGATION_QUERY_PREFIX + "+(DEFAULT:(foo bar))")
                + ") -BOOL_HIDE:true -DC:collection1 -DC:collection2",
                finalQuery);
    }

    /**
     * @see SearchHelper#buildFinalQuery(String,String,boolean,boolean,HttpServletRequest)
     * @verifies escape quotation marks in embedded query
     */
    @Test
    public void buildFinalQuery_shouldEscapeQuotationMarksInEmbeddedQuery() throws Exception {
        String finalQuery =
                SearchHelper.buildFinalQuery(SearchHelper.AGGREGATION_QUERY_PREFIX + "DEFAULT:(\"foo bar\")", true, null,
                        SearchAggregationType.AGGREGATE_TO_TOPSTRUCT);
        Assert.assertEquals("+("
                + SearchHelper.EMBEDDED_QUERY_TEMPLATE.replace("{0}", SearchHelper.AGGREGATION_QUERY_PREFIX + "+(DEFAULT:(\\\"foo bar\\\"))")
                + ") -BOOL_HIDE:true -DC:collection1 -DC:collection2",
                finalQuery);
    }

    @Test
    public void testGetWildcards() {
        String prefix = "*term";
        String suffix = "term*";
        String both = "*term*";
        String neither = "term";
        {
            String[] wildcards = SearchHelper.getWildcardsTokens(prefix);
            assertEquals("*", wildcards[0]);
            assertEquals("term", wildcards[1]);
            assertEquals("", wildcards[2]);
        }
        {
            String[] wildcards = SearchHelper.getWildcardsTokens(suffix);
            assertEquals("", wildcards[0]);
            assertEquals("term", wildcards[1]);
            assertEquals("*", wildcards[2]);
        }
        {
            String[] wildcards = SearchHelper.getWildcardsTokens(both);
            assertEquals("*", wildcards[0]);
            assertEquals("term", wildcards[1]);
            assertEquals("*", wildcards[2]);
        }
        {
            String[] wildcards = SearchHelper.getWildcardsTokens(neither);
            assertEquals("", wildcards[0]);
            assertEquals("term", wildcards[1]);
            assertEquals("", wildcards[2]);
        }
    }

    /**
     * @see SearchHelper#addProximitySearchToken(String,int)
     * @verifies add token correctly
     */
    @Test
    public void addProximitySearchToken_shouldAddTokenCorrectly() throws Exception {
        Assert.assertEquals("\"foo bar\"~10", SearchHelper.addProximitySearchToken("foo bar", 10));
        Assert.assertEquals("\"foo bar\"~10", SearchHelper.addProximitySearchToken("\"foo bar\"", 10));
    }

    /**
     * @see SearchHelper#removeProximitySearchToken(String)
     * @verifies remove token correctly
     */
    @Test
    public void removeProximitySearchToken_shouldRemoveTokenCorrectly() throws Exception {
        Assert.assertEquals("\"foo bar\"", SearchHelper.removeProximitySearchToken("\"foo bar\"~10"));
    }

    /**
     * @see SearchHelper#removeProximitySearchToken(String)
     * @verifies return unmodified term if no token found
     */
    @Test
    public void removeProximitySearchToken_shouldReturnUnmodifiedTermIfNoTokenFound() throws Exception {
        Assert.assertEquals("\"foo bar\"", SearchHelper.removeProximitySearchToken("\"foo bar\""));
        Assert.assertEquals("", SearchHelper.removeProximitySearchToken(""));
    }

    /**
     * @see SearchHelper#buildProximitySearchRegexPattern(String,int)
     * @verifies build regex correctly
     */
    @Test
    public void buildProximitySearchRegexPattern_shouldBuildRegexCorrectly() throws Exception {
        Assert.assertEquals("\\b"
                + "(?:o(| )n(| )e\\W+(?:\\w+\\W+){0,10}?t(| )w(| )o\\W+(?:\\w+\\W+){0,10}?t(| )h(| )r(| )e(| )e"
                + "|"
                + "t(| )h(| )r(| )e(| )e\\W+(?:\\w+\\W+){0,10}?t(| )w(| )o\\W+(?:\\w+\\W+){0,10}?o(| )n(| )e)"
                + "\\b",
                SearchHelper.buildProximitySearchRegexPattern("one two three", 10));
    }

    /**
     * @see SearchHelper#extractProximitySearchDistanceFromQuery(String)
     * @verifies return 0 if query empty
     */
    @Test
    public void extractProximitySearchDistanceFromQuery_shouldReturn0IfQueryEmpty() throws Exception {
        Assert.assertEquals(0, SearchHelper.extractProximitySearchDistanceFromQuery(null));
        Assert.assertEquals(0, SearchHelper.extractProximitySearchDistanceFromQuery(""));
    }

    /**
     * @see SearchHelper#extractProximitySearchDistanceFromQuery(String)
     * @verifies return 0 if query does not contain token
     */
    @Test
    public void extractProximitySearchDistanceFromQuery_shouldReturn0IfQueryDoesNotContainToken() throws Exception {
        Assert.assertEquals(0, SearchHelper.extractProximitySearchDistanceFromQuery("\"foo bar\""));
    }

    /**
     * @see SearchHelper#extractProximitySearchDistanceFromQuery(String)
     * @verifies return 0 if query not phrase search
     */
    @Test
    public void extractProximitySearchDistanceFromQuery_shouldReturn0IfQueryNotPhraseSearch() throws Exception {
        Assert.assertEquals(0, SearchHelper.extractProximitySearchDistanceFromQuery("foo~10"));
    }

    /**
     * @see SearchHelper#extractProximitySearchDistanceFromQuery(String)
     * @verifies extract distance correctly
     */
    @Test
    public void extractProximitySearchDistanceFromQuery_shouldExtractDistanceCorrectly() throws Exception {
        Assert.assertEquals(10, SearchHelper.extractProximitySearchDistanceFromQuery("\"foobar\"~10"));
    }

    /**
     * @see SearchHelper#isPhrase(String)
     * @verifies detect phrase correctly
     */
    @Test
    public void isPhrase_shouldDetectPhraseCorrectly() throws Exception {
        Assert.assertFalse(SearchHelper.isPhrase("foo bar"));
        Assert.assertTrue(SearchHelper.isPhrase("\"foo bar\""));
    }

    /**
     * @see SearchHelper#isPhrase(String)
     * @verifies detect phrase with proximity correctly
     */
    @Test
    public void isPhrase_shouldDetectPhraseWithProximityCorrectly() throws Exception {
        Assert.assertFalse(SearchHelper.isPhrase("foo bar~10"));
        Assert.assertTrue(SearchHelper.isPhrase("\"foo bar\"~10"));
    }

    /**
     * @see SearchHelper#getFacetValues(String,String,String,int,Map)
     * @verifies return correct values via json response
     */
    @Test
    public void getFacetValues_shouldReturnCorrectValuesViaJsonResponse() throws Exception {
        Map<String, String> params = Collections.singletonMap("json.facet", "{uniqueCount : \"unique(" + SolrConstants.PI + ")\"}");
        List<String> values = SearchHelper.getFacetValues(SolrConstants.PI + ":[* TO *]", "json:uniqueCount", null, 1, params);
        Assert.assertNotNull(values);
        Assert.assertEquals(1, values.size());
        int size = !values.isEmpty() ? Integer.valueOf(values.get(0)) : 0;
        Assert.assertTrue(size > 0);
    }

    /**
     * @see SearchHelper#buildExpandQueryFromFacets(List)
     * @verifies return empty string if list null or empty
     */
    @Test
    public void buildExpandQueryFromFacets_shouldReturnEmptyStringIfListNullOrEmpty() throws Exception {
        Assert.assertEquals("", SearchHelper.buildExpandQueryFromFacets(null, null));
        Assert.assertEquals("", SearchHelper.buildExpandQueryFromFacets(Collections.emptyList(), null));
    }

    /**
     * @see SearchHelper#buildExpandQueryFromFacets(List)
     * @verifies construct query correctly
     */
    @Test
    public void buildExpandQueryFromFacets_shouldConstructQueryCorrectly() throws Exception {
        List<String> facets = new ArrayList<>(2);
        facets.add("FOO:bar");
        facets.add("(FACET_DC:\"foo.bar\" OR FACET_DC:foo.bar.*)");
        Assert.assertEquals("+FOO:bar +(FACET_DC:\"foo.bar\" OR FACET_DC:foo.bar.*) +DOCTYPE:DOCSTRCT",
                SearchHelper.buildExpandQueryFromFacets(facets, Collections.emptyList()));
    }

    /**
     * @see SearchHelper#buildExpandQueryFromFacets(List,List)
     * @verifies only use queries that match allowed regex
     */
    @Test
    public void buildExpandQueryFromFacets_shouldOnlyUseQueriesThatMatchAllowedRegex() throws Exception {
        // Regular query
        List<String> facets = new ArrayList<>(2);
        facets.add("FOO:bar");
        facets.add("(FACET_DC:\"foo.bar\" OR FACET_DC:foo.bar.*)");
        Assert.assertEquals("+FOO:bar +DOCTYPE:DOCSTRCT",
                SearchHelper.buildExpandQueryFromFacets(facets, Collections.singletonList("FOO:bar")));

        // Via regex
        String regex = "\\(FACET_DC:\"a.b[\\.\\w]*\" OR FACET_DC:a.b[\\.\\w]*\\.\\*\\)";

        facets = new ArrayList<>(2);
        facets.add("(FACET_DC:\"a.x\" OR FACET_DC:a.x.*)");
        facets.add("(FACET_DC:\"a.b\" OR FACET_DC:a.b.*)");
        Assert.assertEquals("+(FACET_DC:\"a.b\" OR FACET_DC:a.b.*) +DOCTYPE:DOCSTRCT",
                SearchHelper.buildExpandQueryFromFacets(facets, Collections.singletonList(regex)));

        facets = new ArrayList<>(2);
        facets.add("(FACET_DC:\"a.x.c.d\" OR FACET_DC:a.x.c.d*)");
        facets.add("(FACET_DC:\"a.b.c.d\" OR FACET_DC:a.b.c.d.*)");
        Assert.assertEquals("+(FACET_DC:\"a.b.c.d\" OR FACET_DC:a.b.c.d.*) +DOCTYPE:DOCSTRCT",
                SearchHelper.buildExpandQueryFromFacets(facets, Collections.singletonList(regex)));
    }

    /**
     * @see SearchHelper#buildExpandQueryFromFacets(List,List)
     * @verifies return empty string of no query allowed
     */
    @Test
    public void buildExpandQueryFromFacets_shouldReturnEmptyStringOfNoQueryAllowed() throws Exception {
        List<String> facets = new ArrayList<>(2);
        facets.add("FOO:bar");
        facets.add("(FACET_DC:\"foo.bar\" OR FACET_DC:foo.bar.*)");
        Assert.assertTrue("+FOO:bar +DOCTYPE:DOCSTRCT",
                SearchHelper.buildExpandQueryFromFacets(facets, Collections.singletonList("YARGLE:bargle")).isEmpty());
    }

    /**
     * @see SearchHelper#parseSearchQueryGroupFromQuery(String,Locale)
     * @verifies parse phrase search query correctly
     */
    @Test
    public void parseSearchQueryGroupFromQuery_shouldParsePhraseSearchQueryCorrectly() throws Exception {
        SearchQueryGroup group = SearchHelper.parseSearchQueryGroupFromQuery(
                "(+(SUPERDEFAULT:\"foo bar\" SUPERFULLTEXT:\"foo bar\" SUPERUGCTERMS:\"foo bar\" DEFAULT:\"foo bar\" FULLTEXT:\"foo bar\" NORMDATATERMS:\"foo bar\" UGCTERMS:\"foo bar\" CMS_TEXT_ALL:\"foo bar\") +(SUPERFULLTEXT:\"bla blüp\" FULLTEXT:\"bla blüp\"))",
                null, null, "en");
        Assert.assertNotNull(group);
        Assert.assertEquals(SearchQueryGroupOperator.AND, group.getOperator());
        Assert.assertEquals(3, group.getQueryItems().size());

        Assert.assertEquals(SearchQueryItem.ADVANCED_SEARCH_ALL_FIELDS, group.getQueryItems().get(0).getField());
        Assert.assertEquals("foo bar", group.getQueryItems().get(0).getValue());
        Assert.assertEquals(SearchItemOperator.AND, group.getQueryItems().get(0).getOperator());

        Assert.assertEquals(SolrConstants.FULLTEXT, group.getQueryItems().get(1).getField());
        Assert.assertEquals("bla blüp", group.getQueryItems().get(1).getValue());
        Assert.assertEquals(SearchItemOperator.AND, group.getQueryItems().get(1).getOperator());
    }

    /**
     * @see SearchHelper#parseSearchQueryGroupFromQuery(String,Locale)
     * @verifies parse regular search query correctly
     */
    @Test
    public void parseSearchQueryGroupFromQuery_shouldParseRegularSearchQueryCorrectly() throws Exception {
        SearchQueryGroup group = SearchHelper.parseSearchQueryGroupFromQuery(
                "(+(SUPERDEFAULT:(foo bar) SUPERFULLTEXT:(foo bar) SUPERUGCTERMS:(foo bar) DEFAULT:(foo bar) FULLTEXT:(foo bar) NORMDATATERMS:(foo bar) UGCTERMS:(foo bar) CMS_TEXT_ALL:(foo bar)) -(SUPERFULLTEXT:(bla AND blüp) FULLTEXT:(bla AND blüp)))",
                null, null, "en");
        Assert.assertNotNull(group);
        Assert.assertEquals(SearchQueryGroupOperator.AND, group.getOperator());
        Assert.assertEquals(3, group.getQueryItems().size());

        Assert.assertEquals(SearchQueryItem.ADVANCED_SEARCH_ALL_FIELDS, group.getQueryItems().get(0).getField());
        Assert.assertEquals("foo bar", group.getQueryItems().get(0).getValue());
        Assert.assertEquals(SearchItemOperator.AND, group.getQueryItems().get(0).getOperator());

        Assert.assertEquals(SolrConstants.FULLTEXT, group.getQueryItems().get(1).getField());
        Assert.assertEquals("bla blüp", group.getQueryItems().get(1).getValue());
        Assert.assertEquals(SearchItemOperator.NOT, group.getQueryItems().get(1).getOperator());
    }

    /**
     * @see SearchHelper#parseSearchQueryGroupFromQuery(String,String,Locale)
     * @verifies parse range items correctly
     */
    @Test
    public void parseSearchQueryGroupFromQuery_shouldParseRangeItemsCorrectly() throws Exception {
        SearchQueryGroup group = SearchHelper.parseSearchQueryGroupFromQuery("(MD_YEARPUBLISH:([1900 TO 2000]))", null, null, "en");
        Assert.assertNotNull(group);
        Assert.assertEquals(SearchQueryGroupOperator.AND, group.getOperator());
        Assert.assertEquals(3, group.getQueryItems().size());

        Assert.assertEquals("MD_YEARPUBLISH", group.getQueryItems().get(0).getField());
        Assert.assertEquals("1900", group.getQueryItems().get(0).getValue());
        Assert.assertEquals("2000", group.getQueryItems().get(0).getValue2());
        Assert.assertEquals(SearchItemOperator.OR, group.getQueryItems().get(0).getOperator());
    }

    /**
     * @see SearchHelper#parseSearchQueryGroupFromQuery(String,String,Locale)
     * @verifies parse items from facet string correctly
     */
    @Test
    public void parseSearchQueryGroupFromQuery_shouldParseItemsFromFacetStringCorrectly() throws Exception {
        SearchQueryGroup group = SearchHelper.parseSearchQueryGroupFromQuery("", "DC:varia;;MD_CREATOR:bar;;", null, "en");
        Assert.assertNotNull(group);
        Assert.assertEquals(SearchQueryGroupOperator.AND, group.getOperator());
        Assert.assertEquals(3, group.getQueryItems().size());

        Assert.assertEquals(SolrConstants.DC, group.getQueryItems().get(0).getField());
        Assert.assertEquals("varia", group.getQueryItems().get(0).getValue());
        Assert.assertEquals(SearchItemOperator.AND, group.getQueryItems().get(0).getOperator());

        Assert.assertEquals("MD_CREATOR", group.getQueryItems().get(1).getField());
        Assert.assertEquals("bar", group.getQueryItems().get(1).getValue());
        Assert.assertEquals(SearchItemOperator.AND, group.getQueryItems().get(1).getOperator());
    }

    /**
     * @see SearchHelper#parseSearchQueryGroupFromQuery(String,Locale)
     * @verifies parse mixed search query correctly
     */
    @Test
    public void parseSearchQueryGroupFromQuery_shouldParseMixedSearchQueryCorrectly() throws Exception {
        SearchQueryGroup group = SearchHelper.parseSearchQueryGroupFromQuery(
                "(+(SUPERDEFAULT:\"foo bar\" SUPERFULLTEXT:\"foo bar\" SUPERUGCTERMS:\"foo bar\" DEFAULT:\"foo bar\" FULLTEXT:\"foo bar\" NORMDATATERMS:\"foo bar\" UGCTERMS:\"foo bar\" CMS_TEXT_ALL:\"foo bar\") (SUPERFULLTEXT:(bla AND blüp) FULLTEXT:(bla AND blüp)) +(DOCSTRCT_TOP:\"monograph\") -(MD_YEARPUBLISH:([1900 TO 2000])))",
                "DC:varia;;MD_CREATOR:bar;;", null, "en");
        Assert.assertNotNull(group);
        Assert.assertEquals(6, group.getQueryItems().size());

        Assert.assertEquals(SearchQueryItem.ADVANCED_SEARCH_ALL_FIELDS, group.getQueryItems().get(0).getField());
        Assert.assertEquals("foo bar", group.getQueryItems().get(0).getValue());
        Assert.assertEquals(SearchItemOperator.AND, group.getQueryItems().get(0).getOperator());

        Assert.assertEquals(SolrConstants.FULLTEXT, group.getQueryItems().get(1).getField());
        Assert.assertEquals("bla blüp", group.getQueryItems().get(1).getValue());
        Assert.assertEquals(SearchItemOperator.OR, group.getQueryItems().get(1).getOperator());

        Assert.assertEquals(SolrConstants.DOCSTRCT_TOP, group.getQueryItems().get(2).getField());
        Assert.assertEquals("monograph", group.getQueryItems().get(2).getValue());
        Assert.assertEquals(SearchItemOperator.AND, group.getQueryItems().get(2).getOperator());

        Assert.assertEquals("MD_YEARPUBLISH", group.getQueryItems().get(3).getField());
        Assert.assertEquals("1900", group.getQueryItems().get(3).getValue());
        Assert.assertEquals("2000", group.getQueryItems().get(3).getValue2());
        Assert.assertEquals(SearchItemOperator.NOT, group.getQueryItems().get(3).getOperator());

        Assert.assertEquals(SolrConstants.DC, group.getQueryItems().get(4).getField());
        Assert.assertEquals("varia", group.getQueryItems().get(4).getValue());
        Assert.assertEquals(SearchItemOperator.AND, group.getQueryItems().get(4).getOperator());

        Assert.assertEquals("MD_CREATOR", group.getQueryItems().get(5).getField());
        Assert.assertEquals("bar", group.getQueryItems().get(5).getValue());
        Assert.assertEquals(SearchItemOperator.AND, group.getQueryItems().get(5).getOperator());
    }
}
