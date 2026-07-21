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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.LineElement;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoTags.TagType;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.Tag;

/**
 * {@link TextEnricher} implementation that applies inline visual markup to ALTO word tokens carrying word-level
 * layout tags. Each mapped tag LABEL maps to a CSS class that is added to a wrapping {@code <span>}; a word carrying
 * several mapped tags gets all their classes on a single span. The mapping is supplied by the caller (from
 * configuration, see {@code Configuration.getAltoLayoutWordTags()}); there are no built-in labels, since ALTO
 * LayoutTag LABELs are specific to how a given collection's ALTO was produced.
 */
public class LayoutTagEnricher implements TextEnricher {

    private final Map<String, String> labelToStyleClass;

    /** Applies no markup (empty mapping). */
    public LayoutTagEnricher() {
        this(null);
    }

    /**
     * @param labelToStyleClass mapping of layout-tag LABEL to CSS style class; null is treated as empty (no markup)
     */
    public LayoutTagEnricher(Map<String, String> labelToStyleClass) {
        this.labelToStyleClass = (labelToStyleClass == null) ? Collections.emptyMap() : labelToStyleClass;
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

}
