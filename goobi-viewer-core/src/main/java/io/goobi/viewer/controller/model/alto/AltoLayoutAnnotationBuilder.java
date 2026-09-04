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

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.JDOMException;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.intranda.digiverso.ocr.alto.margins.MarginColumnDetector;
import de.intranda.digiverso.ocr.alto.model.structureclasses.Page;
import de.intranda.digiverso.ocr.alto.model.structureclasses.blocks.GraphicalElement;
import de.intranda.digiverso.ocr.alto.model.structureclasses.blocks.TextBlock;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoTags.TagType;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.Tag;
import de.intranda.digiverso.ocr.alto.model.superclasses.GeometricData;
import de.intranda.digiverso.ocr.alto.utils.AltoCoords;
import de.intranda.digiverso.ocr.alto.utils.HyphenationLinker;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.model.alto.AltoHtmlBuilder.AnnotationStyle;

/**
 * Extracts the layout-tag annotations of an ALTO page into a compact, sparse structure for JSON delivery: the
 * printed-text column, author-attributed text blocks with their margin classification, words carrying layout tags
 * (underline/strikethrough) and drawn lines ({@code GraphicalElement}s).
 *
 * <p>
 * Only elements that actually carry relevant information are listed; pages without any layout annotations yield empty
 * lists. All rects use the same coordinate space as the page text annotation endpoint (raw ALTO units via
 * {@link GeometricData#getBounds()}), so clients can join and overlay both data sets without conversion. Tag LABELs
 * are delivered raw alongside their configured presentation data — the semantic {@code types}
 * (underline/strikethrough/mark) per word/line and the {@code styleClass} per author block — so clients render
 * arbitrary customer vocabularies without hardcoded label knowledge. There are no built-in mappings (LayoutTag
 * LABELs are collection-specific); with empty mappings the result stays empty.
 * </p>
 */
public class AltoLayoutAnnotationBuilder {

    static final String MARGIN_LEFT = "left";
    static final String MARGIN_RIGHT = "right";

    private final Map<String, AnnotationStyle> annotationStyles;
    private final Map<String, String> tagTypes;
    private final Map<String, String> wordTagStyleClasses;
    private final MarginColumnDetector marginDetector;

    /** Applies no mappings at all (no anchor, no authors, no types) — the result is always empty. */
    public AltoLayoutAnnotationBuilder() {
        this(null, null, null, null);
    }

    /**
     * @param printedLabel the layout-tag LABEL identifying printed text (anchors margin detection); if blank, margin
     *            classification is disabled
     * @param annotationStyles mapping of handwriting author LABEL to {@link AnnotationStyle}; null is treated as
     *            empty (no author blocks) — the same source as the HTML path, so both stay in agreement
     * @param tagTypes mapping of word/line layout-tag LABEL to semantic type (underline/strikethrough/mark); null is
     *            treated as empty (no types delivered)
     * @param wordTagStyleClasses mapping of word/line layout-tag LABEL to its configured CSS class; null is treated as
     *            empty (no classes delivered)
     */
    public AltoLayoutAnnotationBuilder(String printedLabel, Map<String, AnnotationStyle> annotationStyles, Map<String, String> tagTypes,
            Map<String, String> wordTagStyleClasses) {
        this.annotationStyles = (annotationStyles == null) ? Collections.emptyMap() : annotationStyles;
        this.tagTypes = (tagTypes == null) ? Collections.emptyMap() : tagTypes;
        this.wordTagStyleClasses = (wordTagStyleClasses == null) ? Collections.emptyMap() : wordTagStyleClasses;
        this.marginDetector = new MarginColumnDetector(printedLabel, MarginColumnDetector.DEFAULT_TOLERANCE);
    }

    /**
     * Goobi ALTO files hold one page per file, so only the document's first page is read (unlike the HTML path,
     * which concatenates all pages).
     *
     * @param altoString the ALTO XML document
     * @param encoding the character encoding of the document
     * @return the layout annotations of the document's first page
     * @throws IOException if the document cannot be read
     * @throws JDOMException if the document cannot be parsed
     * @should derive the print column from the printed content
     * @should classify handwritten side notes as left margin
     * @should keep footnote callouts inline and skip printed blocks
     * @should list words carrying layout tags with their bounds
     * @should list drawn lines with their tags
     * @should use the configured annotation styles as author authority
     * @should deliver semantic types and style classes for configured mappings
     * @should return an empty result without configured mappings
     */
    public LayoutAnnotations build(String altoString, String encoding) throws IOException, JDOMException {
        AltoDocument doc = AltoDocument.getDocumentFromString(altoString,
                StringUtils.isBlank(encoding) ? StringTools.DEFAULT_ENCODING : encoding);
        // Same document preparation as the HTML path: block texts must not diverge
        // between the fulltext view and the JSON margin cards.
        new HyphenationLinker().linkWords(doc);
        Page page = doc.getFirstPage();
        LayoutAnnotations result = new LayoutAnnotations();
        if (page == null) {
            return result;
        }

        AltoCoords column = marginDetector.getPrintColumn(page);
        result.printColumn = area(column != null ? column.getBounds() : null);

        for (TextBlock block : page.getAllTextBlocksAsList()) {
            String author = AltoHtmlBuilder.handwritingLabel(block, annotationStyles);
            if (author == null) {
                continue;
            }
            AltoCoords box = marginDetector.getContentBox(block);
            if (box == null) {
                continue;
            }
            String margin = null;
            if (marginDetector.isLeftMargin(block, column)) {
                margin = MARGIN_LEFT;
            } else if (marginDetector.isRightMargin(block, column)) {
                margin = MARGIN_RIGHT;
            }
            AnnotationStyle style = annotationStyles.get(author);
            result.blocks.add(new BlockAnnotation(block.getId(), area(box.getBounds()), author,
                    style != null ? style.name() : null, style != null ? style.styleClass() : null, margin, block.getContent()));
        }

        for (GeometricData<?, ?> word : page.getAllWordsAsList()) {
            addTaggedElement(result.words, word);
        }
        for (GraphicalElement line : page.getAllGraphicalElementsAsList()) {
            addTaggedElement(result.lines, line);
        }
        return result;
    }

    private void addTaggedElement(List<TaggedElement> target, GeometricData<?, ?> element) {
        List<String> labels = layoutTagLabels(element);
        if (!labels.isEmpty()) {
            List<String> types = new ArrayList<>();
            List<String> styleClasses = new ArrayList<>();
            for (String label : labels) {
                String type = tagTypes.get(label);
                if (type != null && !types.contains(type)) {
                    types.add(type);
                }
                String styleClass = wordTagStyleClasses.get(label);
                if (StringUtils.isNotBlank(styleClass) && !styleClasses.contains(styleClass)) {
                    styleClasses.add(styleClass);
                }
            }
            target.add(new TaggedElement(element.getId(), area(element.getBounds()), labels, types, styleClasses));
        }
    }

    private static List<String> layoutTagLabels(GeometricData<?, ?> element) {
        List<String> labels = new ArrayList<>();
        for (Tag tag : element.getTags()) {
            if (tag.getTagType() == TagType.LAYOUT_TAG && StringUtils.isNotBlank(tag.getLabel()) && !labels.contains(tag.getLabel())) {
                labels.add(tag.getLabel());
            }
        }
        return labels;
    }

    private static Area area(Rectangle rect) {
        return rect != null ? new Area(rect.x, rect.y, rect.width, rect.height) : null;
    }

    /** Sparse layout annotations of one ALTO page. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LayoutAnnotations {

        private Area printColumn;
        private final List<BlockAnnotation> blocks = new ArrayList<>();
        private final List<TaggedElement> words = new ArrayList<>();
        private final List<TaggedElement> lines = new ArrayList<>();

        public Area getPrintColumn() {
            return printColumn;
        }

        public List<BlockAnnotation> getBlocks() {
            return blocks;
        }

        public List<TaggedElement> getWords() {
            return words;
        }

        public List<TaggedElement> getLines() {
            return lines;
        }
    }

    /** A text block carrying a handwriting author tag, with its margin classification. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BlockAnnotation {

        private final String id;
        private final Area rect;
        private final String author;
        private final String authorName;
        private final String styleClass;
        private final String margin;
        private final String text;

        BlockAnnotation(String id, Area rect, String author, String authorName, String styleClass, String margin, String text) {
            this.id = id;
            this.rect = rect;
            this.author = author;
            this.authorName = authorName;
            this.styleClass = styleClass;
            this.margin = margin;
            this.text = text;
        }

        public String getId() {
            return id;
        }

        public Area getRect() {
            return rect;
        }

        public String getAuthor() {
            return author;
        }

        /** The configured display name of the author, or null when the mapping provides none. */
        public String getAuthorName() {
            return authorName;
        }

        /** The configured CSS style class of the author — the client's presentation hook for arbitrary vocabularies. */
        public String getStyleClass() {
            return styleClass;
        }

        /** The block's text content — margin notes render it in the side column. */
        public String getText() {
            return text;
        }

        public String getMargin() {
            return margin;
        }
    }

    /**
     * A word or drawn line carrying at least one layout tag. {@code rect} may be null for elements without
     * coordinates (omitted from the JSON via {@code NON_NULL}) — clients must tolerate a missing rect.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TaggedElement {

        private final String id;
        private final Area rect;
        private final List<String> tags;
        private final List<String> types;
        private final List<String> styleClasses;

        TaggedElement(String id, Area rect, List<String> tags, List<String> types, List<String> styleClasses) {
            this.id = id;
            this.rect = rect;
            this.tags = Collections.unmodifiableList(tags);
            this.types = Collections.unmodifiableList(types);
            this.styleClasses = Collections.unmodifiableList(styleClasses);
        }

        public String getId() {
            return id;
        }

        public Area getRect() {
            return rect;
        }

        public List<String> getTags() {
            return tags;
        }

        /** The semantic types the element's tags map to (underline/strikethrough/mark) — the client renders these. */
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public List<String> getTypes() {
            return types;
        }

        /** The configured CSS classes of the element's word tags — the theme colors these. */
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public List<String> getStyleClasses() {
            return styleClasses;
        }
    }

    /** A rectangle in raw ALTO coordinate units. */
    public static class Area {

        private final int x;
        private final int y;
        private final int w;
        private final int h;

        Area(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getW() {
            return w;
        }

        public int getH() {
            return h;
        }
    }
}
