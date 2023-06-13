/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.modules.interfaces;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.model.viewer.PageType;

public class DefaultURLBuilderTest extends AbstractDatabaseEnabledTest {

    /**
     * @see DefaultURLBuilder#buildPageUrl(String,int,String,PageType,boolean)
     * @verifies only add logId if not topStruct
     */
    @Test
    public void buildPageUrl_shouldOnlyAddLogIdIfNotTopStruct() throws Exception {
        IURLBuilder builder = new DefaultURLBuilder();
        Assert.assertEquals("object/PPN123/", builder.buildPageUrl("PPN123", 1, "LOG_0000", PageType.viewObject, true));
        Assert.assertEquals("object/PPN123/2/", builder.buildPageUrl("PPN123", 2, "LOG_0000", PageType.viewObject, true));
        Assert.assertEquals("object/PPN123/1/LOG_0000/", builder.buildPageUrl("PPN123", 1, "LOG_0000", PageType.viewObject, false));
    }
}