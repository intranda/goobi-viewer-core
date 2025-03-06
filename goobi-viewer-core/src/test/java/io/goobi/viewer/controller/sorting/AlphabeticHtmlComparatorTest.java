/*

 * The Alphanum Algorithm is an improved sorting algorithm for strings
 * containing numbers.  Instead of sorting numbers in ASCII order like
 * a standard sort, this algorithm sorts numbers in numeric order.
 *
 * The Alphanum Algorithm is discussed at http://www.DaveKoelle.com
 *
 * Last modified on 2013-02-01 by intranda GmbH
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package io.goobi.viewer.controller.sorting;

import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AlphabeticHtmlComparatorTest {

    @Test
    void testCompareHtml() {
        String h1 =
                "<p><span data-sheets-value=\"{\"1\":2,\"2\":\"Kayser, Lucien. \\\"Tina Gillen\\\". Flydoscope: Magazine de Luxair (2001): Flydoscope: Magazine de Luxair 2001: unknown; Inconnu. \\\"Curriculum Vitae Tina Gillen.\\\"\"}\" data-sheets-userformat=\"{\"2\":15283,\"3\":{\"1\":0},\"4\":{\"1\":3,\"3\":2},\"7\":{\"1\":[{\"1\":2,\"2\":0,\"5\":{\"1\":2,\"2\":0}},{\"1\":0,\"2\":0,\"3\":3},{\"1\":1,\"2\":0,\"4\":1}]},\"8\":{\"1\":[{\"1\":2,\"2\":0,\"5\":{\"1\":2,\"2\":0}},{\"1\":0,\"2\":0,\"3\":3},{\"1\":1,\"2\":0,\"4\":1}]},\"10\":2,\"11\":4,\"12\":0,\"14\":{\"1\":2,\"2\":3815994},\"15\":\"Source Sans Pro,Arial\",\"16\":11}\" data-sheets-textstyleruns=\"{\"1\":0}{\"1\":31,\"2\":{\"6\":1}}{\"1\":99}{\"1\":116,\"2\":{\"2\":{\"1\":2,\"2\":16711680}}}\">Kayser, Lucien. \"Tina Gillen\". <em>Flydoscope: Magazine de Luxair</em> (2001): unknown. Print.</span></p>";
        String h2 =
                "<p>Entringer, Henri. “Splendeur et misère des galleries d’art au Luxembourg”. <em>Nos Cahiers. Lëtzebuerger Zäitschrëft fir Kultur 4 </em>(2005): 49-68. Print.</p>";

        Assertions.assertTrue(h1.compareTo(h2) < 0);
        AlphabeticHtmlComparator<String> comparator = new AlphabeticHtmlComparator<String>(true, Function.identity());
        Assertions.assertTrue(comparator.compare(h1, h2) > 0);

    }

    @Test
    void testStrings() {
        String s1 = "Kayser, Lucien. \"Tina Gillen\". Flydoscope: Magazine de Luxair (2001): unknown. Print.";
        String s2 =
                "Entringer, Henri. “Splendeur et misère des galleries d’art au Luxembourg”. Nos Cahiers. Lëtzebuerger Zäitschrëft fir Kultur 4 (2005): 49-68. Print.";

        Assertions.assertTrue(s1.compareTo(s2) > 0);
        AlphabeticHtmlComparator<String> comparator = new AlphabeticHtmlComparator<String>(true, Function.identity());
        Assertions.assertTrue(comparator.compare(s1, s2) > 0);
    }

    @Test
    void testInvalidHtml() {
        String s1 = "Kayser, Lucien. \"Tina Gillen\". Flydoscope: Magazine de Luxair (2001): unknown. Print.";
        String s2 =
                "<p>Entringer, Henri. “Splendeur et misère des galleries d’art au Luxembourg”/>. <em>Nos Cahiers. Lëtzebuerger</div> Zäitschrëft fir<p> Kultur 4 (2005): 49-68. Print.</p></em>";

        Assertions.assertTrue(s1.compareTo(s2) > 0);
        AlphabeticHtmlComparator<String> comparator = new AlphabeticHtmlComparator<String>(true, Function.identity());
        Assertions.assertTrue(comparator.compare(s1, s2) > 0);
    }

}
