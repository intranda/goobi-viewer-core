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
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.controller;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>
 * AlphabetIterator class.
 * </p>
 *
 * @author Florian Alpers
 */
public class AlphabetIterator implements Iterator<String> {

    private char currentValue;

    /**
     * <p>
     * Constructor for AlphabetIterator.
     * </p>
     */
    public AlphabetIterator() {
        currentValue = decrement('a');
    }

    /**
     * <p>
     * Constructor for AlphabetIterator.
     * </p>
     *
     * @param firstLetter a char.
     */
    public AlphabetIterator(char firstLetter) {
        if (firstLetter >= 'a' && firstLetter <= 'z') {
            currentValue = decrement(firstLetter);
        } else {
            throw new IllegalArgumentException("Can only start with lower case letters");
        }
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    /** {@inheritDoc} */
    @Override
    public boolean hasNext() {
        return currentValue < 'z';
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    /** {@inheritDoc} */
    @Override
    public String next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        currentValue = increment(currentValue);
        return String.valueOf(currentValue);
    }

    private static char increment(char c) {
        int i = (c) + 1;
        return (char) i;
    }

    private static char decrement(char c) {
        int i = (c) - 1;
        return (char) i;
    }

}
