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
package de.intranda.digiverso.presentation.model.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.faces.event.ValueChangeEvent;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.SearchBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.viewer.StringPair;

/**
 * Field/operator/value tuple for the advanced search.
 */
public class SearchQueryItem implements Serializable {

    private static final long serialVersionUID = -367323410132252816L;

    public static final String ADVANCED_SEARCH_ALL_FIELDS = "searchAdvanced_allFields";

    public enum SearchItemOperator {
        AND,
        OR,
        IS,
        PHRASE,
        // AUTO operator will become PHRASE if value is in quotation marks; AND otherwise
        AUTO;

        public String getLabel() {
            return Helper.getTranslation("searchQueryItemOperator_" + this.name(), null);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(SearchQueryItem.class);

    private SearchBean searchBean;
    private String field;
    private SearchItemOperator operator = SearchItemOperator.AND;
    private String value;
    private Locale locale;
    private volatile boolean displaySelectItems = false;

    /** Empty constructor */
    public SearchQueryItem(Locale locale) {
        this.locale = locale;
    }

    /**
     * 
     * @return
     * @should return IS if displaySelectItems true
     * @should return AND, OR, PHRASE if displaySelectItems false
     */
    public List<SearchItemOperator> getAvailableOperators() {
        if (displaySelectItems) {
            return Collections.singletonList(SearchItemOperator.IS);
        }

        return Arrays.asList(new SearchItemOperator[] { SearchItemOperator.AND, SearchItemOperator.OR, SearchItemOperator.PHRASE });
    }

    public List<StringPair> getSelectItems() throws PresentationException, IndexUnreachableException {
        if (locale != null) {
            return getSelectItems(locale.getLanguage());
        }

        return getSelectItems(locale.getLanguage());
    }

    public List<StringPair> getSelectItems(String language) throws PresentationException, IndexUnreachableException {
        if (searchBean == null) {
            searchBean = BeanUtils.getSearchBean();
        }
        if (displaySelectItems && field != null) {
            List<StringPair> ret = searchBean.getAdvancedSearchSelectItems(field, language, isHierarchical());
            if (ret == null) {
                ret = new ArrayList<>();
                logger.warn("No drop-down values found for field: {}", field);
            }
            return ret;
        }

        return Collections.emptyList();
    }

    public void reset() {
        displaySelectItems = false;
        operator = SearchItemOperator.AND;
        field = null;
        value = null;
    }

    /**
     *
     * @return
     */
    public boolean isHierarchical() {
        return DataManager.getInstance().getConfiguration().isAdvancedSearchFieldHierarchical(field);
    }

    /**
     * @return the field
     */
    public String getField() {
        return field;
    }

    /**
     * @param field the field to set
     */
    public void setField(String field) {
        this.field = field;
        toggleDisplaySelectItems();
    }

    /**
     * @return the operator
     * 
     */
    public SearchItemOperator getOperator() {
        checkAutoOperator();
        return operator;
    }

    /**
     * If value is set and operator is AUTO, set it to PHRASE if the value is in quotation marks and to AND otherwise.
     * 
     * @should set operator correctly
     * @should do nothing if no value set
     */
    void checkAutoOperator() {
        if (SearchItemOperator.AUTO.equals(operator) && value != null) {
            if (value.startsWith("\"") && value.endsWith("\"")) {
                operator = SearchItemOperator.PHRASE;
            } else {
                operator = SearchItemOperator.AND;
            }
            logger.trace("AUTO operator automatically set to {}.", operator);
        }
    }

    /**
     * @param operator the operator to set
     */
    public void setOperator(SearchItemOperator operator) {
        this.operator = operator;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    public boolean isDisplaySelectItems() {
        return displaySelectItems;
    }

    /**
     * This is called after <code>setField</code>, so no point in calling <code>toggleDisplaySelectItems</code> here.
     *
     * @param ev
     */
    public void selectOneMenuListener(ValueChangeEvent ev) {
    }

    protected void toggleDisplaySelectItems() {
        if (field != null) {
            if (isHierarchical()) {
                displaySelectItems = true;
                return;
            }
            switch (field) {
                case SolrConstants.DOCSTRCT:
                    displaySelectItems = true;
                    break;
                default:
                    displaySelectItems = false;
            }
            //            logger.trace("toggleDisplaySelectItems: {}:{}", field, displaySelectItems);
        }
    }

    /**
     * Generates the advanced query part for this item.
     * 
     * @param searchTerms
     * @param aggregateHits
     * @return
     * @should generate query correctly
     * @should escape reserved characters
     * @should always use OR operator if searching in all fields
     * @should preserve truncation
     */
    public String generateQuery(Set<String> searchTerms, boolean aggregateHits) {
        checkAutoOperator();
        StringBuilder sbItem = new StringBuilder();

        if (StringUtils.isBlank(value)) {
            return "";
        }

        List<String> fields = new ArrayList<>();
        if (ADVANCED_SEARCH_ALL_FIELDS.equals(field)) {
            // Search everywhere
            if (aggregateHits) {
                // When doing an aggregated search, make sure to include both SUPER and regular fields (because sub-elements don't have the SUPER)
                fields.add(SolrConstants.SUPERDEFAULT);
                fields.add(SolrConstants.SUPERFULLTEXT);
            }
            fields.add(SolrConstants.DEFAULT);
            fields.add(SolrConstants.FULLTEXT);
            fields.add(SolrConstants.NORMDATATERMS);
            fields.add(SolrConstants.UGCTERMS);
            fields.add(SolrConstants.OVERVIEWPAGE_DESCRIPTION);
            fields.add(SolrConstants.OVERVIEWPAGE_PUBLICATIONTEXT);
        } else {
            if (SolrConstants.SUPERDEFAULT.equals(field) || SolrConstants.DEFAULT.equals(field)) {
                if (aggregateHits) {
                    fields.add(SolrConstants.SUPERDEFAULT);
                }
                fields.add(SolrConstants.DEFAULT);
            } else if (SolrConstants.SUPERFULLTEXT.equals(field) || SolrConstants.FULLTEXT.equals(field)) {
                if (aggregateHits) {
                    fields.add(SolrConstants.SUPERFULLTEXT);
                }
                fields.add(SolrConstants.FULLTEXT);
            } else if (SolrConstants.OVERVIEWPAGE.equals(field)) {
                fields.add(SolrConstants.OVERVIEWPAGE_DESCRIPTION);
                fields.add(SolrConstants.OVERVIEWPAGE_PUBLICATIONTEXT);
            } else {
                fields.add(field);
            }
        }
        switch (operator) {
            case IS:
            case PHRASE:
            // Phrase search operator: just the whole value in quotation marks
            {
                if (fields.size() > 1) {
                    sbItem.append('(');
                }
                String useValue = value.trim();
                boolean additionalField = false;
                for (String field : fields) {
                    if (additionalField) {
                        sbItem.append(" OR ");
                    }
                    // No longer using _UNTOKENIZED fields for phrase searches, otherwise only complete field value matches are possible; contained exact matches within a string won't be found (e.g. "foo bar" in DEFAULT:"bla foo bar blup")
                    //                    String useField = field.startsWith("MD_") && !field.endsWith(SolrConstants._UNTOKENIZED) ? new StringBuilder(field).append(
                    //                            SolrConstants._UNTOKENIZED).toString() : field;
                    String useField = field;
                    sbItem.append(useField).append(':');
                    if (useValue.charAt(0) != '"') {
                        sbItem.append('"');
                    }
                    sbItem.append(useValue);
                    if (useValue.charAt(useValue.length() - 1) != '"') {
                        sbItem.append('"');
                    }
                    if (SolrConstants.FULLTEXT.equals(field) || SolrConstants.SUPERFULLTEXT.equals(field)) {
                        String val = useValue.replace("\"", "");
                        if (val.length() > 0) {
                            searchTerms.add(val);
                        }
                    }
                    additionalField = true;
                }
                if (fields.size() > 1) {
                    sbItem.append(')');
                }
            }
                break;
            case AND:
            case OR:
            // AND/OR: e.g. '(FIELD:value1 AND/OR FIELD:"value2" AND/OR -FIELD:value3)' for each query item
            {
                if (!value.trim().isEmpty()) {
                    String[] valueSplit = value.trim().split(" ");
                    boolean moreThanOneField = false;
                    for (String field : fields) {
                        if (moreThanOneField) {
                            sbItem.append(" OR ");
                        }
                        String useField = field;
                        //                    if (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"' && field.startsWith("MD_") && !field.endsWith(
                        //                            LuceneConstants._UNTOKENIZED)) {
                        //                        useField = new StringBuilder(field).append(LuceneConstants._UNTOKENIZED).toString();
                        //                    }
                        sbItem.append(useField).append(':');
                        if (valueSplit.length > 1) {
                            sbItem.append('(');
                        }
                        boolean moreThanOneValue = false;
                        for (String value : valueSplit) {
                            value = value.trim();
                            if (value.charAt(0) == '"') {
                                if (value.charAt(value.length() - 1) != '"') {
                                    // Do not allow " being only on the left
                                    value = value.substring(1);
                                }
                            } else if (value.charAt(value.length() - 1) == '"' && value.charAt(0) != '"') {
                                // Do not allow " being only on the right
                                value = value.substring(0, value.length() - 1);
                            }

                            if (value.charAt(0) == '-' && value.length() > 1) {
                                // negation
                                //                            if (!"*".equals(value)) {
                                //                                // Unless user searches for "contains not *", make sure only documents that actually have the field are found
                                //                                sbItem.append(useField).append(":* ");
                                //                            }
                                sbItem.append(" -");
                                value = value.substring(1);
                            } else if (moreThanOneValue) {
                                switch (this.field) {
                                    case ADVANCED_SEARCH_ALL_FIELDS:
                                        sbItem.append(" OR ");
                                        break;
                                    default:
                                        sbItem.append(' ').append(operator.name()).append(' ');
                                        break;
                                }
                            }
                            // Lowercase the search term for certain fields
                            switch (useField) {
                                case SolrConstants.DEFAULT:
                                case SolrConstants.SUPERDEFAULT:
                                case SolrConstants.FULLTEXT:
                                case SolrConstants.SUPERFULLTEXT:
                                case SolrConstants.NORMDATATERMS:
                                case SolrConstants.UGCTERMS:
                                case SolrConstants.OVERVIEWPAGE_DESCRIPTION:
                                case SolrConstants.OVERVIEWPAGE_PUBLICATIONTEXT:
                                    value = value.toLowerCase();
                                    break;
                                default:
                                    if (field.startsWith("MD_")) {
                                        value = value.toLowerCase();
                                    }
                                    break;
                            }

                            if (value.contains("-")) {
                                // Hack to enable fuzzy searching for terms that contain hyphens
                                sbItem.append('"').append(ClientUtils.escapeQueryChars(value)).append('"');
                            } else {
                                // Preserve truncation before escaping
                                String prefix = "";
                                String useValue = value;
                                String suffix = "";
                                if (useValue.startsWith("*")) {
                                    prefix = "*";
                                    useValue = useValue.substring(1);
                                }
                                if (useValue.endsWith("*")) {
                                    suffix = "*";
                                    useValue = useValue.substring(0, useValue.length() - 1);
                                }

                                sbItem.append(prefix).append(ClientUtils.escapeQueryChars(useValue)).append(suffix);
                            }
                            if (SolrConstants.FULLTEXT.equals(field) || SolrConstants.SUPERFULLTEXT.equals(field)) {
                                String val = value.replace("\"", "");
                                if (val.length() > 0) {
                                    searchTerms.add(val);
                                    // TODO do not add negated terms
                                }
                            }
                            moreThanOneValue = true;
                        }
                        if (valueSplit.length > 1) {
                            sbItem.append(')');
                        }
                        moreThanOneField = true;
                    }
                }
            }
                break;
            default:
                break;
        }

        return sbItem.toString();
    }

    @Override
    public String toString() {
        return field + " " + operator + " " + value;
    }
}
