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
package de.intranda.digiverso.presentation.servlets.rest.bookshelves;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.intranda.digiverso.presentation.TestUtils;
import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.RestApiException;
import de.intranda.digiverso.presentation.model.bookshelf.Bookshelf;

/**
 * @author Florian Alpers
 *
 */
public class BookshelfResourceTest {

    private static final String PI_1 = "pi1";
    private static final String PI_2 = "pi2";
    private static final String PI_3 = "pi3";
    private static final String LOGID_1 = "LOG_0004";
    private static final String PAGE_1 = "7";
        
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        Configuration configuration = new Configuration("resources/test/config_viewer.test.xml");
        DataManager.getInstance().injectConfiguration(configuration );
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() throws DAOException, IOException, RestApiException {
        
        BookshelfResource resource = new BookshelfResource(TestUtils.mockHttpRequest());
        
        Bookshelf bs = resource.getBookshelf();
        Assert.assertNotNull(bs);
        
        resource.addToBookshelf(PI_1);
        Assert.assertEquals(1, resource.countItems(), 0);
        resource.addToBookshelf(PI_2);
        Assert.assertEquals(2, resource.countItems(), 0);
        resource.addToBookshelf(PI_3, LOGID_1, PAGE_1);
        Assert.assertEquals(3, resource.countItems(), 0);
        
        Assert.assertTrue(resource.isInBookshelf(PI_2));
        Assert.assertTrue(resource.isInBookshelf(PI_3, LOGID_1, PAGE_1));
        Assert.assertFalse(resource.isInBookshelf(PI_2, LOGID_1, PAGE_1));
        Assert.assertFalse(resource.isInBookshelf(PI_3));
        
        resource.deleteFromBookshelf(PI_2);
        Assert.assertEquals(2, resource.countItems(), 0);

        resource.deleteFromBookshelf(PI_3);
        Assert.assertEquals(2, resource.countItems(), 0);
        
        resource.deleteFromBookshelf(PI_3, LOGID_1, PAGE_1);
        Assert.assertEquals(1, resource.countItems(), 0);
        
        resource.deleteBookshelf();
        Assert.assertEquals(0, resource.countItems(), 0);
    }

}
