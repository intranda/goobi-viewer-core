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
package de.intranda.digiverso.presentation.managedbeans;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.intranda.digiverso.presentation.AbstractDatabaseEnabledTest;
import de.intranda.digiverso.presentation.exceptions.DAOException;

public class CmsMediaBeanTest extends AbstractDatabaseEnabledTest {

    CmsMediaBean bean;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        bean = new CmsMediaBean();
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testSelectedTag() {
        String tag = "sampleTag";
        bean.setSelectedTag(tag);
        Assert.assertEquals(tag, bean.getSelectedTag());
    }

    @Test
    public void testGetAllMediaTags() throws DAOException {
        List<String> tags = bean.getAllMediaTags();
        Assert.assertEquals(3, tags.size());
    }

    @Test
    public void testGetMediaItems() throws DAOException {
        bean.setSelectedTag("");
        Assert.assertEquals(4, bean.getMediaItems().size());
        bean.setSelectedTag("tag1");
        Assert.assertEquals(3, bean.getMediaItems().size());
        bean.setSelectedTag("");
    }

}
