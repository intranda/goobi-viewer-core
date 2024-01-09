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
package io.goobi.viewer.model.termbrowsing;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import io.goobi.viewer.AbstractTest;

public class BrowseTermComparatorTest extends AbstractTest {

    /**
     * @see BrowseTermComparator#compare(BrowseTerm,BrowseTerm)
     * @verifies compare correctly
     */
    @Test
    public void compare_shouldCompareCorrectly() throws Exception {
        BrowseTermComparator comparator = new BrowseTermComparator(null);
        Assertions.assertEquals(1, comparator.compare(new BrowseTerm("foo", null, null), new BrowseTerm("bar", null, null)));
        Assertions.assertEquals(-1, comparator.compare(new BrowseTerm("A", null, null), new BrowseTerm("Á", null, null)));
        Assertions.assertEquals(-1, comparator.compare(new BrowseTerm("Azcárate", null, null), new BrowseTerm("Ávila", null, null)));
        Assertions.assertEquals(0, comparator.compare(new BrowseTerm("foo123", null, null), new BrowseTerm("foo123", null, null)));
        Assertions.assertEquals(-1, comparator.compare(new BrowseTerm("foo12", null, null), new BrowseTerm("foo123", null, null)));
        Assertions.assertEquals(-1, comparator.compare(new BrowseTerm("12foo", null, null), new BrowseTerm("123foo", null, null)));
    }

    /**
     * @see BrowseTermComparator#compare(BrowseTerm,BrowseTerm)
     * @verifies use sort term if provided
     */
    @Test
    public void compare_shouldUseSortTermIfProvided() throws Exception {
        Assertions.assertEquals(-1, new BrowseTermComparator(null).compare(new BrowseTerm("foo", "1", null), new BrowseTerm("bar", "2", null)));
    }

    /**
     * @see BrowseTermComparator#compare(BrowseTerm,BrowseTerm)
     * @verifies use translated term if provided
     */
    @Test
    public void compare_shouldUseTranslatedTermIfProvided() throws Exception {
        Map<String, String> translations1 = new HashMap<>();
        translations1.put("de", "Deutsch");
        translations1.put("en", "German");
        Map<String, String> translations2 = new HashMap<>();
        translations2.put("de", "Englisch");
        translations2.put("en", "English");

        Assertions.assertEquals(-1,
                new BrowseTermComparator(Locale.GERMAN).compare(new BrowseTerm("ger", null, new MultiLanguageMetadataValue(translations1)),
                        new BrowseTerm("eng", null, new MultiLanguageMetadataValue(translations2))));

        Assertions.assertEquals(1,
                new BrowseTermComparator(Locale.ENGLISH).compare(new BrowseTerm("ger", null, new MultiLanguageMetadataValue(translations1)),
                        new BrowseTerm("eng", null, new MultiLanguageMetadataValue(translations2))));
    }


    /**
     * @see BrowseTermComparator#compare(BrowseTerm,BrowseTerm)
     * @verifies sort accented vowels after plain vowels
     */
    @Test
    public void compare_shouldSortAccentedVowelsAfterPlainVowels() throws Exception {
//        Assertions.assertEquals(1, new BrowseTermComparator(null).compare(new BrowseTerm("Ávila", null, null), new BrowseTerm("Azcárate", null, null)));
        Assertions.assertEquals(-1, new BrowseTermComparator(null).compare(new BrowseTerm("arm", null, null), new BrowseTerm("árm", null, null)));
    }

    /**
     * @see BrowseTermComparator#normalizeString(String,String)
     * @verifies use ignoreChars if provided
     */
    @Test
    public void normalizeString_shouldUseIgnoreCharsIfProvided() throws Exception {
        Assertions.assertEquals("#.foo", BrowseTermComparator.normalizeString("[.]#.foo", ".[]"));
    }

    /**
     * @see BrowseTermComparator#normalizeString(String,String)
     * @verifies remove first char if non alphanum if ignoreChars not provided
     */
    @Test
    public void normalizeString_shouldRemoveFirstCharIfNonAlphanumIfIgnoreCharsNotProvided() throws Exception {
        Assertions.assertEquals(".]#.foo", BrowseTermComparator.normalizeString("[.]#.foo", null));
    }
}
