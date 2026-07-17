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
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.LineElement;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoTags.TagType;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.Tag;

/**
 * {@link TextEnricher} implementation that applies inline visual markup to ALTO word tokens carrying word-level
 * layout tags. Underlined words ({@code gr_unterstrichen}) and struck-through words ({@code gr_durchgestrichen}) are
 * wrapped in a {@code <span>} whose CSS classes drive the rendering; a word carrying both tags gets both classes on a
 * single span.
 */
public class LayoutTagEnricher implements TextEnricher {

    static final String LABEL_UNDERLINE = "gr_unterstrichen";
    static final String LABEL_STRIKETHROUGH = "gr_durchgestrichen";

    static final String CLASS_UNDERLINE = "alto-tag--underline";
    static final String CLASS_STRIKETHROUGH = "alto-tag--strikethrough";

    /**
     * Wraps the word content in a styled span if the word references any supported word-level layout tag.
     *
     * @param content the textual content of the word being enriched (possibly already wrapped by a previous enricher)
     * @param element the ALTO line element whose word content is being processed
     * @return the span-wrapped content if a supported layout tag applies, otherwise the unchanged {@code content}
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
            String label = tag.getLabel();
            if (LABEL_UNDERLINE.equals(label) && !classes.contains(CLASS_UNDERLINE)) {
                classes.add(CLASS_UNDERLINE);
            } else if (LABEL_STRIKETHROUGH.equals(label) && !classes.contains(CLASS_STRIKETHROUGH)) {
                classes.add(CLASS_STRIKETHROUGH);
            }
        }
        if (classes.isEmpty()) {
            return content;
        }
        return "<span class=\"alto-tag " + StringUtils.join(classes, " ") + "\">" + content + "</span>";
    }

}
