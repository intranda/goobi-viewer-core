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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.JDOMException;

import de.intranda.digiverso.ocr.alto.margins.MarginColumnDetector;
import de.intranda.digiverso.ocr.alto.model.structureclasses.Line;
import de.intranda.digiverso.ocr.alto.model.structureclasses.Page;
import de.intranda.digiverso.ocr.alto.model.structureclasses.blocks.TextBlock;
import de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.LineElement;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;
import de.intranda.digiverso.ocr.alto.utils.AltoCoords;
import de.intranda.digiverso.ocr.alto.utils.HyphenationLinker;
import io.goobi.viewer.controller.StringTools;

/**
 * Builds a structured HTML representation of an ALTO document for display in the fulltext view. In addition to the
 * word-level enrichment performed by the supplied {@link TextEnricher}s (named-entity buttons, underline/strikethrough
 * markup), it separates handwritten margin annotations from the printed-text column and renders them in dedicated
 * side columns, and it wraps handwriting-attributed blocks so their author can be styled distinctly.
 *
 * <p>
 * A block is treated as a margin annotation when it both lies horizontally outside the printed-text column
 * ({@link MarginColumnDetector}) and carries a handwriting author tag. Blocks that carry a handwriting tag but sit
 * inside the column (e.g. an inline title annotation or a footnote callout in the gutter) stay in the main flow but
 * keep their author styling.
 * </p>
 */
public class AltoHtmlBuilder {

    /** Layout-tag LABELs that attribute content to a handwriting author (i.e. anything but printed text). */
    static final Map<String, String> HANDWRITING_LABEL_TO_MODIFIER = buildHandwritingLabelMap();

    /** Readable, locale-independent display names for the handwriting authors (used as the annotation title). */
    static final Map<String, String> HANDWRITING_LABEL_TO_NAME = buildHandwritingNameMap();

    private final List<TextEnricher> wordEnrichers;
    private final MarginColumnDetector marginDetector = new MarginColumnDetector();

    public AltoHtmlBuilder(TextEnricher... wordEnrichers) {
        this.wordEnrichers = Arrays.asList(wordEnrichers);
    }

    /**
     * @param altoString the ALTO XML document
     * @param encoding the character encoding of the document
     * @return a structured HTML string representing the document's text with layout markup
     * @throws IOException if the document cannot be read
     * @throws JDOMException if the document cannot be parsed
     * @should separate handwritten margin annotations from the main text
     * @should keep footnote callouts in the main text
     * @should mark up underlined and struck through words
     */
    public String build(String altoString, String encoding) throws IOException, JDOMException {
        AltoDocument doc = loadAlto(altoString, encoding);
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"alto-fulltext\">");
        for (Page page : doc.getAllPagesAsList()) {
            appendPage(sb, page);
        }
        sb.append("</div>");
        return sb.toString();
    }

    private void appendPage(StringBuilder sb, Page page) {
        AltoCoords column = marginDetector.getPrintColumn(page);

        List<TextBlock> leftMargin = new ArrayList<>();
        List<TextBlock> mainBlocks = new ArrayList<>();
        List<TextBlock> rightMargin = new ArrayList<>();

        for (TextBlock block : page.getAllTextBlocksAsList()) {
            boolean handwriting = handwritingModifier(block) != null;
            if (handwriting && marginDetector.isLeftMargin(block, column)) {
                leftMargin.add(block);
            } else if (handwriting && marginDetector.isRightMargin(block, column)) {
                rightMargin.add(block);
            } else {
                mainBlocks.add(block);
            }
        }

        sb.append("<div class=\"alto-fulltext__page\">");
        appendMargin(sb, "alto-fulltext__margin--left", leftMargin);
        sb.append("<div class=\"alto-fulltext__main\">");
        for (TextBlock block : mainBlocks) {
            appendBlock(sb, block);
        }
        sb.append("</div>");
        appendMargin(sb, "alto-fulltext__margin--right", rightMargin);
        sb.append("</div>");
    }

    private void appendMargin(StringBuilder sb, String modifier, List<TextBlock> blocks) {
        if (blocks.isEmpty()) {
            return;
        }
        sb.append("<aside class=\"alto-fulltext__margin ").append(modifier).append("\">");
        for (TextBlock block : blocks) {
            appendBlock(sb, block);
        }
        sb.append("</aside>");
    }

    private void appendBlock(StringBuilder sb, TextBlock block) {
        String modifier = handwritingModifier(block);
        sb.append("<p class=\"alto-block");
        if (modifier != null) {
            sb.append(" alto-annotation alto-annotation--").append(modifier);
        }
        sb.append('"');
        if (modifier != null) {
            String label = handwritingLabel(block);
            sb.append(" data-author=\"").append(escapeAttribute(label)).append('"');
            String name = HANDWRITING_LABEL_TO_NAME.get(label);
            if (name != null) {
                sb.append(" title=\"").append(escapeAttribute(name)).append('"');
            }
        }
        sb.append('>');

        List<Line> lines = block.getChildren();
        for (int i = 0; i < lines.size(); i++) {
            appendLine(sb, lines.get(i));
            if (i < lines.size() - 1) {
                sb.append("<br/>");
            }
        }
        sb.append("</p>");
    }

    private void appendLine(StringBuilder sb, Line line) {
        boolean first = true;
        for (LineElement element : line.getChildren()) {
            String raw = element.getContentHyphenated();
            if (StringUtils.isEmpty(raw)) {
                continue;
            }
            String content = escapeText(raw);
            for (TextEnricher enricher : wordEnrichers) {
                content = enricher.enrich(content, element);
            }
            if (!first) {
                sb.append(' ');
            }
            sb.append(content);
            first = false;
        }
    }

    /**
     * @return the handwriting author LABEL of the block, or null if the block is printed text / untagged
     */
    private String handwritingLabel(TextBlock block) {
        for (String label : block.getTagLabels()) {
            if (HANDWRITING_LABEL_TO_MODIFIER.containsKey(label)) {
                return label;
            }
        }
        return null;
    }

    /**
     * @return the CSS modifier for the block's handwriting author, or null if the block is printed text / untagged
     */
    private String handwritingModifier(TextBlock block) {
        String label = handwritingLabel(block);
        return label != null ? HANDWRITING_LABEL_TO_MODIFIER.get(label) : null;
    }

    private static AltoDocument loadAlto(String altoString, String charset) throws IOException, JDOMException {
        AltoDocument doc = AltoDocument.getDocumentFromString(altoString, StringUtils.isBlank(charset) ? StringTools.DEFAULT_ENCODING : charset);
        new HyphenationLinker().linkWords(doc);
        return doc;
    }

    private static Map<String, String> buildHandwritingLabelMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("gr_jacob_grimm", "jacob-grimm");
        map.put("gr_wilhelm_grimm", "wilhelm-grimm");
        map.put("gr_jacob_oder_wilhelm_grimm", "jacob-oder-wilhelm-grimm");
        map.put("gr_andere_handschrift", "andere-handschrift");
        map.put("gr_einweisungszeichen", "einweisungszeichen");
        map.put("gr_korrekturzeichen", "korrekturzeichen");
        return map;
    }

    private static Map<String, String> buildHandwritingNameMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("gr_jacob_grimm", "Jacob Grimm");
        map.put("gr_wilhelm_grimm", "Wilhelm Grimm");
        map.put("gr_jacob_oder_wilhelm_grimm", "Jacob oder Wilhelm Grimm");
        map.put("gr_andere_handschrift", "Andere Handschrift");
        map.put("gr_einweisungszeichen", "Einweisungszeichen");
        map.put("gr_korrekturzeichen", "Korrekturzeichen");
        return map;
    }

    private static String escapeText(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String escapeAttribute(String s) {
        return escapeText(s).replace("\"", "&quot;");
    }

}
