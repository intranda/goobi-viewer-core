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
package io.goobi.viewer.model.jsf;

import java.util.Collection;
import java.util.function.Function;

public class CheckboxSelectable<T> {

    private final Collection<T> dataSet;
    private final T value;
    private final Function<T, String> labelGetter;

    /**
     * 
     * @param dataSet
     * @param value
     * @param labelGetter
     */
    public CheckboxSelectable(Collection<T> dataSet, T value, Function<T, String> labelGetter) {
        super();
        this.dataSet = dataSet;
        this.value = value;
        this.labelGetter = labelGetter;
    }

    public boolean isSelected() {
        return this.dataSet.contains(value);
    }

    /**
     * 
     * @param selected
     */
    public void setSelected(boolean selected) {
        if (selected) {
            if (!this.dataSet.contains(this.value)) {
                this.dataSet.add(this.value);
            }
        } else {
            this.dataSet.remove(this.value);
        }
    }

    public String getLabel() {
        return this.labelGetter.apply(this.value);
    }

    @Override
    public String toString() {
        return value.toString() + ": " + isSelected();
    }

}
