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
package io.goobi.viewer.managedbeans;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.model.annotation.PersistentAnnotation;

/**
 * @author florian
 *
 */
public class AnnotationBeanTest extends AbstractDatabaseEnabledTest {

    AnnotationBean bean;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        bean = new AnnotationBean();
        bean.init();
        createDatabaseEntries();
    }


    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * 
     */
    private void createDatabaseEntries() {
        // TODO Auto-generated method stub
        
    }
    
    @Test
    public void testGetAllConfiguredAnnotations() {
        List<PersistentAnnotation> firstPageAnnotations = bean.getLazyModelAnnotations().getPaginatorList();   
        
        firstPageAnnotations.stream().forEach(System.out::println);
//        firstPageAnnotations.stream().map(anno -> anno.getGeneratorId()).forEach(System.out::println);
//        firstPageAnnotations.stream().map(anno -> bean.getOwningCampaign(anno).orElse(null)).filter(c -> c != null).map(c -> c.getId()).forEach(System.out::println);
        
        Assert.assertEquals(5, firstPageAnnotations.size());
    }
    
    @Test
    public void testGetAllAnnotationsOfCampaign() {
        bean.setOwnerCampaignId("1");
        List<PersistentAnnotation> firstPageAnnotations = bean.getLazyModelAnnotations().getPaginatorList();   
        Assert.assertEquals(3, firstPageAnnotations.size());
    }
    
    @Test
    public void testGetAllAnnotationsOfRecord() {
        bean.setTargetRecordPI("PI_2");
        List<PersistentAnnotation> firstPageAnnotations = bean.getLazyModelAnnotations().getPaginatorList();   
        Assert.assertEquals(2, firstPageAnnotations.size());
    }
    
}
