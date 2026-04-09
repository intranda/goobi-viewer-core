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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.JDOMException;

import de.intranda.digiverso.ocr.alto.model.structureclasses.Line;
import de.intranda.digiverso.ocr.alto.model.structureclasses.Page;
import de.intranda.digiverso.ocr.alto.model.structureclasses.blocks.TextBlock;
import de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.LineElement;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;
import de.intranda.digiverso.ocr.alto.utils.HyphenationLinker;
import io.goobi.viewer.controller.StringTools;

/**
 * Parses an ALTO XML document and extracts its plain text content, preserving the structural
 * hierarchy of pages, text blocks, lines, and words. Optionally applies a chain of
 * {@link TextEnricher} instances to augment individual word tokens (e.g. with named-entity markup).
 */
public class AltoTextReader {

    private static final CharSequence PAGE_SEPARATOR = "\n\n";
    private static final CharSequence BLOCK_SEPARATOR = "\n\n";
    private static final CharSequence LINE_SEPARATOR = "\n";
    private static final CharSequence WORD_SEPARATOR = " ";

    private final AltoDocument alto;
    private final List<TextEnricher> textEnricher;

    public AltoTextReader(String altoString, String encoding, TextEnricher... textEnricher) throws IOException, JDOMException {
        if (altoString == null) {
            throw new IllegalArgumentException("altoDoc may not be null");
        }
        this.alto = loadAlto(altoString, encoding);
        this.textEnricher = Arrays.asList(textEnricher);
    }

    public String extractText() {
        return alto.getAllPagesAsList().stream().map(this::extractText).collect(Collectors.joining(PAGE_SEPARATOR));
    }

    String extractText(Page page) {
        return page.getAllTextBlocksAsList().stream().map(this::extractText).collect(Collectors.joining(BLOCK_SEPARATOR));
    }

    String extractText(TextBlock block) {
        return block.getChildren().stream().map(this::extractText).collect(Collectors.joining(LINE_SEPARATOR));
    }

    String extractText(Line line) {
        return line.getChildren().stream().map(this::extractText).collect(Collectors.joining(WORD_SEPARATOR));
    }

    String extractText(LineElement word) {
        String content = word.getContentHyphenated();
        for (TextEnricher enricher : textEnricher) {
            content = enricher.enrich(content, word);
        }
        return content;
    }

    private static AltoDocument loadAlto(String altoString, String charset) throws IOException, JDOMException {
        AltoDocument doc = AltoDocument.getDocumentFromString(altoString, StringUtils.isBlank(charset) ? StringTools.DEFAULT_ENCODING : charset);
        HyphenationLinker linker = new HyphenationLinker();
        linker.linkWords(doc);
        return doc;
    }

}
