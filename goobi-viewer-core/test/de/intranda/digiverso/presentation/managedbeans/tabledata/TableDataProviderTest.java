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
package de.intranda.digiverso.presentation.managedbeans.tabledata;

import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.intranda.digiverso.presentation.managedbeans.tabledata.TableDataProvider.SortOrder;

/**
 * @author Florian Alpers
 *
 */
public class TableDataProviderTest {

    List<Integer> sourceList;
    TableDataProvider<Integer> provider;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        sourceList = IntStream.rangeClosed(1, 2005)
                .boxed()
                .collect(Collectors.toList());
        Collections.shuffle(sourceList);
        
        provider = new TableDataProvider<>(new TableDataSource<Integer>() {

            private Optional<Long> totalDataSize = Optional.empty();
                
            @Override
            public List<Integer> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters)
                    throws TableDataSourceException {

                return sourceList.stream()
                        .sorted((i, j) -> {
                            switch (sortOrder) {
                                case DESCENDING:
                                    return j.compareTo(i);
                                case ASCENDING:
                                    return i.compareTo(j);
                                default:
                                    return 0;
                            }
                        })
                        .filter(i -> matches(i, filters))
                        .skip(first)
                        .limit(pageSize)
                        .collect(Collectors.toList());
            }

            @Override
            public long getTotalNumberOfRecords(Map<String, String> filters) {
                if (!totalDataSize.isPresent()) {
                    totalDataSize = Optional.of(sourceList.stream().filter(i -> matches(i, filters)).count())
                            .map(i -> Long.valueOf(i));
                }
                return totalDataSize.orElse(0l);
            }

            @Override
            public void resetTotalNumberOfRecords() {
                this.totalDataSize = Optional.empty();
            }
        });
        provider.setFilters("MIN", "MAX");
        provider.setEntriesPerPage(10);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetFirstPage() {
        provider.resetAll();
        List<Integer> list = provider.getPaginatorList();
        Assert.assertEquals(10, list.size());
        Assert.assertEquals(1, list.get(0), 0);
        Assert.assertEquals(10, list.get(9), 0);
    }
    
    @Test
    public void testGetTotalSize() {
        provider.resetAll();
        Assert.assertEquals(2005, provider.getSizeOfDataList());
    }
    
    @Test
    public void testChangePageSize() {
        provider.resetAll();
        List<Integer> list = provider.getPaginatorList();
        Assert.assertEquals(10, list.size());
        provider.setEntriesPerPage(5);
        list = provider.getPaginatorList();
        Assert.assertEquals(5, list.size());
        provider.setEntriesPerPage(20);
        list = provider.getPaginatorList();
        Assert.assertEquals(20, list.size());
    }

    @Test
    public void testGotoLastPage() {
        provider.resetAll();
        provider.cmdMoveLast();
        List<Integer> list = provider.getPaginatorList();
        Assert.assertEquals(5, list.size(),  0);
        Assert.assertEquals(2005, list.get(4), 0);
    }
    
    @Test
    public void testGotoFirstPage() {
        provider.resetAll();
        provider.cmdMoveLast();
        provider.cmdMoveFirst();
        List<Integer> list = provider.getPaginatorList();
        Assert.assertEquals(10, list.size(),  0);
        Assert.assertEquals(10, list.get(9), 0);
    }
    
    @Test
    public void testGotoNextPage() {
        provider.resetAll();
        provider.cmdMoveNext();
        List<Integer> list = provider.getPaginatorList();
        Assert.assertEquals(10, list.size(),  0);
        Assert.assertEquals(11, list.get(0), 0);
    }
    
    @Test
    public void testGotoPreviousPage() {
        provider.resetAll();
        provider.cmdMoveLast();
        provider.cmdMovePrevious();
        List<Integer> list = provider.getPaginatorList();
        Assert.assertEquals(10, list.size(),  0);
        Assert.assertEquals(2000, list.get(9), 0);
    }
    
    @Test
    public void testGotoPage() {
        provider.resetAll();
        provider.setTxtMoveTo(100);
        List<Integer> list = provider.getPaginatorList();
        Assert.assertEquals(10, list.size(),  0);
        Assert.assertEquals(1000, list.get(9), 0);
    }
    
    @Test
    public void testFilter() {
        provider.resetAll();
        provider.getFilterAsOptional("MIN").ifPresent(filter -> filter.setValue("100"));
        Assert.assertEquals("100", provider.getFilterAsOptional("MIN").map(filter -> filter.getValue()).orElse("x"));
        provider.update();
        Assert.assertEquals(2005-99, provider.getSizeOfDataList());
        List<Integer> list = provider.getPaginatorList();
        Assert.assertEquals(100, list.get(0), 0);
        
        provider.getFilterAsOptional("MAX").ifPresent(filter -> filter.setValue("200"));
        Assert.assertEquals("200", provider.getFilterAsOptional("MAX").map(filter -> filter.getValue()).orElse("x"));
        provider.update();
        Assert.assertEquals(101, provider.getSizeOfDataList());
        list = provider.getPaginatorList();
        provider.cmdMoveLast();
        list = provider.getPaginatorList();
        Assert.assertEquals(200, list.get(list.size()-1), 0);
        provider.cmdMoveFirst();
        list = provider.getPaginatorList();
        Assert.assertEquals(100, list.get(0), 0);
        
        provider.getFilterAsOptional("MIN").ifPresent(filter -> filter.setValue(""));
        Assert.assertEquals("", provider.getFilterAsOptional("MIN").map(filter -> filter.getValue()).orElse("x"));
        provider.update();
        Assert.assertEquals(200, provider.getSizeOfDataList());
        list = provider.getPaginatorList();
        provider.cmdMoveFirst();
        list = provider.getPaginatorList();
        Assert.assertEquals(1, list.get(0), 0);
        provider.cmdMoveLast();
        list = provider.getPaginatorList();
        Assert.assertEquals(200, list.get(list.size()-1), 0);
        
    }
    
    @Test
    public void testSorting() {
        provider.resetAll();
        provider.setSortOrder(SortOrder.DESCENDING);
        Assert.assertEquals(2005, provider.getPaginatorList().get(0), 0);
        provider.cmdMoveLast();
        Assert.assertEquals(1, provider.getPaginatorList().get(provider.getPaginatorList().size()-1), 0);
    }
    
    /**
     * @param i
     * @param filters
     * @return
     */
    protected boolean matches(Integer i, Map<String, String> filters) {
        return filters.entrySet().stream()
        .allMatch(entry -> {
            switch(entry.getKey()) {
                case "MIN":
                        Integer min = StringUtils.isNotBlank(entry.getValue()) ? Integer.parseInt(entry.getValue()) : Integer.MIN_VALUE;
                        return i >= min;
                case "MAX":
                    Integer max = StringUtils.isNotBlank(entry.getValue()) ? Integer.parseInt(entry.getValue()) : Integer.MAX_VALUE;
                    return i <= max;
                default: 
                        return true;
            }
        });
    }

}
