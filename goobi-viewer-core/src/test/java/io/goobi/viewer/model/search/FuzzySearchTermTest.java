package io.goobi.viewer.model.search;

import static org.junit.Assert.*;

import org.junit.Test;

public class FuzzySearchTermTest {

    @Test
    public void testSpecialCharacters() {
        String search = "wissenschaftlichen";
        String text = "wisſenſchaftlichen";
        FuzzySearchTerm fuzzy = new FuzzySearchTerm(search);
        assertTrue(fuzzy.matches(text));
    }

}
