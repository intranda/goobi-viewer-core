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
 * Interface for filter configurations that evaluate whether a given value passes a set of conditions.
 * Implementations define a regex match pattern, a pass-on-match flag, and nested filter conditions.
 */
public interface IFilterConfiguration {

    public boolean passes(String value, VariableReplacer vr);

    public String getMatchRegex();

    public FilterAction getAction();

    public boolean passesOnMatch();

    public List<AbstractFilterConfiguration> getFilterConditions();

    public static IFilterConfiguration fromConfiguration(HierarchicalConfiguration<ImmutableNode> config) throws ConfigurationException {

        if (config.containsKey("[@value]")) {
            return ConfiguredValueFilter.fromConfiguration(config);
        } else {
            return PassedValueFilter.fromConfiguration(config);
        }
    }

}
