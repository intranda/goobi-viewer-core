package io.goobi.viewer.managedbeans;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;

public class HighlightedObjectBeanTest extends AbstractDatabaseEnabledTest {

    HighlightedObjectBean bean;
    
    @Before
    public void setup() throws Exception {
        super.setUp();
        bean = new HighlightedObjectBean();
        bean.init();
    }
    
    @Test
    public void test_listObjecs() {
        
        assertEquals(3, bean.getDataProvider().getPaginatorList().size());
        assertEquals(3, bean.getDataProvider().getSizeOfDataList());
        assertEquals(1l, bean.getDataProvider().getPageNumberCurrent().longValue());
        assertEquals(0, bean.getDataProvider().getLastPageNumber());
        bean.getDataProvider().cmdMoveNext();
        assertEquals(1l, bean.getDataProvider().getPageNumberCurrent().longValue());
    }
    
    @Test
    public void test_filterList() {
        
        bean.getDataProvider().getFilter("name").setValue("Monat");
        assertEquals(2, bean.getDataProvider().getSizeOfDataList());
        
        bean.getDataProvider().getFilter("name").setValue("Januar");
        assertEquals(1, bean.getDataProvider().getSizeOfDataList());

        bean.getDataProvider().getFilter("name").setValue("");
        assertEquals(3, bean.getDataProvider().getSizeOfDataList());
    }

}
