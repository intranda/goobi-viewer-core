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
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package de.intranda.digiverso.presentation.model.calendar;

import de.intranda.digiverso.presentation.controller.Helper;

public abstract class AbstractCalendarItem implements ICalendarItem {

    protected final String name;
    protected final int value;
    protected int hits;
    protected boolean selected = false;

    protected AbstractCalendarItem(String name, int value, int hits) {
        this.name = name;
        this.value = value;
        this.hits = hits;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.search.ICalendarItem#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.search.ICalendarItem#getValue()
     */
    @Override
    public int getValue() {
        return value;
    }

    /**
     * Returns a two-digit string representation of this item's value.
     * 
     * @return
     */
    public String getFormattedValue() {
        return (Helper.dfTwoDigitInteger.format(value));
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.search.ICalendarItem#getHits()
     */
    @Override
    public int getHits() {
        return hits;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.search.ICalendarItem#setHits(int)
     */
    @Override
    public void setHits(int hits) {
        this.hits = hits;

    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.search.ICalendarItem#isSelected()
     */
    @Override
    public boolean isSelected() {
        return selected;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.search.ICalendarItem#setSelected(boolean)
     */
    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
