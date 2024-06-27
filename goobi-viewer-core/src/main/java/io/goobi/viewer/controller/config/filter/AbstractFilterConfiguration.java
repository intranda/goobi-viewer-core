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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;

import io.goobi.viewer.model.variables.VariableReplacer;

/**
 * A configurable filter allowing passage to document entities (record, docStruct, page) which satisfy certain conditions. The filter itself may
 * contain condition filters which determine if the filter should be applied to an entity
 */
public abstract class AbstractFilterConfiguration implements IFilterConfiguration {

    /**
     * Whether the filter should block or pass entities meeting its condition.
     */
    protected final FilterAction action;
    /**
     * Additional filters which must be passed in order for this filter to apply. If any filter conditions don't pass, this
     * #{@link #applies(VariableReplacer)} will always return false
     */
    protected final List<AbstractFilterConfiguration> filterConditions;

    /**
     * internal constructor.
     * 
     * @param action whether to pass or block matching entities
     */
    protected AbstractFilterConfiguration(FilterAction action) {
        this.action = action;
        this.filterConditions = new ArrayList<>();
    }

    /**
     * Create a new filter from a configuration block.
     * 
     * @param config an xml configuration
     * @throws ConfigurationException if the config is invalid
     */
    public void addConditionFilters(HierarchicalConfiguration<ImmutableNode> config) throws ConfigurationException {

        for (HierarchicalConfiguration<ImmutableNode> condition : config.configurationsAt("conditions.filter", false)) {
            this.addCondition(ConfiguredValueFilter.fromConfiguration(condition));
        }
    }

    /**
     * Test whether all conditions of this filter apply, if any.
     * 
     * @param vr a variable replacer representing the object to test
     * @return true if the {@link #filterConditions} all pass. If they don't the filter should not be applied
     */
    public boolean applies(VariableReplacer vr) {
        return this.filterConditions.stream().allMatch(condition -> condition.passes("", vr));
    }

    /**
     * Add a conditional filter. The main filter is only applied if all conditional filters pass
     * 
     * @param condition
     */
    public void addCondition(AbstractFilterConfiguration condition) {
        if (condition != this && !condition.getFilterConditions().contains(this)) {
            this.filterConditions.add(condition);
        }
    }

    /**
     * Get the {@link FilterAction}.
     * 
     * @return the {@link FilterAction}
     */
    public FilterAction getAction() {
        return action;
    }

    /**
     * Get all {@link #filterConditions}.
     * 
     * @return the {@link #filterConditions}
     */
    public List<AbstractFilterConfiguration> getFilterConditions() {
        return Collections.unmodifiableList(filterConditions);
    }

    /**
     * check if matching this filter results in a pass or block
     * 
     * @return true if {@link #action} is {@link FilterAction#SHOW}.
     */
    public boolean passesOnMatch() {
        return FilterAction.SHOW == this.action;
    }

}
