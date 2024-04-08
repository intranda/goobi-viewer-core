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
package io.goobi.viewer.model.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.model.viewer.StructElement;

class VariableReplacerTest {

    private static final String phraseTemplate = "It's a {EMOTION_QUANTIFIER} {EMOTION} to meet {MD_PRONOUN}!";
    private final static Map<String, List<String>> METADATA_MAP = Map.of(
            "EMOTION_QUANTIFIER", List.of("extraordinary", "great"),
            "EMOTION", List.of("pleasure", "honor", "disappointment"),
            "MD_PRONOUN", List.of("you", "her", "them"));

    @Test
    void test() {
        StructElement struct = Mockito.mock(StructElement.class);
        Mockito.when(struct.getMetadataValues(Mockito.anyString())).thenAnswer(arg -> {
            String s = arg.getArgument(0, String.class);
            return METADATA_MAP.get(s);
        });
        VariableReplacer replacer = new VariableReplacer(struct);
        List<String> phrases = replacer.replace(phraseTemplate);
        assertEquals(3, phrases.size());
        assertTrue(phrases.contains("It's a extraordinary pleasure to meet you!"));
        assertTrue(phrases.contains("It's a great honor to meet her!"));
        assertTrue(phrases.contains("It's a  disappointment to meet them!"));
    }

    @Test
    void test_noVariables() {
        String phraseTemplate = "It's a bloody pleasure to meet him!";
        StructElement struct = Mockito.mock(StructElement.class);
        Mockito.when(struct.getMetadataValues(Mockito.anyString())).thenAnswer(arg -> {
            String s = arg.getArgument(0, String.class);
            return METADATA_MAP.get(s);
        });
        VariableReplacer replacer = new VariableReplacer(struct);

        List<String> phrases = replacer.replace(phraseTemplate);
        assertEquals(1, phrases.size());
        assertTrue(phrases.contains(phraseTemplate));
    }

}
