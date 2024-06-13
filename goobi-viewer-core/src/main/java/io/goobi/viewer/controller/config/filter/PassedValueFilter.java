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
package io.goobi.viewer.controller.config.filter;

import java.util.List;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;

import io.goobi.viewer.model.variables.VariableReplacer;

/**
 * A configurable filter allowing passage to document entities (record, docStruct, page) which satisfy certain conditions. The filter itself may
 * contain condition filters which determine if the filter should be applied to an entity
 */
public final class PassedValueFilter extends AbstractFilterConfiguration {

    /**
     * A regex to test the value against. This may make use of {@link VariableReplacer} expressions
     */
    private final String matchRegex;

    /**
     * internal constructor.
     * 
     * @param action whether to pass or block matching entities
     * @param matchRegex a regex which must match the value parameter for the filter to match
     */
    private PassedValueFilter(FilterAction action, String matchRegex) {
        super(action);
        this.matchRegex = matchRegex;
    }

    /**
     * get a filter which passes all matches.
     * 
     * @param matchRegex a regex which must match the value parameter for the filter to match
     * @return a new {@link PassedValueFilter}
     */
    public static PassedValueFilter getShowFilter(String matchRegex) {
        return new PassedValueFilter(FilterAction.SHOW, matchRegex);
    }

    /**
     * get a filter which blocks all matches.
     * 
     * @param matchRegex a regex which must match the value parameter for the filter to match
     * @return a new {@link PassedValueFilter}
     */
    public static PassedValueFilter getHideFilter(String matchRegex) {
        return new PassedValueFilter(FilterAction.HIDE, matchRegex);
    }

    /**
     * Create a new filter from a configuration block.
     * 
     * @param config an xml configuration
     * @return a new {@link PassedValueFilter}
     * @throws ConfigurationException if the config is invalid
     */
    public static PassedValueFilter fromConfiguration(HierarchicalConfiguration<ImmutableNode> config) throws ConfigurationException {
        String action = config.getString("[@action]", FilterAction.SHOW.name());
        String match = config.getString("[@regex]", "");
        PassedValueFilter filter = FilterAction.getAction(action)
                .map(fa -> new PassedValueFilter(fa, match))
                .orElseThrow(() -> new ConfigurationException("Not a valid filter action: " + action));

        filter.addConditionFilters(config);
        return filter;
    }

    /**
     * This is the main method to apply the filter. It first checks if the conditions apply. If they don't, true is always returned. Otherwise, it
     * returns true if action is {@link FilterAction#SHOW} and {@link #matches(String value, VariableReplacer)} returns true, or if action is
     * {@link FilterAction#HIDE} and {@link #matches(String value, VariableReplacer)} returns false. In all other cases, it returns false
     * 
     * @param value the value to test. May contain {@link VariableReplacer} expressions
     * @param vr a variable replacer containing values to test
     * @return whether conditions apply and the object represented by the variable replacer passes the filter
     */
    public boolean passes(String value, VariableReplacer vr) {
        if (this.applies(vr)) {
            return this.test(value, vr);
        } else {
            return true;
        }
    }

    /**
     * Similar to {@link #matches(Strinf value, VariableReplacer)}, but if {@link #action} is {@link FilterAction#HIDE} the return value is negated.
     * 
     * @param value the value to test. May contain {@link VariableReplacer} expressions
     * @param vr a variable replacer containing values to test
     * @return whether the object represented by the variable replacer passes the filter, ignoring conditions
     */
    private boolean test(String value, VariableReplacer vr) {
        boolean match = matches(value, vr);
        return passesOnMatch() ? match : !match;
    }

    /**
     * Test the match condition on a variable replacer.
     * 
     * @param value the value to test. May contain {@link VariableReplacer} expressions
     * @param vr a variable replacer containing values to test
     * @return whether the {@link PassedValueFilter#value} matches the {@link #matchRegex} if both are filled with values from the variable replacer
     */
    private boolean matches(String value, VariableReplacer vr) {
        List<String> filledValues = vr.replace(value);
        List<String> filledMatchs = vr.replace(this.matchRegex);

        for (String filledMatch : filledMatchs) {
            for (String filledValue : filledValues) {
                if (filledValue.matches(filledMatch)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get get {@link #matchRegex}.
     */
    public String getMatchRegex() {
        return matchRegex;
    }

}
