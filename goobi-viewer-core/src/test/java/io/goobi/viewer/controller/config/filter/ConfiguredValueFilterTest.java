package io.goobi.viewer.controller.config.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.variables.ReplacerVariables;
import io.goobi.viewer.model.variables.VariableReplacer;

class ConfiguredValueFilterTest {

    VariableReplacer vr = new VariableReplacer(Map.of(
            "record", Map.of("DC", List.of("abra.ka.dabra.simsala.bim", "hex, hex")),
            "page", Map.of(ReplacerVariables.MIME_TYPE, List.of("application/epub-zip"))));

    /**
     * @verifies pass show filter
     * @see ConfiguredValueFilter#passes
     */
    @Test
    void passes_shouldPassShowFilter() {
        ConfiguredValueFilter filter = ConfiguredValueFilter.getShowFilter("{record.DC}", "abra\\.ka\\.dabra.*");
        assertTrue(filter.passes(vr));
    }

    /**
     * @verifies block show filter
     * @see ConfiguredValueFilter#passes
     */
    @Test
    void passes_shouldBlockShowFilter() {
        ConfiguredValueFilter filter = ConfiguredValueFilter.getShowFilter("{record.DC}", "visibili.vanitar");
        assertFalse(filter.passes(vr));
    }

    /**
     * @verifies block hide filter
     * @see ConfiguredValueFilter#getHideFilter
     */
    @Test
    void getHideFilter_shouldBlockHideFilter() {
        ConfiguredValueFilter filter = ConfiguredValueFilter.getHideFilter("{record.DC}", "abra\\.ka\\.dabra.*");
        assertFalse(filter.passes(vr));
    }

    /**
     * @verifies pass hide filter
     * @see ConfiguredValueFilter#getHideFilter
     */
    @Test
    void getHideFilter_shouldPassHideFilter() {
        ConfiguredValueFilter filter = ConfiguredValueFilter.getHideFilter("{record.DC}", "visibili.vanitar");
        assertTrue(filter.passes(vr));
    }

    /**
     * @verifies block conditions apply
     * @see ConfiguredValueFilter#passes
     */
    @Test
    void passes_shouldBlockConditionsApply() {
        ConfiguredValueFilter filter = ConfiguredValueFilter.getHideFilter("{record.DC}", "abra\\.ka\\.dabra.*");
        ConfiguredValueFilter condition = ConfiguredValueFilter.getShowFilter("{page.mimeType}", "image/tiff");
        assertFalse(filter.passes(vr));
        filter.addCondition(condition);
        assertTrue(filter.passes(vr));
    }

}
