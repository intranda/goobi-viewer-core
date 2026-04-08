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
 * Abstract base class for calendar browsing items representing a time period (year, month, day, etc.).
 */
public abstract class AbstractCalendarItem implements ICalendarItem {

    /** Constant <code>dfTwoDigitInteger</code>. */
    private static DecimalFormat dfTwoDigitInteger = new DecimalFormat("00");

    protected String name;
    protected int value;
    protected int hits;
    protected boolean selected = false;

    /** No-arg constructor. */
    protected AbstractCalendarItem() {
    }

    /**
     * Creates a new AbstractCalendarItem instance.
     *
     * @param name display label for this item
     * @param value numeric calendar value (e.g. day, month, year)
     * @param hits initial number of search hits
     */
    protected AbstractCalendarItem(String name, int value, int hits) {
        this.name = name;
        this.value = value;
        this.hits = hits;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

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

    /** {@inheritDoc} */
    @Override
    public int getHits() {
        return hits;
    }

    /** {@inheritDoc} */
    @Override
    public void setHits(int hits) {
        this.hits = hits;

    }

    /** {@inheritDoc} */
    @Override
    public boolean isSelected() {
        return selected;
    }

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
