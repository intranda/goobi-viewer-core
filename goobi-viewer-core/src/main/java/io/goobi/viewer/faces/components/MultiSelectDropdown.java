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
package io.goobi.viewer.faces.components;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UINamingContainer;
import jakarta.faces.context.FacesContext;

/**
 * @author florian
 *
 */
@FacesComponent("io.goobi.viewer.faces.components.MultiSelectDropdown")
@SuppressWarnings("unchecked")
public class MultiSelectDropdown extends UINamingContainer {

    enum PropertyKeys {
        valueMap
    }

    public MultiSelectDropdown() {
        super();
    }

    /* (non-Javadoc)
     * @see jakarta.faces.component.UIComponentBase#encodeBegin(jakarta.faces.context.FacesContext)
     */
    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        super.encodeBegin(context);
        List<Object> values = (List<Object>) getValueExpression("value").getValue(context.getELContext());
        List<Object> options = (List<Object>) getValueExpression("items").getValue(context.getELContext());
        Map<Object, Boolean> valueMap = options.stream().collect(Collectors.toMap(Function.identity(), o -> values.contains(getValue(o))));
        this.setValueMap(valueMap);
        super.encodeBegin(context);
    }

    public Map<Object, Boolean> getValueMap() {
        return (Map<Object, Boolean>) getStateHelper().eval(PropertyKeys.valueMap, Collections.emptyMap());
    }

    public void setValueMap(Map<Object, Boolean> values) {
        getStateHelper().put(PropertyKeys.valueMap, values);
        getValueExpression("value").setValue(getFacesContext().getELContext(),
                values.entrySet().stream().filter(Entry::getValue).collect(Collectors.toList()));
    }

    public String getLabel(Object item) {
        return item.toString();
    }

    public Object getValue(Object item) {
        return item;
    }

}
