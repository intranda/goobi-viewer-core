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
package de.intranda.digiverso.presentation.controller.language;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Florian Alpers
 *
 */
public class LocaleComparatorTest {

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
        List<Locale> locales = new ArrayList<>();
        locales.add(Locale.FRANCE);
        locales.add(Locale.ENGLISH);
        locales.add(Locale.ITALIAN);
        locales.add(Locale.GERMAN);
        locales.add(Locale.CHINA);

        List<Locale> germanFirst = locales.stream().sorted(new LocaleComparator(Locale.GERMANY)).collect(Collectors.toList());

        Assert.assertEquals(Locale.GERMAN, germanFirst.get(0));
        Assert.assertEquals(Locale.ENGLISH, germanFirst.get(1));

        List<Locale> englishFirst = locales.stream().sorted(new LocaleComparator(Locale.US)).collect(Collectors.toList());

        Assert.assertEquals(Locale.ENGLISH, englishFirst.get(0));

        List<Locale> chineseFirst = locales.stream().sorted(new LocaleComparator(Locale.CHINESE)).collect(Collectors.toList());

        Assert.assertEquals(Locale.CHINA, chineseFirst.get(0));
        Assert.assertEquals(Locale.ENGLISH, chineseFirst.get(1));

    }

}
