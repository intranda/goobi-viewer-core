package io.goobi.viewer.controller.config.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.variables.ReplacerVariables;
import io.goobi.viewer.model.variables.VariableReplacer;

class PassedValueFilterTest {

    VariableReplacer vr = new VariableReplacer(Map.of(
            "record", Map.of("DC", List.of("abra.ka.dabra.simsala.bim", "hex, hex")),
            "page", Map.of(ReplacerVariables.MIME_TYPE, List.of("application/epub-zip"))));

    @Test
    void test_passShowFilter() {
        PassedValueFilter filter = PassedValueFilter.getShowFilter("abra\\.ka\\.dabra.*");
        assertTrue(filter.passes("{record.DC}", vr));
    }

    @Test
    void test_blockShowFilter() {
        PassedValueFilter filter = PassedValueFilter.getShowFilter("visibili.vanitar");
        assertFalse(filter.passes("{record.DC}", vr));
    }

    @Test
    void test_blockHideFilter() {
        PassedValueFilter filter = PassedValueFilter.getHideFilter("abra\\.ka\\.dabra.*");
        assertFalse(filter.passes("{record.DC}", vr));
    }

    @Test
    void test_passHideFilter() {
        PassedValueFilter filter = PassedValueFilter.getHideFilter("visibili.vanitar");
        assertTrue(filter.passes("{record.DC}", vr));
    }

    @Test
    void test_block_conditionsApply() {
        PassedValueFilter filter = PassedValueFilter.getHideFilter("abra\\.ka\\.dabra.*");
        ConfiguredValueFilter condition = ConfiguredValueFilter.getShowFilter("{page.mimeType}", "image/tiff");
        assertFalse(filter.passes("{record.DC}", vr));
        filter.addCondition(condition);
        assertTrue(filter.passes("{record.DC}", vr));
    }

}
