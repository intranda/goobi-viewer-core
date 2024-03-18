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
            "MD_PRONOUN", List.of("you", "her", "them")
            );

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
