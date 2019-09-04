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
package io.goobi.viewer.model.annotation;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.intranda.api.annotation.wa.WebAnnotation;
import io.goobi.viewer.controller.DataManager;

/**
 * @author florian
 *
 */
public class PersistentAnnotationTest {

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
    public void testIdConversion() throws JsonParseException, JsonMappingException, IOException {
       
        URI webAnnoURI = URI.create(DataManager.getInstance().getConfiguration().getRestApiUrl() + "annotations/562");
        
        PersistentAnnotation persAnno = new PersistentAnnotation();
        persAnno.setId(PersistentAnnotation.getId(webAnnoURI));
        
        Assert.assertEquals(persAnno.getId(), 562l, 0l);
        
        WebAnnotation webAnno = persAnno.getAsAnnotation();
        
        Assert.assertEquals(webAnno.getId(), webAnnoURI);
        
    }

}
