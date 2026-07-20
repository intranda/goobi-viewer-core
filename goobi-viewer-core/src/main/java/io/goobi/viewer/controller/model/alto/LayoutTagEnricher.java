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

package io.goobi.viewer.controller.model.alto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.LineElement;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoTags.TagType;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.Tag;

/**
 * {@link TextEnricher} implementation that applies inline visual markup to ALTO word tokens carrying word-level
 * layout tags. Each configured tag LABEL maps to a CSS class that is added to a wrapping {@code <span>}; a word
 * carrying several mapped tags gets all their classes on a single span. The default mapping covers underlined
 * ({@code gr_unterstrichen}) and struck-through ({@code gr_durchgestrichen}) words, but it can be overridden via
 * configuration (see {@code Configuration.getAltoLayoutWordTags()}).
 */
public class LayoutTagEnricher implements TextEnricher {

    static final String LABEL_UNDERLINE = "gr_unterstrichen";
    static final String LABEL_STRIKETHROUGH = "gr_durchgestrichen";

    static final String CLASS_UNDERLINE = "alto-tag--underline";
    static final String CLASS_STRIKETHROUGH = "alto-tag--strikethrough";

    /** Default mapping of word-level layout-tag LABEL to CSS style class. */
    static final Map<String, String> DEFAULT_STYLE_CLASSES = buildDefaults();

    private final Map<String, String> labelToStyleClass;

    /** Uses the built-in default label-to-style-class mapping. */
    public LayoutTagEnricher() {
        this(null);
    }

    /**
     * @param labelToStyleClass mapping of layout-tag LABEL to CSS style class; if null or empty, the built-in
     *            defaults are used
     */
    public LayoutTagEnricher(Map<String, String> labelToStyleClass) {
        this.labelToStyleClass =
                (labelToStyleClass == null || labelToStyleClass.isEmpty()) ? DEFAULT_STYLE_CLASSES : labelToStyleClass;
    }

    /**
     * Wraps the word content in a styled span if the word references any mapped word-level layout tag.
     *
     * @param content the textual content of the word being enriched (possibly already wrapped by a previous enricher)
     * @param element the ALTO line element whose word content is being processed
     * @return the span-wrapped content if a mapped layout tag applies, otherwise the unchanged {@code content}
     * @should wrap underlined words in an underline span
     * @should wrap struck through words in a strikethrough span
     * @should combine both classes on a single span
     * @should leave untagged words unchanged
     */
    @Override
    public String enrich(String content, LineElement element) {
        List<String> classes = new ArrayList<>();
        for (Tag tag : element.getTags()) {
            if (tag.getTagType() != TagType.LAYOUT_TAG) {
                continue;
            }
            String styleClass = labelToStyleClass.get(tag.getLabel());
            if (styleClass != null && !classes.contains(styleClass)) {
                classes.add(styleClass);
            }
        }
        if (classes.isEmpty()) {
            return content;
        }
        return "<span class=\"alto-tag " + String.join(" ", classes) + "\">" + content + "</span>";
    }

    private static Map<String, String> buildDefaults() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(LABEL_UNDERLINE, CLASS_UNDERLINE);
        map.put(LABEL_STRIKETHROUGH, CLASS_STRIKETHROUGH);
        return map;
    }

}
