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
 * layout tags. Each mapped tag LABEL contributes CSS classes to a wrapping {@code <span>}: a class derived from the
 * tag's semantic type ({@code alto-tag--<type>}, styled by the core) and/or the configured style class verbatim (an
 * optional hook for theme CSS). A word carrying several mapped tags gets all their classes on a single span. Both
 * mappings are supplied by the caller (from configuration, see {@code Configuration.getAltoLayoutWordTagTypes()} and
 * {@code Configuration.getAltoLayoutWordTags()}); there are no built-in labels, since ALTO LayoutTag LABELs are
 * specific to how a given collection's ALTO was produced.
 */
public class LayoutTagEnricher implements TextEnricher {

    /** Prefix of the CSS class derived from a tag's semantic type. */
    private static final String TYPE_CLASS_PREFIX = "alto-tag--";

    private final Map<String, String> labelToType;
    private final Map<String, String> labelToStyleClass;

    /** Applies no markup (empty mappings). */
    public LayoutTagEnricher() {
        this(null, null);
    }

    /**
     * @param labelToType mapping of layout-tag LABEL to its semantic type (e.g. {@code underline}), rendered as
     *            {@code alto-tag--<type>}; null is treated as empty
     * @param labelToStyleClass mapping of layout-tag LABEL to an additional CSS style class applied verbatim; null is
     *            treated as empty
     */
    public LayoutTagEnricher(Map<String, String> labelToType, Map<String, String> labelToStyleClass) {
        this.labelToType = (labelToType == null) ? Collections.emptyMap() : labelToType;
        this.labelToStyleClass = (labelToStyleClass == null) ? Collections.emptyMap() : labelToStyleClass;
    }

    /**
     * Wraps the word content in a styled span if the word references any mapped word-level layout tag.
     *
     * @param content the textual content of the word being enriched (possibly already wrapped by a previous enricher)
     * @param element the ALTO line element whose word content is being processed
     * @return the span-wrapped content if a mapped layout tag applies, otherwise the unchanged {@code content}
     * @should derive the css class from the semantic type
     * @should wrap struck through words in a strikethrough span
     * @should combine both classes on a single span
     * @should append the configured style class after the derived class
     * @should apply style class only mappings without a type
     * @should not duplicate a class mapped by both type and style class
     * @should leave untagged words unchanged
     */
    @Override
    public String enrich(String content, LineElement element) {
        List<String> classes = new ArrayList<>();
        for (Tag tag : element.getTags()) {
            if (tag.getTagType() != TagType.LAYOUT_TAG) {
                continue;
            }
            String type = labelToType.get(tag.getLabel());
            if (type != null && !classes.contains(TYPE_CLASS_PREFIX + type)) {
                classes.add(TYPE_CLASS_PREFIX + type);
            }
            String styleClass = labelToStyleClass.get(tag.getLabel());
            if (styleClass != null && !classes.contains(styleClass)) {
                classes.add(styleClass);
            }
        }
        if (classes.isEmpty()) {
            return content;
        }
        return "<span class=\"alto-tag " + AltoHtmlBuilder.escapeAttribute(String.join(" ", classes)) + "\">" + content + "</span>";
    }

}
