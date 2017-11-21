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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Florian Alpers
 *
 */
public class SemiRandomOrderComparatorTest {
    
    private static final int testRuns = 100;
    
    private static final SortTestObject[] testArray = {
            new SortTestObject("A", 4), 
            new SortTestObject("B", 5),
            new SortTestObject("C", 0),
            new SortTestObject("D", 3),
            new SortTestObject("E", 7),
            new SortTestObject("F", 9),
            new SortTestObject("G", 6),
            new SortTestObject("H", 0),
            new SortTestObject("I", 9),
            new SortTestObject("J", 2),
            new SortTestObject("K", 0),
            new SortTestObject("L", 5),
            new SortTestObject("M", 5),
            new SortTestObject("N", 1)};

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() {
        
        List<List<SortTestObject>> resultList = new ArrayList<>();
        
        for (int i = 0; i < testRuns; i++) {            
            List<SortTestObject> sortedList = Arrays.stream(testArray)
                    .sorted(new SemiRandomOrderComparator<>(o -> o.number))
                    .collect(Collectors.toList());
            sortedList.forEach(System.out::print);
            System.out.println();
            resultList.add(sortedList);
        }
        for (List<SortTestObject> list : resultList) {            
            Assert.assertTrue(isOrdered(list));
        }
        Assert.assertFalse(allEqual(resultList.stream()
                .map( list -> list.stream()
                        .map(l -> l.name)
                        .collect(Collectors.joining()))
                .collect(Collectors.toList())
                     ));
    }
    
    /**
     * @param collect
     * @return
     */
    private boolean allEqual(List<String> strings) {
        for (String s1 : strings) {
            for (String s2 : strings) {
                if(!s1.equals(s2)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @param list
     * @return
     */
    private boolean isOrdered(List<SortTestObject> list) {
        int lastValue = -1;
        for (SortTestObject o : list) {
            if(o.number > 0) {
                if(lastValue == 0 || lastValue > o.number) {
                    return false;
                }
            }
            lastValue = o.number;
        } 
        return true;
    }

    private static class SortTestObject {
        public String name;
        public Integer number;
        
        /**
         * 
         */
        public SortTestObject(String name, int number) {
            this.name = name;
            this.number = number;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return name + " -> " + number.toString() + "\t";
        }
    }
    
    @Test
    public void testComparatorContract() {
        Random picker = new Random(System.nanoTime());

        //Commutation
        for (int i = 0; i < testRuns; i++) {
            SortTestObject o1 = testArray[picker.nextInt(testArray.length)];
            SortTestObject o2 = testArray[picker.nextInt(testArray.length)];
            
            SemiRandomOrderComparator<SortTestObject> comparator = new SemiRandomOrderComparator<>(o -> o.number);
            
            int resA = comparator.compare(o1, o2);
            int resB = comparator.compare(o2, o1);
            
            Assert.assertEquals("Contract violated for " + o1 + ", " + o2, -Math.signum(resA), Math.signum(resB), 0.0);
        }
        
        //Transitivity
        for (int i = 0; i < testRuns; i++) {
            SortTestObject o1 = testArray[picker.nextInt(testArray.length)];
            SortTestObject o2 = testArray[picker.nextInt(testArray.length)];
            SortTestObject o3 = testArray[picker.nextInt(testArray.length)];
            
            SemiRandomOrderComparator<SortTestObject> comparator = new SemiRandomOrderComparator<>(o -> o.number);
            
            int resA = comparator.compare(o1, o2);
            int resB = comparator.compare(o2, o3);
            int resC = comparator.compare(o1, o3);
            
            if(resA > 0 && resB > 0) {
                Assert.assertEquals("Contract violated for " + o1 + ", " + o2 + ", " + o3, 1.0, Math.signum(resC), 0.0);
            } else if (resA < 0 && resB < 0) {
                Assert.assertEquals("Contract violated for " + o1 + ", " + o2 + ", " + o3, -1.0, Math.signum(resC), 0.0);
            } else if(resA == 0) {
                Assert.assertEquals("Contract violated for " + o1 + ", " + o2 + ", " + o3, Math.signum(resB), Math.signum(resC), 0.0);
            } else if(resB == 0) {
                Assert.assertEquals("Contract violated for " + o1 + ", " + o2 + ", " + o3, Math.signum(resA), Math.signum(resC), 0.0);
            }
            
        }
    }

}
