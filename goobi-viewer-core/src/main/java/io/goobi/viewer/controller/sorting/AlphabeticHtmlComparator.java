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

import java.text.Collator;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class AlphabeticHtmlComparator<T> implements Comparator<T> {

    private final int reverse;
    private final Collator col;
    private final Function<T, String> stringifier;

    public AlphabeticHtmlComparator(boolean asc, Function<T, String> stringifier) {
        super();
        this.reverse = asc ? 1 : -1;
        col = Collator.getInstance();
        col.setStrength(Collator.PRIMARY);
        this.stringifier = stringifier;
    }

    @Override
    public int compare(T v1, T v2) {
        String s1 = this.stringifier.apply(v1);
        String s2 = this.stringifier.apply(v2);
        Document d1 = Jsoup.parse(s1);
        Document d2 = Jsoup.parse(s2);
        String test = d1.text();
        String test2 = d2.text();
        return this.reverse * Optional.ofNullable(d1.text()).orElse("").compareTo(Optional.ofNullable(d2.text()).orElse(""));
    }

}
