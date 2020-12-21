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

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.viewer.StringPair;

/**
 * Field/operator/value tuple for the advanced search.
 */
public class SearchQueryItem implements Serializable {

    private static final long serialVersionUID = -367323410132252816L;

    /** Constant <code>ADVANCED_SEARCH_ALL_FIELDS="searchAdvanced_allFields"</code> */
    public static final String ADVANCED_SEARCH_ALL_FIELDS = "searchAdvanced_allFields";

    public enum SearchItemOperator {
        AND,
        OR,
        IS,
        PHRASE,
        // AUTO operator will become PHRASE if value is in quotation marks; AND otherwise
        AUTO;

        public String getLabel() {
            return ViewerResourceBundle.getTranslation("searchQueryItemOperator_" + this.name(), null);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(SearchQueryItem.class);

    private SearchBean searchBean;
    private String field;
    private SearchItemOperator operator = SearchItemOperator.AND;
    private String value;
    private String value2;
    private Locale locale;
    private volatile boolean displaySelectItems = false;

    /**
     * Empty constructor
     *
     * @param locale a {@link java.util.Locale} object.
     */
    public SearchQueryItem(Locale locale) {
        this.locale = locale;
    }

    /**
     * <p>
     * getAvailableOperators.
     * </p>
     *
     * @should return IS if displaySelectItems true
     * @should return AND, OR, PHRASE if displaySelectItems false
     * @return a {@link java.util.List} object.
     */
    public List<SearchItemOperator> getAvailableOperators() {
        if (displaySelectItems) {
            return Collections.singletonList(SearchItemOperator.IS);
        }

        return Arrays.asList(new SearchItemOperator[] { SearchItemOperator.AND, SearchItemOperator.OR, SearchItemOperator.PHRASE });
    }

    /**
     * <p>
     * getSelectItems.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<StringPair> getSelectItems() throws PresentationException, IndexUnreachableException, DAOException {
        if (locale != null) {
            return getSelectItems(locale.getLanguage());
        }

        return getSelectItems(null);
    }

    /**
     * <p>
     * getSelectItems.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<StringPair> getSelectItems(String language) throws PresentationException, IndexUnreachableException, DAOException {
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

    /**
     * <p>
     * reset.
     * </p>
     */
    public void reset() {
        displaySelectItems = false;
        operator = SearchItemOperator.AND;
        field = null;
        value = null;
        value2 = null;
    }

    /**
     * <p>
     * isHierarchical.
     * </p>
     *
     * @return true or false
     */
    public boolean isHierarchical() {
        return DataManager.getInstance().getConfiguration().isAdvancedSearchFieldHierarchical(field);
    }

    /**
     * <p>
     * isRange.
     * </p>
     *
     * @return true or false
     */
    public boolean isRange() {
        return DataManager.getInstance().getConfiguration().isAdvancedSearchFieldRange(field);
    }

    /**
     * <p>
     * isUntokenizeForPhraseSearch.
     * </p>
     *
     * @return true or false
     */
    public boolean isUntokenizeForPhraseSearch() {
        return DataManager.getInstance().getConfiguration().isAdvancedSearchFieldUntokenizeForPhraseSearch(field);
    }

    /**
     * <p>
     * Getter for the field <code>field</code>.
     * </p>
     *
     * @return the field
     */
    public String getField() {
        return field;
    }

    /**
     * <p>
     * Setter for the field <code>field</code>.
     * </p>
     *
     * @param field the field to set
     */
    public void setField(String field) {
        this.field = field;
        toggleDisplaySelectItems();
    }

    /**
     * <p>
     * Getter for the field <code>operator</code>.
     * </p>
     *
     * @return the operator
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
     * <p>
     * Setter for the field <code>operator</code>.
     * </p>
     *
     * @param operator the operator to set
     */
    public void setOperator(SearchItemOperator operator) {
        this.operator = operator;
    }

    /**
     * <p>
     * Getter for the field <code>value</code>.
     * </p>
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * <p>
     * Setter for the field <code>value</code>.
     * </p>
     *
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = StringTools.stripJS(value);
    }

    /**
     * @return the value2
     */
    public String getValue2() {
        return value2;
    }

    /**
     * @param value2 the value2 to set
     */
    public void setValue2(String value2) {
        this.value2 = value2;
    }

    /**
     * <p>
     * isDisplaySelectItems.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplaySelectItems() {
        return displaySelectItems;
    }

    /**
     * This is called after <code>setField</code>, so no point in calling <code>toggleDisplaySelectItems</code> here.
     *
     * @param ev a {@link javax.faces.event.ValueChangeEvent} object.
     */
    public void selectOneMenuListener(ValueChangeEvent ev) {
    }

    /**
     * <p>
     * toggleDisplaySelectItems.
     * </p>
     */
    protected void toggleDisplaySelectItems() {
        if (field != null) {
            if (isHierarchical()) {
                displaySelectItems = true;
                return;
            }
            switch (field) {
                case SolrConstants.DOCSTRCT:
                case SolrConstants.DOCSTRCT_TOP:
                case SolrConstants.DOCSTRCT_SUB:
                case SolrConstants.BOOKMARKS:
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
     * @param searchTerms a {@link java.util.Set} object.
     * @param aggregateHits a boolean.
     * @return a {@link java.lang.String} object.
     * @should generate query correctly
     * @should escape reserved characters
     * @should always use OR operator if searching in all fields
     * @should preserve truncation
     * @should generate range query correctly
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
                fields.add(SolrConstants.SUPERUGCTERMS);
            }
            fields.add(SolrConstants.DEFAULT);
            fields.add(SolrConstants.FULLTEXT);
            fields.add(SolrConstants.NORMDATATERMS);
            fields.add(SolrConstants.UGCTERMS);
            fields.add(SolrConstants.CMS_TEXT_ALL);
        } else if (SolrConstants.SUPERDEFAULT.equals(field) || SolrConstants.DEFAULT.equals(field)) {
            if (aggregateHits) {
                fields.add(SolrConstants.SUPERDEFAULT);
            }
            fields.add(SolrConstants.DEFAULT);
        } else if (SolrConstants.SUPERFULLTEXT.equals(field) || SolrConstants.FULLTEXT.equals(field)) {
            if (aggregateHits) {
                fields.add(SolrConstants.SUPERFULLTEXT);
            }
            fields.add(SolrConstants.FULLTEXT);
        } else if (SolrConstants.SUPERUGCTERMS.equals(field) || SolrConstants.UGCTERMS.equals(field)) {
            if (aggregateHits) {
                fields.add(SolrConstants.SUPERUGCTERMS);
            }
            fields.add(SolrConstants.UGCTERMS);
        } else {
            fields.add(field);
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
                    // Use _UNTOKENIZED field for phrase searches if the field is configured for that. In that case, only complete field value 
                    // matches are possible; contained exact matches within a string won't be found (e.g. "foo bar" in DEFAULT:"bla foo bar blup")
                    String useField = field;
                    if (isUntokenizeForPhraseSearch() && !field.endsWith(SolrConstants._UNTOKENIZED)) {
                        useField = field += SolrConstants._UNTOKENIZED;
                    }
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
                if (value.trim().isEmpty()) {
                    break;
                }
                String[] valueSplit = value.trim().split(" ");
                boolean moreThanOneField = false;
                for (String field : fields) {
                    if (moreThanOneField) {
                        sbItem.append(" OR ");
                    }
                    String useField = field;
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
                            case SolrConstants.SUPERUGCTERMS:
                            case SolrConstants.UGCTERMS:
                            case SolrConstants.CMS_TEXT_ALL:
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
                            if (StringUtils.isNotBlank(value2)) {
                                // Range search
                                sbItem.append('[')
                                        .append(ClientUtils.escapeQueryChars(useValue))
                                        .append(" TO ")
                                        .append(ClientUtils.escapeQueryChars(value2.trim()))
                                        .append("]");
                            } else {
                                // Regular search
                                sbItem.append(prefix).append(ClientUtils.escapeQueryChars(useValue)).append(suffix);
                            }
                        }
                        if (SolrConstants.FULLTEXT.equals(field) || SolrConstants.SUPERFULLTEXT.equals(field)) {
                            String val = value.replace("\"", "");
                            if (val.length() > 0) {
                                searchTerms.add(val);
                                // TODO do not add negated terms
                            }
                        }
                        if (StringUtils.isBlank(value2)) {
                            moreThanOneValue = true;
                        }
                    }
                    if (valueSplit.length > 1) {
                        sbItem.append(')');
                    }
                    moreThanOneField = true;
                }
            }
                break;
            default:
                break;
        }

        return sbItem.toString();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return field + " " + operator + " " + value;
    }
}
