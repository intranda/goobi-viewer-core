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
package io.goobi.viewer.model.security;

import static org.junit.Assert.*;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.model.security.LicenseType;

/**
 * @author Florian Alpers
 *
 */
public class LicenseTypeTest {
    
    private static final String CONDITION_QUERY_1 = "(DOCTYPE:Monograph AND isWork:true) -DC:privatecollection";
    private static final String CONDITION_FILENAME_1 = "(private|secret)\\..{2-4}";

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
    public void testGetProcessedConditions() {
        LicenseType type = new LicenseType();
        type.setConditions("FILENAME:{" + CONDITION_FILENAME_1 + "}");
        Assert.assertTrue("processed conditions are " + type.getProcessedConditions(), StringUtils.isBlank(type.getProcessedConditions()));
    
        type.setConditions("FILENAME:{" + CONDITION_FILENAME_1 + "} " + CONDITION_QUERY_1);
        Assert.assertTrue("processed conditions are " + type.getProcessedConditions(), type.getProcessedConditions().equals(CONDITION_QUERY_1));;

        type.setConditions(CONDITION_QUERY_1 + "FILENAME:{" + CONDITION_FILENAME_1 + "}");
        Assert.assertTrue("processed conditions are " + type.getProcessedConditions(), type.getProcessedConditions().equals(CONDITION_QUERY_1));

        type.setConditions(CONDITION_QUERY_1);
        Assert.assertTrue("processed conditions are " + type.getProcessedConditions(), type.getProcessedConditions().equals(CONDITION_QUERY_1));
    }
    
    @Test
    public void testGetFilenameConditions() {
        LicenseType type = new LicenseType();
        type.setConditions("FILENAME:{" + CONDITION_FILENAME_1 + "}");
        Assert.assertTrue("filename conditions are " + type.getFilenameConditions(), type.getFilenameConditions().equals(CONDITION_FILENAME_1));
    
        type.setConditions("FILENAME:{" + CONDITION_FILENAME_1 + "} " + CONDITION_QUERY_1);
        Assert.assertTrue("filename conditions are " + type.getFilenameConditions(), type.getFilenameConditions().equals(CONDITION_FILENAME_1));;

        type.setConditions(CONDITION_QUERY_1 + "FILENAME:{" + CONDITION_FILENAME_1 + "}");
        Assert.assertTrue("filename conditions are " + type.getFilenameConditions(), type.getFilenameConditions().equals(CONDITION_FILENAME_1));

        type.setConditions(CONDITION_QUERY_1);
        Assert.assertTrue("filename conditions are " + type.getFilenameConditions(), StringUtils.isBlank(type.getFilenameConditions()));
    }

}
