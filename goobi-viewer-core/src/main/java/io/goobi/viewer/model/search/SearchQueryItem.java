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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.faces.event.ValueChangeEvent;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.jsf.CheckboxSelectable;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

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
        NOT;

        public String getLabel() {
            return ViewerResourceBundle.getTranslation("searchOperator_" + this.name(), null);
        }
    }

    private static final Logger logger = LogManager.getLogger(SearchQueryItem.class);

    private SearchBean searchBean;
    private final String template;
    /** Optional label message key, if different from field. */
    private String label;
    /** Index field to search. */
    private String field;
    /** This operator now describes the relation of this item with the other items rather than between terms within this item's query! */
    private SearchItemOperator operator = SearchItemOperator.AND;
    private List<String> values = new ArrayList<>();
    private volatile boolean displaySelectItems = false;
    /** If >0, proximity search will be applied to phrase searches. */
    private int proximitySearchDistance = 0;

    /**
     * Zero-argument constructor.
     */
    public SearchQueryItem() {
        this(null);
    }

    /**
     * @param template
     */
    public SearchQueryItem(String template) {
        this.template = template;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(field, operator, template, values);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SearchQueryItem other = (SearchQueryItem) obj;
        return Objects.equals(field, other.field) && operator == other.operator && Objects.equals(template, other.template)
                && Objects.equals(values, other.values);
    }

    /**
     * <p>
     * getAvailableOperators.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<SearchItemOperator> getAvailableOperators() {
        return Arrays.asList(SearchItemOperator.AND, SearchItemOperator.OR, SearchItemOperator.NOT);
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
    public List<StringPair> getSelectItems(String language)
            throws PresentationException, IndexUnreachableException, DAOException {
        if (searchBean == null) {
            searchBean = BeanUtils.getSearchBean();
        }
        if (displaySelectItems && field != null) {
            List<StringPair> ret = searchBean.getAdvancedSearchSelectItems(field, language, isHierarchical());
            if (ret == null) {
                ret = new ArrayList<>();
                logger.warn("No values found for field: {}", field);
            }

            return ret;
        }

        return Collections.emptyList();
    }

    /**
     * 
     * @param language
     * @param additionalValues 0-n additional, manually added values
     * @return List&lt;CheckboxSelectable&lt;String&gt;&gt;
     * @throws DAOException
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public List<CheckboxSelectable<String>> getCheckboxSelectables(String language, String... additionalValues)
            throws DAOException, PresentationException, IndexUnreachableException {
        List<CheckboxSelectable<String>> ret = this.getSelectItems(language)
                .stream()
                .map(item -> new CheckboxSelectable<String>(this.values, item.getOne(), s -> item.getTwo()))
                .collect(Collectors.toList());
        if (additionalValues != null && additionalValues.length > 0) {
            for (String additinalValue : additionalValues) {
                if (StringUtils.isNotEmpty(additinalValue)) {
                    ret.add(new CheckboxSelectable<>(this.values, additinalValue, s -> ViewerResourceBundle.getTranslation(additinalValue, null)));
                }
            }
        }

        return ret;
    }

    public CheckboxSelectable<String> getCustomSelectableItem(String value) {
        return new CheckboxSelectable<>(this.values, field, s -> value);
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
        values.clear();
    }

    /**
     * <p>
     * isHierarchical.
     * </p>
     *
     * @return true or false
     */
    public boolean isHierarchical() {
        return DataManager.getInstance().getConfiguration().isAdvancedSearchFieldHierarchical(field, template, true);
    }

    /**
     * <p>
     * isRange.
     * </p>
     *
     * @return true or false
     */
    public boolean isRange() {
        return DataManager.getInstance().getConfiguration().isAdvancedSearchFieldRange(field, template, true);
    }

    /**
     * <p>
     * isUntokenizeForPhraseSearch.
     * </p>
     *
     * @return true or false
     */
    public boolean isUntokenizeForPhraseSearch() {
        return DataManager.getInstance().getConfiguration().isAdvancedSearchFieldUntokenizeForPhraseSearch(field, template, true);
    }

    /**
     * 
     * @return true if selected field is "all fields"; false otherwise
     */
    public boolean isAllFields() {
        return ADVANCED_SEARCH_ALL_FIELDS.equals(field);
    }

    /**
     *
     * @return Configured threshold for displayed select items
     */
    public int getDisplaySelectItemsThreshold() {
        return DataManager.getInstance().getConfiguration().getAdvancedSearchFieldDisplaySelectItemsThreshold(field, template, false);
    }

    /**
     * @return the selectType
     */
    public String getSelectType() {
        return DataManager.getInstance().getConfiguration().getAdvancedSearchFieldSelectType(field, template, false);
    }

    /**
     * @return the label
     * @should return field if label empty
     */
    public String getLabel() {
        if (StringUtils.isEmpty(label)) {
            return field;
        }

        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
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
        return operator;
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
     * @return the values
     */
    public List<String> getValues() {
        return values;
    }

    /**
     * @param values the values to set
     */
    public void setValues(List<String> values) {
        this.values = values;
    }

    /**
     * <p>
     * Getter for the field <code>value</code>.
     * </p>
     *
     * @return the value
     */
    public String getValue() {
        if (!values.isEmpty()) {
            return values.get(0);
        }

        return null;
    }

    /**
     * <p>
     * Setter for the field <code>value</code>.
     * </p>
     *
     * @param value the value to set
     */
    public void setValue(final String value) {
        // logger.trace("setValue: {}", value); //NOSONAR Debug
        String val = StringTools.stripJS(value);
        values.add(0, val);
    }

    /**
     * 
     * @param value
     * @return true if values contains given value; false otherwise
     */
    public boolean isValueSet(String value) {
        return this.values.contains(value);
    }

    /**
     * Sets/unsets the given value in the item, depending on the current status.
     * 
     * @param value Value to set/unset
     * @should set values correctly
     * @should unset values correctly
     */
    public void toggleValue(final String value) {
        String val = StringTools.stripJS(value);
        int index = this.values.indexOf(val);
        if (index >= 0) {
            this.values.remove(index);
        } else {
            this.values.add(val);
        }
    }

    /**
     * @return the value2
     */
    public String getValue2() {
        if (values.size() < 2) {
            return null;
        }

        return values.get(1);
    }

    /**
     * @param value2 the value2 to set
     */
    public void setValue2(final String value2) {
        logger.trace("setValue2: {}", value2);
        String val2 = StringTools.stripJS(value2);
        if (values.isEmpty()) {
            values.add(null);
        }
        values.add(1, val2);
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
     * Setter for unit tets.
     * 
     * @param displaySelectItems the displaySelectItems to set
     */
    void setDisplaySelectItems(boolean displaySelectItems) {
        this.displaySelectItems = displaySelectItems;
    }

    /**
     * This is called after <code>setField</code>, so no point in calling <code>toggleDisplaySelectItems</code> here.
     *
     * @param ev a {@link javax.faces.event.ValueChangeEvent} object.
     */
    public void selectOneMenuListener(ValueChangeEvent ev) {
        //
    }

    /**
     * <p>
     * toggleDisplaySelectItems.
     * </p>
     * 
     * @should set displaySelectItems false if searching in all fields
     * @should set displaySelectItems false if searching in fulltext
     * @should set displaySelectItems true if value count below threshold
     * @should set displaySelectItems false if value count above threshold
     * @should set displaySelectItems false if value count zero
     */
    protected void toggleDisplaySelectItems() {
        if (field == null) {
            return;
        }

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
            case ADVANCED_SEARCH_ALL_FIELDS:
            case SolrConstants.FULLTEXT:
                displaySelectItems = false;
                break;
            default:
                try {
                    // Fields containing less values than the threshold for this field should be displayed as a drop-down
                    String facetField = SearchHelper.facetifyField(field); // use FACET_ to exclude reversed values from the count
                    String suffix = SearchHelper.getAllSuffixes();

                    // Via unique()
                    Map<String, String> params = Collections.singletonMap("json.facet", "{uniqueCount : \"unique(" + facetField + ")\"}");
                    List<String> vals = SearchHelper.getFacetValues(facetField + ":[* TO *]" + suffix, "json:uniqueCount", null, 1, params);
                    int size = !vals.isEmpty() ? Integer.valueOf(vals.get(0)) : 0;

                    if (size > 0 && size < getDisplaySelectItemsThreshold()) {
                        displaySelectItems = true;
                    } else {
                        displaySelectItems = false;
                    }
                } catch (PresentationException | IndexUnreachableException e) {
                    logger.error(SolrTools.extractExceptionMessageHtmlTitle(e.getMessage()));
                    displaySelectItems = false;
                }
        }
        //            logger.trace("toggleDisplaySelectItems: {}:{}", field, displaySelectItems); //NOSONAR Debug
    }

    /**
     * Generates the advanced query part for this item.
     *
     * @param searchTerms a {@link java.util.Set} object.
     * @param aggregateHits a boolean.
     * @param allowFuzzySearch If true, search terms will be augmented by fuzzy search tokens
     * @return a {@link java.lang.String} object.
     * @should generate query correctly
     * @should escape reserved characters
     * @should always use OR operator if searching in all fields
     * @should preserve truncation
     * @should generate range query correctly
     * @should add proximity search token correctly
     */
    public String generateQuery(Set<String> searchTerms, boolean aggregateHits, boolean allowFuzzySearch) {
        if (values.isEmpty() || StringUtils.isBlank(getValue())) {
            return "";
        }
        this.proximitySearchDistance  = 0;
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

        // Detect implicit phrase search
        boolean phrase = false;
        if (SearchHelper.isPhrase(values.get(0).trim())) {
            logger.trace("Phrase detected, changing operator.");
            phrase = true;
        }

        StringBuilder sbItem = new StringBuilder();

        switch (operator) {
            case AND:
                sbItem.append('+');
                break;
            case NOT:
                sbItem.append('-');
                break;
            default:
                break;
        }
        sbItem.append('(');
        // Phrase search operator: just the whole value in quotation marks
        if (phrase || isDisplaySelectItems()) {
            boolean additionalField = false;
            for (final String f : fields) {
                if (additionalField) {
                    sbItem.append(" ");
                }
                // Use _UNTOKENIZED field for phrase searches if the field is configured for that. In that case, only complete field value
                // matches are possible; contained exact matches within a string won't be found (e.g. "foo bar" in DEFAULT:"bla foo bar blup")
                String useField = f;
                if (isUntokenizeForPhraseSearch() && !f.endsWith(SolrConstants.SUFFIX_UNTOKENIZED)) {
                    useField += SolrConstants.SUFFIX_UNTOKENIZED;
                }

                boolean additionalValue = false;
                for (String value : values) {
                    if (StringUtils.isEmpty(value)) {
                        continue;
                    }
                    if (additionalValue) {
                        // TODO AND-option?
                        sbItem.append(' ');
                    }
                    String useValue = value.trim();
                    this.proximitySearchDistance = SearchHelper.extractProximitySearchDistanceFromQuery(useValue);
                    logger.trace("proximity distance: {}", proximitySearchDistance);

                    sbItem.append(useField).append(':');
                    if (useValue.charAt(0) != '"') {
                        sbItem.append('"');
                    }
                    sbItem.append(useValue);
                    if (useValue.charAt(useValue.length() - 1) != '"' && proximitySearchDistance == 0) {
                        sbItem.append('"');
                    }
                    if (SolrConstants.FULLTEXT.equals(useField) || SolrConstants.SUPERFULLTEXT.equals(useField)) {
                        // Remove quotation marks to add to search terms
                        String val = useValue.replace("\"", "");
                        if (val.length() > 0) {
                            searchTerms.add(val);
                        }
                    }
                    additionalField = true;
                    additionalValue = true;
                }
            }
        }
        // AND/OR: e.g. '(FIELD:value1 AND/OR FIELD:"value2" AND/OR -FIELD:value3)' for each query item
        else {
            if (!values.get(0).trim().isEmpty()) {
                String[] valueSplit = values.get(0).trim().split(SearchHelper.SEARCH_TERM_SPLIT_REGEX);
                boolean moreThanOneField = false;
                for (final String f : fields) {
                    if (moreThanOneField) {
                        sbItem.append(" ");
                    }
                    String useField = f;
                    sbItem.append(useField).append(':');
                    sbItem.append('(');
                    boolean moreThanOneValue = false;
                    for (final String v : valueSplit) {
                        String val = v.trim();
                        if (val.length() == 0) {
                            continue;
                        }
                        if (val.charAt(0) == '"') {
                            if (val.charAt(val.length() - 1) != '"') {
                                // Do not allow " being only on the left
                                val = val.substring(1);
                            }
                        } else if (val.charAt(val.length() - 1) == '"' && val.charAt(0) != '"') {
                            // Do not allow " being only on the right
                            val = val.substring(0, val.length() - 1);
                        }

                        if (val.charAt(0) == '-' && val.length() > 1) {
                            // negation
                            sbItem.append(" -");
                            val = val.substring(1);
                        } else if (moreThanOneValue) {
                            if (ADVANCED_SEARCH_ALL_FIELDS.equals(this.field)) {
                                sbItem.append(' ');
                            } else {
                                sbItem.append(SolrConstants.SOLR_QUERY_AND);
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
                                val = val.toLowerCase();
                                break;
                            default:
                                if (field.startsWith("MD_")) {
                                    val = val.toLowerCase();
                                }
                                break;
                        }

                        if (val.contains("-")) {
                            if (allowFuzzySearch) {
                                //remove wildcards; they don't work with search containing hyphen
                                String tempValue = SearchHelper.getWildcardsTokens(val)[1];
                                tempValue = ClientUtils.escapeQueryChars(tempValue);
                                tempValue = SearchHelper.addFuzzySearchToken(tempValue, "", "");
                                sbItem.append("(").append(tempValue).append(")");
                            } else {
                                // Hack to enable fuzzy searching for terms that contain hyphens
                                sbItem.append('"').append(ClientUtils.escapeQueryChars(val)).append('"');
                            }
                        } else {
                            // Preserve truncation before escaping
                            String prefix = "";
                            String useValue = val;
                            String suffix = "";
                            if (useValue.startsWith("*")) {
                                prefix = "*";
                                useValue = useValue.substring(1);
                            }
                            if (useValue.endsWith("*")) {
                                suffix = "*";
                                useValue = useValue.substring(0, useValue.length() - 1);
                            }
                            if (isRange() && values.size() > 1 && StringUtils.isNotBlank(values.get(1))) {
                                // Range search
                                sbItem.append('[')
                                        .append(ClientUtils.escapeQueryChars(useValue))
                                        .append(" TO ")
                                        .append(ClientUtils.escapeQueryChars(values.get(1).trim()))
                                        .append("]");
                            } else {
                                // Regular search
                                String escValue = ClientUtils.escapeQueryChars(useValue);
                                if (allowFuzzySearch) {
                                    escValue = SearchHelper.addFuzzySearchToken(escValue, prefix, suffix);
                                    sbItem.append("(").append(escValue).append(")");
                                } else {
                                    sbItem.append(prefix).append(ClientUtils.escapeQueryChars(useValue)).append(suffix);
                                }
                            }
                        }
                        if (SolrConstants.FULLTEXT.equals(useField) || SolrConstants.SUPERFULLTEXT.equals(useField)) {
                            String v2 = val.replace("\"", "");
                            if (v2.length() > 0) {
                                searchTerms.add(v2);
                                // TODO do not add negated terms
                            }
                        }
                        if (valueSplit.length > 1 || values.size() < 2 || StringUtils.isBlank(values.get(1))) {
                            moreThanOneValue = true;
                        }

                    }
                    sbItem.append(')');
                    moreThanOneField = true;
                }
            }
        }

        sbItem.append(')');

        return sbItem.toString().replace("\\~", "~");
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return field + " " + operator + " " + getValue();
    }
    
    public int getProximitySearchDistance() {
        return proximitySearchDistance;
    }

}
