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
package io.goobi.viewer.faces.validators;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Florian Alpers
 *
 */
class HtmlTagValidatorTest {

    @Test
    void test() {
        Assertions.assertFalse(new HtmlTagValidator().validate("abc\njkl  h <p>asdasd</p> ashdoha<br/> asdas"), "Accepts <p> tag");
        Assertions.assertFalse(new HtmlTagValidator().validate("abc\njkl  h <div test=\"asd\">asdasd</div> ashdoha<br/> asdas"),
                "Accepts <div> tag with attribute");
        Assertions.assertTrue(
                new HtmlTagValidator().validate("abc\njkl  h <em>asdasd</em> ashdoha<br/> asdas"), "Does not accept text with em and br");
        Assertions.assertTrue(new HtmlTagValidator().validate("abc\njkl  h <em test=\"asd\">asdasd</em> ashdoha<br/> asdas"),
                "Does not accept text with em with attribute");
        Assertions.assertFalse(new HtmlTagValidator().validate("abc\njkl  h <script type=\"hidden\">asdasd</script> ashdoha<br/> asdas"),
                "Accepts text with script tag");
        Assertions.assertFalse(new HtmlTagValidator()
                .validate("<head><p>asdas</p></head> <body>abc\njkl  h <script type=\"hidden\">asdasd</script> ashdoha<br/> asdas</body>"),
                "Accepts tags in header");
        Assertions.assertFalse(new HtmlTagValidator().validate("<body>abc\njkl  h <div type=\"hidden\">asdasd</div> ashdoha<br/> asdas"),
                "Accepts <body> tag with <div>");
        Assertions.assertFalse(
                new HtmlTagValidator().validate("<head></head><body>abc\njkl  h <script type=\"hidden\">asdasd</script> ashdoha<br/> asdas</body>"),
                "Accepts <body> tag with <script>");
        Assertions.assertFalse(new HtmlTagValidator().validate("<html><script>var i = 1;</script><head></head><body>asdas</body></html>"),
                "Accepts <script> tag in html");
        Assertions.assertFalse(new HtmlTagValidator().validate("<script>var i = 1;</script>"), "Accepts pure <script> tag");
    }
}
