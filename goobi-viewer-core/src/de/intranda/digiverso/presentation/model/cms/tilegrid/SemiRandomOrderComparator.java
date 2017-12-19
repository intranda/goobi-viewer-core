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
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

/**
 * Compares objects of type T by applying the given comparisonOperator and comparing the resulting integers.
 * A comparison value of 0 is considered to come after any other values, and treated as Integer.MAX_VALUE for the comparison.
 * Equal comparison values return a semi-random value of -1, 0 or 1.
 * Each comparator instance fulfills all Comparator.compare contracts because each object gets a fixed random value which is used for all equal value comparisons. 
 * 
 * @author Florian Alpers
 *
 */
public class SemiRandomOrderComparator<T> implements Comparator<T> {
 
    private final Function<T, Integer> comparisonOperator;
    private final Map<T, Integer> map = new IdentityHashMap<>();
    private final Random random = new Random(System.nanoTime());
    
    /**
     * @param comparisonOperator A function from the object to compare to an integer value to use for comparison
     */
    public SemiRandomOrderComparator(Function<T, Integer> comparisonOperator) {
        this.comparisonOperator = comparisonOperator;
    }
    
    
    /**
     * Compares two objects a und b, using the comparisonOperator passed to the Comparator constructor.
     *
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
            return Integer.compare(valueFor(a), valueFor(b));
//            int res = random.nextInt(3)-1;
//            return res;
        } else {
            int res = nA.compareTo(nB);
            return res;
        }
    }


    /**
     * @param nA
     * @return
     */
    private int valueFor(T object) {
        synchronized (map) {
            return map.computeIfAbsent(object, ignore -> random.nextInt());
        }
    }

}
