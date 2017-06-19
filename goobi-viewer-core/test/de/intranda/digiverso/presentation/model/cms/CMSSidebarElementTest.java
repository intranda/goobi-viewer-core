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
package de.intranda.digiverso.presentation.model.cms;

import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

public class CMSSidebarElementTest {

    /**
     * @see CMSSidebarElement#cleanupHtmlTag(String)
     * @verifies remove attributes correctly
     */
    @Test
    public void cleanupHtmlTag_shouldRemoveAttributesCorrectly() throws Exception {
        Assert.assertEquals("<tag>", CMSSidebarElement.cleanupHtmlTag("<tag attribute=\"value\" attribute=\"value\"  >"));
    }

    /**
     * @see CMSSidebarElement#cleanupHtmlTag(String)
     * @verifies remove closing tag correctly
     */
    @Test
    public void cleanupHtmlTag_shouldRemoveClosingTagCorrectly() throws Exception {
        Assert.assertEquals("<tag>", CMSSidebarElement.cleanupHtmlTag("<tag />"));
    }

    @Test
    public void isValidTest() {
        String html = "<dl>dsdf <br /> sadasdsdf</dl>";
        CMSSidebarElement element = new CMSSidebarElement();
        element.setHtml(html);
        assertTrue(element.isValid());
    }

    @Test
    public void replaceHtmlTest() {
        String html = "<dl>dsdf <br> sada<br > sdsdf</dl>";
        String reference = "<dl>dsdf <br /> sada<br /> sdsdf</dl>";
        CMSSidebarElement element = new CMSSidebarElement();
        element.setHtml(html);
        //        System.out.println(element.getHtml());
        assertTrue(element.getHtml().equals(reference));
    }
}