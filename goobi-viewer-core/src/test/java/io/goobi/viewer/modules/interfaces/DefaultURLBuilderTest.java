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

import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.model.search.BrowseElement;
import io.goobi.viewer.model.viewer.PageType;

public class DefaultURLBuilderTest extends AbstractDatabaseEnabledTest {

    /**
     * @see DefaultURLBuilder#generateURL(BrowseElement)
     * @verifies only add page if not topStruct or page greater than one
     */
    @Test
    public void generateURL_shouldOnlyAddPageIfNotTopStructOrPageGreaterThanOne() throws Exception {
        BrowseElement be = new BrowseElement("PPN123", 1, "Title", null, Locale.ENGLISH, null, null);
        be.setWork(true);
        IURLBuilder builder = new DefaultURLBuilder();
        Assertions.assertEquals("metadata/PPN123/", builder.generateURL(be));
        be.setImageNo(2);
        Assertions.assertEquals("metadata/PPN123/2/", builder.generateURL(be));
        be.setImageNo(1);
        be.setWork(false);
        Assertions.assertEquals("metadata/PPN123/1/-/", builder.generateURL(be));
    }

    /**
     * @see DefaultURLBuilder#generateURL(BrowseElement)
     * @verifies only add logId if not topStruct
     */
    @Test
    public void generateURL_shouldOnlyAddLogIdIfNotTopStruct() throws Exception {
        BrowseElement be = new BrowseElement("PPN123", 1, "Title", null, Locale.ENGLISH, null, null);
        be.setWork(true);
        be.setLogId("LOG_0000");
        IURLBuilder builder = new DefaultURLBuilder();
        Assertions.assertEquals("metadata/PPN123/", builder.generateURL(be));
        be.setImageNo(2);
        Assertions.assertEquals("metadata/PPN123/2/", builder.generateURL(be));
        be.setImageNo(1);
        be.setWork(false);
        Assertions.assertEquals("metadata/PPN123/1/LOG_0000/", builder.generateURL(be));
    }

    /**
     * @see DefaultURLBuilder#buildPageUrl(String,int,String,PageType,boolean)
     * @verifies only add page if not topStruct or page greater than one
     */
    @Test
    public void buildPageUrl_shouldOnlyAddPageIfNotTopStructOrPageGreaterThanOne() throws Exception {
        IURLBuilder builder = new DefaultURLBuilder();
        Assertions.assertEquals("object/PPN123/", builder.buildPageUrl("PPN123", 1, null, PageType.viewObject, true));
        Assertions.assertEquals("object/PPN123/2/", builder.buildPageUrl("PPN123", 2, null, PageType.viewObject, true));
        Assertions.assertEquals("object/PPN123/1/-/", builder.buildPageUrl("PPN123", 1, null, PageType.viewObject, false));
    }

    /**
     * @see DefaultURLBuilder#buildPageUrl(String,int,String,PageType,boolean)
     * @verifies only add logId if not topStruct
     */
    @Test
    public void buildPageUrl_shouldOnlyAddLogIdIfNotTopStruct() throws Exception {
        IURLBuilder builder = new DefaultURLBuilder();
        Assertions.assertEquals("object/PPN123/", builder.buildPageUrl("PPN123", 1, "LOG_0000", PageType.viewObject, true));
        Assertions.assertEquals("object/PPN123/2/", builder.buildPageUrl("PPN123", 2, "LOG_0000", PageType.viewObject, true));
        Assertions.assertEquals("object/PPN123/1/LOG_0000/", builder.buildPageUrl("PPN123", 1, "LOG_0000", PageType.viewObject, false));
    }
}