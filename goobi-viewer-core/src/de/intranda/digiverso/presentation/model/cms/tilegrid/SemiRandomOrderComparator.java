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
package de.intranda.digiverso.presentation.model.cms.tilegrid;

import java.util.Comparator;
import java.util.Random;
import java.util.function.Function;

import org.apache.poi.ss.formula.functions.T;

/**
 * @author Florian Alpers
 *
 */
public class SemiRandomOrderComparator<T> implements Comparator<T> {
 
    private final Function<T, Integer> comparisonOperator;
    private final Random random = new Random(System.nanoTime());
    
    public SemiRandomOrderComparator(Function<T, Integer> comparisonOperator) {
        this.comparisonOperator = comparisonOperator;
    }
    
    
    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(T a, T b) {
        Integer nA = comparisonOperator.apply(a);
        Integer nB = comparisonOperator.apply(b);
        if(nA.equals(0)) {
            nA = Integer.MAX_VALUE;
        }
        if(nB.equals(0)) {
            nB = Integer.MAX_VALUE;
        }
        
        if(nA.equals(nB)) {
            //return random
            int res = random.nextInt(3)-1;
            return res;
        } else {
            int res = nA.compareTo(nB);
            return res;
        }
    }

}
