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
package io.goobi.viewer.model.calendar;

import java.text.DecimalFormat;

/**
 * <p>
 * Abstract AbstractCalendarItem class.
 * </p>
 */
public abstract class AbstractCalendarItem implements ICalendarItem {

    /** Constant <code>dfTwoDigitInteger</code> */
    private static DecimalFormat dfTwoDigitInteger = new DecimalFormat("00");

    protected String name;
    protected int value;
    protected int hits;
    protected boolean selected = false;

    /** No-arg constructor. */
    protected AbstractCalendarItem() {
    }

    /**
     * <p>
     * Constructor for AbstractCalendarItem.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @param value a int.
     * @param hits a int.
     */
    protected AbstractCalendarItem(String name, int value, int hits) {
        this.name = name;
        this.value = value;
        this.hits = hits;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.ICalendarItem#getName()
     */
    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.ICalendarItem#getValue()
     */
    /** {@inheritDoc} */
    @Override
    public int getValue() {
        return value;
    }

    /**
     * Returns a two-digit string representation of this item's value.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getFormattedValue() {
        return (dfTwoDigitInteger.format(value));
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.ICalendarItem#getHits()
     */
    /** {@inheritDoc} */
    @Override
    public int getHits() {
        return hits;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.ICalendarItem#setHits(int)
     */
    /** {@inheritDoc} */
    @Override
    public void setHits(int hits) {
        this.hits = hits;

    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.ICalendarItem#isSelected()
     */
    /** {@inheritDoc} */
    @Override
    public boolean isSelected() {
        return selected;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.ICalendarItem#setSelected(boolean)
     */
    /** {@inheritDoc} */
    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isEmpty() {
        return this.hits < 1;
    }

    public void addHits(Integer additionalHits) {
        this.hits += additionalHits;
    }
}
