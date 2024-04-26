package io.goobi.viewer.controller.config.filter;

import java.util.List;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;

import io.goobi.viewer.model.variables.VariableReplacer;

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
