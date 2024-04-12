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
package io.goobi.viewer.controller.model;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.StringUtils;

/**
 * A matcher read from a configuration node which matches strings based on certain criteria
 * 
 * @author florian
 *
 */
public class StringMatchConfiguration implements Predicate<String> {

    private final String includeRegex;
    private final String excludeRegex;
    private final List<String> allowedValues;

    /**
     * 
     * @param includeRegex
     * @param excludeRegex
     * @param allowedValues
     */
    public StringMatchConfiguration(String includeRegex, String excludeRegex, List<String> allowedValues) {
        this.includeRegex = includeRegex;
        this.excludeRegex = excludeRegex;
        this.allowedValues = allowedValues;
    }

    /**
     * 
     * @param allowedValues
     */
    public StringMatchConfiguration(List<String> allowedValues) {
        this("", "", allowedValues);
    }

    /**
     * 
     * @param includeRegex
     * @param excludeRegex
     */
    public StringMatchConfiguration(String includeRegex, String excludeRegex) {
        this(includeRegex, excludeRegex, Collections.emptyList());
    }

    /**
     * 
     * @param includeRegex
     */
    public StringMatchConfiguration(String includeRegex) {
        this(includeRegex, "", Collections.emptyList());
    }

    public boolean test(String s) {
        if (StringUtils.isBlank(s)) {
            return false;
        }
        return (StringUtils.isBlank(includeRegex) || s.matches(includeRegex))
                && (StringUtils.isBlank(excludeRegex) || !s.matches(excludeRegex))
                && (allowedValues.isEmpty() || allowedValues.contains(s));
    }

    /**
     * 
     * @param config
     * @return {@link StringMatchConfiguration}
     */
    public static StringMatchConfiguration fromConfig(HierarchicalConfiguration<ImmutableNode> config) {
        if (config != null) {
            String include = config.getString("regex.include", "");
            String exclude = config.getString("regex.exclude", "");
            List<String> values = config.getList(String.class, "list.value", Collections.emptyList());
            return new StringMatchConfiguration(include, exclude, values);
        }
        return new StringMatchConfiguration("");
    }

}
