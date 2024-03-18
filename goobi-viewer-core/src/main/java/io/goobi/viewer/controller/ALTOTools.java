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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.output.XMLOutputter;

import de.intranda.digiverso.ocr.alto.model.structureclasses.Line;
import de.intranda.digiverso.ocr.alto.model.structureclasses.Page;
import de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.Word;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.Tag;
import de.intranda.digiverso.ocr.alto.model.superclasses.GeometricData;
import de.intranda.digiverso.ocr.alto.utils.HyphenationLinker;
import io.goobi.viewer.api.rest.model.ner.ElementReference;
import io.goobi.viewer.api.rest.model.ner.NERTag;
import io.goobi.viewer.api.rest.model.ner.NERTag.Type;
import io.goobi.viewer.api.rest.model.ner.TagCount;
import io.goobi.viewer.model.search.FuzzySearchTerm;

/**
 * <p>
 * ALTOTools class.
 * </p>
 */
public final class ALTOTools {

    private static final Logger logger = LogManager.getLogger(ALTOTools.class);

    private static final String STRING = "String";
    private static final String CONTENT = "CONTENT";
    private static final String SUBS_CONTENT = "SUBS_CONTENT";
    private static final String ID = "ID";
    private static final String TEXTLINE = "TextLine";
    private static final String TAGREFS = "TAGREFS";
    private static final String NETAG = "NamedEntityTag";
    private static final String TYPE = "TYPE";
    private static final String URI = "URI";
    private static final String LABEL = "LABEL";

    /** Constant <code>TAG_LABEL_IGNORE_REGEX</code> */
    public static final String TAG_LABEL_IGNORE_REGEX =
            "(^[^a-zA-ZÄäÁáÀàÂâÖöÓóÒòÔôÜüÚúÙùÛûëÉéÈèÊêßñ]+)|([^a-zA-ZÄäÁáÀàÂâÖöÓóÒòÔôÜüÚúÙùÛûëÉéÈèÊêßñ]+$)";

    /**
     * Private constructor.
     */
    private ALTOTools() {
        //
    }

    /**
     * Read the plain full-text from an alto file. Don't merge line breaks.
     *
     * @param path
     * @param encoding
     * @return {@link String} containing plain text from ALTO at the given path
     * @throws IOException
     */
    public static String getFulltext(Path path, String encoding) throws IOException {
        String altoString = FileTools.getStringFromFile(path.toFile(), encoding);
        return getFulltext(altoString, encoding, false, null);
    }

    /**
     * <p>
     * getFullText.
     * </p>
     *
     * @param alto a {@link java.lang.String} object.
     * @param charset
     * @param mergeLineBreakWords a boolean.
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @return a {@link java.lang.String} object.
     * @should extract fulltext correctly
     */
    public static String getFulltext(String alto, String charset, boolean mergeLineBreakWords, HttpServletRequest request) {
        try {
            return alto2Txt(alto, charset, mergeLineBreakWords, request);
        } catch (IOException | XMLStreamException | JDOMException e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * <p>
     * getNERTags.
     * </p>
     *
     * @param alto a {@link java.lang.String} object.
     * @param inCharset
     * @param type a {@link io.goobi.viewer.servlets.rest.ner.NERTag.Type} object.
     * @return a {@link java.util.List} object.
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
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return ret;
    }

    /**
     * @param tags
     * @param list
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
     * @param tag
     * @return List<TagCount>
     */
    @SuppressWarnings("rawtypes")
    private static List<TagCount> createNERTag(Tag tag) {
        String value = tag.getLabel();
        value =
                value.replaceAll(TAG_LABEL_IGNORE_REGEX, ""); //NOSONAR TAG_LABEL_IGNORE_REGEX contains no lazy internal repetitions
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
            ret.add(nerTag);
        }

        return ret;
    }

    /**
     * <p>
     * alto2Txt.
     * </p>
     *
     * @param alto a {@link java.lang.String} object.
     * @param charset ALTO charset
     * @param mergeLineBreakWords a boolean.
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @return a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     * @throws javax.xml.stream.XMLStreamException if any.
     * @throws JDOMException
     * @should use extract fulltext correctly
     * @should concatenate word at line break correctly
     */
    protected static String alto2Txt(String alto, String charset, boolean mergeLineBreakWords, HttpServletRequest request)
            throws IOException, XMLStreamException, JDOMException {
        if (alto == null) {
            throw new IllegalArgumentException("alto may not be null");
        }

        String useAlto = alto;
        String useCharset = charset != null ? charset : StringTools.DEFAULT_ENCODING;

        // Link hyphenated words before parsing the document
        if (mergeLineBreakWords) {
            AltoDocument altoDoc = AltoDocument.getDocumentFromString(alto, charset);
            new HyphenationLinker().linkWords(altoDoc);
            Document doc = new Document(altoDoc.writeToDom());
            useAlto = new XMLOutputter().outputString(doc);
        }

        Map<String, String> neTypeMap = new HashMap<>();
        Map<String, String> neLabelMap = new HashMap<>();
        Map<String, String> neUriMap = new HashMap<>();
        Set<String> usedTags = new HashSet<>();
        StringBuilder strings = new StringBuilder(500);
        XMLStreamReader parser = null;
        try (InputStream is = new ByteArrayInputStream(useAlto.getBytes(useCharset))) {
            parser = createXmlParser(is);

            String prevSubsContent = null;
            while (parser.hasNext()) {
                switch (parser.getEventType()) {
                    case XMLStreamConstants.START_DOCUMENT:
                        break;

                    case XMLStreamConstants.END_DOCUMENT:
                        parser.close();
                        break;

                    case XMLStreamConstants.NAMESPACE:
                        break;

                    case XMLStreamConstants.START_ELEMENT:
                        if (STRING.equals(parser.getLocalName())) {
                            String content = null;
                            String subsContent = null;
                            String tagref = null;
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                switch (parser.getAttributeLocalName(i)) {
                                    case CONTENT:
                                        content = parser.getAttributeValue(i);
                                        break;
                                    case SUBS_CONTENT:
                                        if (mergeLineBreakWords) {
                                            subsContent = parser.getAttributeValue(i);
                                        }
                                        break;
                                    case TAGREFS:
                                        tagref = parser.getAttributeValue(i);
                                        break;
                                    default:
                                        break;
                                }
                            }
                            if (tagref != null && neTypeMap.get(tagref) != null) {
                                // NE tag
                                if (!usedTags.contains(tagref)) {
                                    String type = neTypeMap.get(tagref);
                                    strings.append("<span class=\"ner-");
                                    strings.append(type.toLowerCase());
                                    strings.append("\">");
                                    if (neUriMap.get(tagref) != null) {
                                        // with URI (skip if tag is already used so
                                        // that the tag link is not rendered for
                                        // every tagged word)
                                        if (request != null) {
                                            String contextPath = request.getContextPath();
                                            strings.append("<span data-remotecontent=\"");
                                            strings.append(contextPath);
                                            strings.append("/api?action=normdata&amp;url=");
                                            strings.append(neUriMap.get(tagref));
                                            strings.append("&amp;lang=de\" class=\"ner-trigger\" title=\"");
                                            strings.append(neLabelMap.get(tagref));
                                            strings.append("\" tabindex=\"-1\"><span class=\"ner-popover-pointer\"></span>");
                                        }
                                        switch (neTypeMap.get(tagref)) {
                                            case "person":
                                                strings.append("<i class=\"fa fa-user\" aria-hidden=\"true\"></i>");
                                                break;
                                            case "location":
                                                strings.append("<i class=\"fa fa-map-marker\" aria-hidden=\"true\"></i>");
                                                break;
                                            case "institution":
                                                strings.append("<i class=\"fa fa-home\" aria-hidden=\"true\"></i>");
                                                break;
                                            default:
                                                strings.append("<i></i>");
                                                break;
                                        }

                                        // Use the tag's label instead of individual
                                        // strings so that there's only one link per
                                        // tag
                                        strings.append(neLabelMap.get(tagref));
                                        strings.append("</span>");
                                    } else {
                                        // w/o URI

                                        // Use the tag's label instead of individual
                                        // strings so that there's only one link per
                                        // tag
                                        strings.append(neLabelMap.get(tagref));
                                    }
                                    strings.append(" </span>");
                                    usedTags.add(tagref);
                                }
                            } else {
                                // No NE tag
                                if (subsContent != null) {
                                    subsContent = StringTools.escapeHtmlLtGt(subsContent);
                                    // Add concatenated SUBS_CONTENT word, if found, but only once
                                    if (!subsContent.equals(prevSubsContent)) {
                                        strings.append(subsContent);
                                        prevSubsContent = subsContent;
                                    }
                                    subsContent = null;
                                } else {
                                    content = StringTools.escapeHtmlLtGt(content);
                                    strings.append(content);
                                }
                                strings.append(' ');
                            }
                        } else if (NETAG.equals(parser.getLocalName())) {
                            String id = null;
                            String type = null;
                            String label = null;
                            String uri = null;
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                switch (parser.getAttributeLocalName(i)) {
                                    case ID:
                                        id = parser.getAttributeValue(i);
                                        break;
                                    case TYPE:
                                        type = parser.getAttributeValue(i);
                                        break;
                                    case URI:
                                        uri = parser.getAttributeValue(i);
                                        break;
                                    case LABEL:
                                        label = parser.getAttributeValue(i);
                                        break;
                                    default:
                                        break;
                                }
                            }
                            neTypeMap.put(id, type);
                            neLabelMap.put(id, label);
                            neUriMap.put(id, uri);
                        } else if (TEXTLINE.equals(parser.getLocalName())) {
                            strings.append("\n");
                        }
                        break;

                    default:
                        break;

                }
                parser.next();
            }
        } catch (Exception e) {
            // Wrong charset can result in an exception being thrown by the underlying parser implementation
            logger.warn(e.getMessage());
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
        if (strings.length() > 0) {
            strings.deleteCharAt(strings.length() - 1);
        }

        return strings.toString();
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
     * <p>
     * getWordCoords.
     * </p>
     *
     * @param altoString a {@link java.lang.String} object.
     * @param charset
     * @param searchTerms a {@link java.util.Set} object.
     * @return a {@link java.util.List} object.
     */
    public static List<String> getWordCoords(String altoString, String charset, Set<String> searchTerms) {
        return getWordCoords(altoString, charset, searchTerms, 0);
    }

    /**
     * <p>
     * getRotatedCoordinates.
     * </p>
     *
     * @param inCoords a {@link java.lang.String} object.
     * @param rotation a int.
     * @param pageSize a {@link java.awt.Dimension} object.
     * @return a {@link java.lang.String} object.
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
     * @param charset
     * @param searchTerms Set of search terms
     * @param rotation Image rotation in degrees
     * @return a {@link java.util.List} object.
     * @should match hyphenated words
     * @should match phrases
     * @should match diacritics via base letter
     */
    public static List<String> getWordCoords(String altoString, String charset, Set<String> searchTerms, int rotation) {
        return getWordCoords(altoString, charset, searchTerms, 0, rotation);
    }

    public static List<String> getWordCoords(String altoString, String charset, Set<String> searchTerms, int proximitySearchDistance, int rotation) {
        if (altoString == null) {
            throw new IllegalArgumentException("altoDoc may not be null");
        }
        List<Word> words = new ArrayList<>();
        Dimension pageSize = new Dimension(0, 0);
        try {
            AltoDocument document = AltoDocument.getDocumentFromString(altoString, charset);
            HyphenationLinker linker = new HyphenationLinker();
            linker.linkWords(document);

            Page page = document.getFirstPage();
            List<Line> lines = page.getAllLinesAsList();
            for (Line line : lines) {
                words.addAll(line.getWords());
            }
            pageSize = new Dimension((int) page.getWidth(), (int) page.getHeight());
        } catch (NullPointerException e) {
            logger.error("Could not parse ALTO: No width or height specified in 'page' element.");
        } catch (NumberFormatException e) {
            logger.error("Could not parse ALTO: Could not parse page width or height.");
        } catch (IOException | JDOMException e) {
            logger.error("Could not parse ALTO: ", e);
        }
        logger.trace("{} ALTO words found for this page.", words.size());
        List<String> coordList = new ArrayList<>();
        for (String s : searchTerms) {
            String[] searchWords = s.split("\\s+");
            if (searchWords == null || searchWords.length == 0 || StringUtils.isBlank(searchWords[0])) {
                continue;
            }
            for (int wordIndex = 0; wordIndex < words.size(); wordIndex++) {
                List<String> tempList = new ArrayList<>();
                Word eleWord = words.get(wordIndex);
                int totalHits = ALTOTools.getMatchALTOWord(eleWord, searchWords);
                if (totalHits > 0) {
                    boolean match = true;
                    addWordCoords(rotation, pageSize, eleWord, tempList);
                    if (eleWord.getHyphenationPartNext() != null && eleWord.getHyphenationPartNext().getContent().matches("\\S+")) {
                        wordIndex++;
                        addWordCoords(rotation, pageSize, eleWord.getHyphenationPartNext(), tempList);
                    }
                    // Match next words if search term has more than one word
                    if (totalHits < searchWords.length) {
                        int remainingProximityReach = proximitySearchDistance;
                        while (totalHits < searchWords.length && words.size() > wordIndex + 1) {
                            wordIndex++;
                            Word nextWord = words.get(wordIndex);
                            int hits = ALTOTools.getMatchALTOWord(nextWord, Arrays.copyOfRange(searchWords, totalHits, searchWords.length));
                            if (hits == 0) {
                                if (remainingProximityReach < 1) {
                                    wordIndex--;
                                    match = false;
                                    break;
                                } else {
                                    remainingProximityReach--;
                                }
                            } else {
                                remainingProximityReach = proximitySearchDistance;
                            }
                            totalHits += hits;
                            addWordCoords(rotation, pageSize, nextWord, tempList);
                            if (nextWord.getHyphenationPartNext() != null) {
                                wordIndex++;
                                addWordCoords(rotation, pageSize, nextWord.getHyphenationPartNext(), tempList);
                            }
                        }
                    }
                    if (match) {
                        coordList.addAll(tempList);
                    }
                }
            }
        }

        return coordList;
    }

    /**
     * 
     * @param rotation
     * @param pageSize
     * @param eleWord
     * @param tempList
     * @return ALTO word coordinates as a {@link String}
     */
    private static String addWordCoords(int rotation, Dimension pageSize, Word eleWord, List<String> tempList) {
        String coords = ALTOTools.getALTOCoords(eleWord);
        if (coords != null && rotation != 0) {
            try {
                Rectangle wordRect = getRectangle(coords);
                wordRect = rotate(wordRect, rotation, pageSize);
                coords = getString(wordRect);
            } catch (NumberFormatException e) {
                logger.error("Cannot rotate coords {}: {}", coords, e.getMessage());
            }
        }
        if (coords != null) {
            tempList.add(coords);
            if (logger.isTraceEnabled()) {
                logger.trace("ALTO word found: {} ({})", eleWord.getAttributeValue(CONTENT), coords);
            }
        }

        return coords;
    }

    /**
     * @param rect
     * @return ALTO word coordinates as a {@link String}
     */
    private static String getString(Rectangle rect) {
        StringBuilder sb = new StringBuilder();
        sb.append(rect.x).append(",").append(rect.y).append(",").append(rect.x + rect.width).append(",").append(rect.y + rect.height);
        return sb.toString();
    }

    /**
     * <p>
     * rotate.
     * </p>
     *
     * @param rect a {@link java.awt.Rectangle} object.
     * @param rotation a int.
     * @param imageSize a {@link java.awt.Dimension} object.
     * @return a {@link java.awt.Rectangle} object.
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

        // if(rotation%360 == 0) {
        // return rect;
        // }
        // if(imageSize.height*imageSize.width == 0) {
        // logger.error("Cannot rotate in page with no extent");
        // return rect;
        // }
        //
        // Point center = new Point(imageSize.width/2, imageSize.height/2);
        //
        // double scale = imageSize.width/(double)imageSize.height;
        //
        // AffineTransform transform = new AffineTransform();
        // transform.translate(center.x, center.y);
        // transform.rotate(Math.toRadians(rotation));
        // transform.scale(scale, scale);
        // transform.translate(-center.x, -center.y);
        //
        // Path2D.Double path = new Path2D.Double();
        // path.append(rect, false);
        // path.transform(transform);
        // Rectangle rotatedRect = path.getBounds();
        //
        // return rotatedRect;
    }

    /**
     * Reads rectangle coordinates from the given String. The String-coordinates are assumed to be int coordinates for left, top, right, bottom in
     * that order
     *
     * @param string
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
     * <p>
     * getMatchALTOWord.
     * </p>
     *
     * @param eleWord a {@link de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.Word} object.
     * @param words an array of {@link java.lang.String} objects.
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
     * <p>
     * getALTOCoords.
     * </p>
     *
     * @param element a {@link de.intranda.digiverso.ocr.alto.model.superclasses.GeometricData} object.
     * @return a {@link java.lang.String} object.
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
