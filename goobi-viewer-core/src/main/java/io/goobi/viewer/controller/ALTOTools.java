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
package io.goobi.viewer.controller;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;

import de.intranda.digiverso.normdataimporter.Utils;
import de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.Word;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.Tag;
import de.intranda.digiverso.ocr.alto.model.superclasses.GeometricData;
import io.goobi.viewer.api.rest.model.ner.ElementReference;
import io.goobi.viewer.api.rest.model.ner.NERTag;
import io.goobi.viewer.api.rest.model.ner.NERTag.Type;
import io.goobi.viewer.api.rest.model.ner.TagCount;
import io.goobi.viewer.controller.model.alto.AltoTextReader;
import io.goobi.viewer.controller.model.alto.CoordinateFinder;
import io.goobi.viewer.controller.model.alto.NamedEntityEnricher;
import io.goobi.viewer.controller.model.alto.TextEnricher;
import io.goobi.viewer.model.search.FuzzySearchTerm;

/**
 * Utility class for parsing and processing ALTO (Analyzed Layout and Text Object) XML documents.
 */
public final class ALTOTools {

    private static final Logger logger = LogManager.getLogger(ALTOTools.class);

    /** Constant <code>TAG_LABEL_IGNORE_REGEX</code>. */
    public static final String TAG_LABEL_IGNORE_REGEX =
            "(^[^a-zA-ZÄäÁáÀàÂâÖöÓóÒòÔôÜüÚúÙùÛûëÉéÈèÊêßñ]+)|([^a-zA-ZÄäÁáÀàÂâÖöÓóÒòÔôÜüÚúÙùÛûëÉéÈèÊêßñ]+$)";
    /** Characters that can cause an "Invalid UTF-8 middle byte" error in the parser. */
    public static final String ALTO_PROBLEMATIC_CHARS = "[ﬅﬆﬃﬄﬂﬁ�]";
    // Pre-compiled patterns to avoid per-call Pattern.compile() in hot paths (called for every ALTO word/tag)
    private static final Pattern TAG_LABEL_IGNORE_COMPILED = Pattern.compile(TAG_LABEL_IGNORE_REGEX);
    private static final Pattern ALTO_PROBLEMATIC_CHARS_COMPILED = Pattern.compile(ALTO_PROBLEMATIC_CHARS);

    /**
     * Private constructor.
     */
    private ALTOTools() {
        //
    }

    /**
     * Reads the plain full-text from an alto file. Don't merge line breaks.
     *
     * @param path path to the ALTO file
     * @param encoding character encoding to use when reading the file
     * @return {@link String} containing plain text from ALTO at the given path
     * @throws IOException
     */
    public static String getFulltext(Path path, String encoding) throws IOException {
        String altoString = FileTools.getStringFromFile(path.toFile(), encoding);
        return getFulltext(altoString, encoding, false);
    }

    /**
     * getFullText.
     *
     * @param alto ALTO XML document as a string
     * @param charset character encoding of the ALTO document
     * @param mergeLineBreakWords merge words split across line breaks into one
     * @return the plain text extracted from the ALTO XML document, or null on error
     * @should extract fulltext correctly
     */
    public static String getFulltext(String alto, String charset, boolean mergeLineBreakWords) {
        try {
            return alto2Txt(alto, charset, mergeLineBreakWords);
        } catch (IOException | JDOMException e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * getNERTags.
     *
     * @param alto ALTO XML document as a string
     * @param inCharset character encoding of the ALTO document
     * @param type NER tag type to filter by; null returns all types
     * @return a list of NER TagCount objects extracted from the given ALTO document
     */
    public static List<TagCount> getNERTags(String alto, final String inCharset, NERTag.Type type) {
        String charset = inCharset;
        // Make sure an empty charset value is changed to null to avoid exceptions
        if (StringUtils.isBlank(charset)) {
            charset = null;
        }
        List<TagCount> ret = new ArrayList<>();
        try {
            AltoDocument doc = AltoDocument.getDocumentFromString(alto, charset);
            for (Tag tag : doc.getTags().getTagsAsList()) {
                if (type == null || type.matches(tag.getType())) {
                    addTags(createNERTag(tag), ret);
                }
            }
        } catch (UnsupportedEncodingException e) {
            logger.error("{}: {}", e.getMessage(), charset);
        } catch (JDOMException | IOException e) {
            logger.error(e.getMessage(), e);
        }

        return ret;
    }

    /**
     * @param tags tags to add or merge into the list
     * @param list target list to add tags to
     */
    private static void addTags(List<TagCount> tags, List<TagCount> list) {
        for (TagCount tag : tags) {
            int index = list.indexOf(tag);
            if (index > -1) {
                list.get(index).addReferences(tag.getReferences());
            } else {
                list.add(tag);
            }
        }
    }

    /**
     * @param tag ALTO named-entity tag to convert into TagCount entries
     * @return List<TagCount>
     * @should add identifier to TagCount
     */
    @SuppressWarnings("rawtypes")
    static List<TagCount> createNERTag(Tag tag) {
        String value = tag.getLabel();
        // Use pre-compiled pattern to avoid per-call Pattern.compile() in this hot path (called for every ALTO NER tag)
        value =
                TAG_LABEL_IGNORE_COMPILED.matcher(value).replaceAll(""); //NOSONAR TAG_LABEL_IGNORE_REGEX contains no lazy internal repetitions
        Type type = Type.getByLabel(tag.getType());
        if (type == null) {
            logger.trace("Unknown tag type: {}, using {}", tag.getType(), Type.MISC.name());
            type = Type.MISC;
        }
        ElementReference element = null;

        List<TagCount> ret = new ArrayList<>(tag.getReferences().size());
        for (GeometricData reference : tag.getReferences()) {
            String elementId = reference.getId();
            Rectangle elementCoordinates = reference.getRect().getBounds();
            String elementContent = reference.getContent();
            element = new ElementReference(elementId, elementCoordinates, elementContent, tag.getUri());
            TagCount nerTag = new TagCount(value, type, element);
            if (tag.getUri() != null) {
                // Parse authority identifier from URI, if available
                String identifier = Utils.getIdentifierFromURI(tag.getUri());
                if (StringUtils.isNotEmpty(identifier)) {
                    nerTag.setIdentifier(identifier);
                }
            }
            ret.add(nerTag);
        }

        return ret;
    }

    /**
     * alto2Txt.
     *
     * @param alto ALTO XML document as a string
     * @param charset character encoding of the ALTO document
     * @param mergeLineBreakWords merge words split across line breaks into one
     * @return the plain text extracted from the ALTO XML document
     * @throws java.io.IOException if any.
     * @throws javax.xml.stream.XMLStreamException if any.
     * @throws JDOMException
     * @should extract fulltext correctly
     * @should concatenate word at line break correctly
     * @should add uris correctly
     */
    protected static String alto2Txt(String alto, String charset, boolean mergeLineBreakWords)
            throws IOException, JDOMException {
        if (alto == null) {
            throw new IllegalArgumentException("alto may not be null");
        }

        // Use pre-compiled pattern to avoid per-call Pattern.compile() in this hot path (called for every ALTO word)
        TextEnricher charCleanupEnricher = (string, element) -> ALTO_PROBLEMATIC_CHARS_COMPILED.matcher(string).replaceAll(" ");
        TextEnricher nerEnricher = new NamedEntityEnricher();
        AltoTextReader reader =
                new AltoTextReader(alto, charset != null ? charset : StringTools.DEFAULT_ENCODING, charCleanupEnricher, nerEnricher);

        return reader.extractText();
    }

    public static XMLStreamReader createXmlParser(InputStream is) throws FactoryConfigurationError, XMLStreamException {
        XMLStreamReader parser;
        XMLInputFactory factory = XMLInputFactory.newInstance();
        // Disable access to external entities
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        parser = factory.createXMLStreamReader(is);
        return parser;
    }

    /**
     * getWordCoords.
     *
     * @param altoString ALTO XML document as a string
     * @param charset character encoding of the ALTO document
     * @param searchTerms set of terms whose coordinates to locate
     * @return a list of coordinate strings for words matching any of the given search terms in the ALTO document
     */
    public static List<String> getWordCoords(String altoString, String charset, Set<String> searchTerms) {
        return getWordCoords(altoString, charset, searchTerms, 0);
    }

    /**
     * getRotatedCoordinates.
     *
     * @param inCoords comma-separated coordinate string to rotate
     * @param rotation rotation angle in degrees (0, 90, 180, 270)
     * @param pageSize dimensions of the page image
     * @return the rotated bounding box as a comma-separated coordinate string
     */
    public static String getRotatedCoordinates(final String inCoords, int rotation, Dimension pageSize) {
        String coords = inCoords;
        if (rotation != 0) {
            try {
                Rectangle wordRect = getRectangle(coords);
                wordRect = rotate(wordRect, rotation, pageSize);
                coords = getString(wordRect);
            } catch (NumberFormatException e) {
                logger.error("Cannot rotate coords {}: {}", coords, e.getMessage());
            }
        }
        return coords;
    }

    /**
     *
     * @param altoString String containing the ALTO XML document
     * @param charset character encoding of the ALTO document
     * @param searchTerms Set of search terms
     * @param rotation Image rotation in degrees
     * @return a list of coordinate strings for words matching any of the given search terms, rotated to match the image orientation
     * @should match hyphenated words
     * @should match phrases
     * @should match diacritics via base letter
     */
    public static List<String> getWordCoords(String altoString, String charset, Set<String> searchTerms, int rotation) {
        return getWordCoords(altoString, charset, searchTerms, 0, rotation);
    }

    public static List<String> getWordCoords(String altoString, String charset, Set<String> searchTerms, int proximitySearchDistance, int rotation) {
        try {
            return new CoordinateFinder(altoString, charset).getWordCoords(searchTerms, proximitySearchDistance, rotation);
            //            return new WordCoordinateService(altoString, charset).getWordCoords(searchTerms, proximitySearchDistance, rotation);
        } catch (IOException | JDOMException e) {
            logger.error("Could not parse alto: {}", e.toString());
            return Collections.emptyList();
        }
    }

    /**
     * @param rect rectangle whose coordinates to format
     * @return ALTO word coordinates as a {@link String}
     */
    private static String getString(Rectangle rect) {
        StringBuilder sb = new StringBuilder();
        sb.append(rect.x).append(",").append(rect.y).append(",").append(rect.x + rect.width).append(",").append(rect.y + rect.height);
        return sb.toString();
    }

    /**
     * rotate.
     *
     * @param rect rectangle to rotate around the image center
     * @param rotation rotation angle in degrees (90, 180, 270)
     * @param imageSize dimensions of the image for computing the rotation
     * @return the rotated bounding rectangle
     */
    protected static Rectangle rotate(Rectangle rect, int rotation, Dimension imageSize) {

        double x1 = rect.getMinX();
        double y1 = rect.getMinY();
        double x2 = rect.getMaxX();
        double y2 = rect.getMaxY();

        double w = imageSize.getWidth();
        double h = imageSize.getHeight();

        double x1r = x1;
        double y1r = y1;
        double x2r = x2;
        double y2r = y2;

        switch (rotation) {
            case 270:
                x1r = y1;
                y1r = w - x2;
                x2r = y2;
                y2r = w - x1;
                break;
            case 90:
                x1r = h - y2;
                y1r = x1;
                x2r = h - y1;
                y2r = x2;
                break;
            case 180:
                x1r = w - x2;
                y1r = h - y2;
                x2r = w - x1;
                y2r = h - y1;
                break;
            default:
                // coordinates unchanged
                break;
        }

        return new Rectangle((int) x1r, (int) y1r, (int) (x2r - x1r), (int) (y2r - y1r));

    }

    /**
     * Reads rectangle coordinates from the given String. The String-coordinates are assumed to be int coordinates for left, top, right, bottom in
     * that order
     *
     * @param string comma-separated left, top, right, bottom coordinate string
     * @return {@link Rectangle} from the given coordinates string
     */
    private static Rectangle getRectangle(String string) throws NumberFormatException {
        String[] parts = string.split(",\\s*");
        if (parts.length != 4) {
            throw new NumberFormatException("Rectangle coordinates must consist of four integer numbers");
        }
        int left = (int) Double.parseDouble(parts[0]);
        int top = (int) Double.parseDouble(parts[1]);
        int right = (int) Double.parseDouble(parts[2]);
        int bottom = (int) Double.parseDouble(parts[3]);

        return new Rectangle(left, top, right - left, bottom - top);
    }

    /**
     * getMatchALTOWord.
     *
     * @param eleWord ALTO word element whose content to match against
     * @param words array of search words to match against the element
     * @return 1 if there is a match; 0 otherwise
     */
    public static int getMatchALTOWord(Word eleWord, String[] words) {
        if (eleWord == null) {
            throw new IllegalArgumentException("eleWord may not be null");
        }
        if (words == null || words.length == 0) {
            return 0;
        }

        // Word exists and has content
        String content = eleWord.getContent();
        // Normalize (remove diacritical marks)
        content = StringTools.removeDiacriticalMarks(content);
        // Clean up leading non-alphanumeric characters so that matching works
        while (content.length() > 0 && !StringUtils.isAlphanumeric(content.substring(0, 1))) {
            content = content.substring(1);
        }
        // replace content with complete content of hyphenated word if applicable
        if (content.matches("\\S+")) {
            String subsContent = eleWord.getSubsContent();
            if (subsContent != null && !subsContent.isEmpty()) {
                content = subsContent;
            }
        }

        String[] contents = content.trim().split("\\s+");
        int hits = 0;
        for (String altoWord : contents) {
            for (String searchWord : words) {
                FuzzySearchTerm fuzzy = new FuzzySearchTerm(searchWord);
                if (fuzzy.matches(altoWord)) {
                    hits++;
                }
            }
        }
        return hits;

    }

    /**
     * getALTOCoords.
     *
     * @param element ALTO geometric element whose bounding box to extract
     * @return the bounding box of the given element as a comma-separated coordinate string (x, y, width, height)
     */
    public static String getALTOCoords(GeometricData element) {
        if (element == null) {
            throw new IllegalArgumentException("eleWord may not be null");
        }
        String coords = null;
        try {
            int hpos = (int) element.getRect().x;
            int vpos = (int) element.getRect().y;
            int height = (int) element.getRect().height;
            int width = (int) element.getRect().width;
            coords = new StringBuilder().append(hpos)
                    .append(",")
                    .append(vpos)
                    .append(",")
                    .append(hpos + width)
                    .append(",")
                    .append(vpos + height)
                    .toString();
        } catch (NumberFormatException e) {
            logger.error(e.getMessage());
        }

        return coords;
    }
}
