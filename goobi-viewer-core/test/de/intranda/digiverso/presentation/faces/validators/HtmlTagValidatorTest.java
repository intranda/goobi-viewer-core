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
package de.intranda.digiverso.presentation.faces.validators;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Florian Alpers
 *
 */
public class HtmlTagValidatorTest {

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
    public void test() {
        Assert.assertFalse("Accepts <p> tag",new HtmlTagValidator().validate("abc\njkl  h <p>asdasd</p> ashdoha<br/> asdas"));
        Assert.assertFalse("Accepts <div> tag with attribute", new HtmlTagValidator().validate("abc\njkl  h <div test=\"asd\">asdasd</div> ashdoha<br/> asdas"));
        Assert.assertTrue("Does not accept text with em and br",new HtmlTagValidator().validate("abc\njkl  h <em>asdasd</em> ashdoha<br/> asdas"));
        Assert.assertTrue("Does not accept text with em with attribute",new HtmlTagValidator().validate("abc\njkl  h <em test=\"asd\">asdasd</em> ashdoha<br/> asdas"));
        Assert.assertFalse("Accepts text with script tag",new HtmlTagValidator().validate("abc\njkl  h <script type=\"hidden\">asdasd</script> ashdoha<br/> asdas"));
        Assert.assertFalse("Accepts tags in header",new HtmlTagValidator().validate("<head><p>asdas</p></head> <body>abc\njkl  h <script type=\"hidden\">asdasd</script> ashdoha<br/> asdas</body>"));
        Assert.assertFalse("Accepts <body> tag with <div>",new HtmlTagValidator().validate("<body>abc\njkl  h <div type=\"hidden\">asdasd</div> ashdoha<br/> asdas"));
        Assert.assertFalse("Accepts <body> tag with <script>",new HtmlTagValidator().validate("<head></head><body>abc\njkl  h <script type=\"hidden\">asdasd</script> ashdoha<br/> asdas</body>"));
        Assert.assertFalse("Accepts <script> tag in html",new HtmlTagValidator().validate("<html><script>var i = 1;</script><head></head><body>asdas</body></html>"));
        Assert.assertFalse("Accepts pure <script> tag",new HtmlTagValidator().validate("<script>var i = 1;</script>"));

    }

}
