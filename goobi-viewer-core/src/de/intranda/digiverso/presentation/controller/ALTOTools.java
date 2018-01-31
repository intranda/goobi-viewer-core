/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.intranda.digiverso.presentation.controller;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.ocr.alto.model.structureclasses.Line;
import de.intranda.digiverso.ocr.alto.model.structureclasses.Page;
import de.intranda.digiverso.ocr.alto.model.structureclasses.lineelements.Word;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.Tag;
import de.intranda.digiverso.ocr.alto.model.superclasses.GeometricData;
import de.intranda.digiverso.ocr.alto.utils.HyphenationLinker;
import de.intranda.digiverso.presentation.servlets.rest.ner.ElementReference;
import de.intranda.digiverso.presentation.servlets.rest.ner.NERTag;
import de.intranda.digiverso.presentation.servlets.rest.ner.NERTag.Type;
import de.intranda.digiverso.presentation.servlets.rest.ner.TagCount;

public class ALTOTools {

	private static final Logger logger = LoggerFactory.getLogger(ALTOTools.class);

	private final static String STRING = "String";
	private final static String CONTENT = "CONTENT";
	private final static String ID = "ID";
	private final static String TEXTLINE = "TextLine";
	private final static String TAGREFS = "TAGREFS";
	private final static String NETAG = "NamedEntityTag";
	private final static String TYPE = "TYPE";
	private final static String URI = "URI";
	private final static String LABEL = "LABEL";
	
	public static final String TAG_LABEL_IGNORE_REGEX="^\\W+|\\W+$";


	/**
	 *
	 * @param altoDoc
	 * @return
	 * @should throw IllegalArgumentException if altoDoc is null
	 */
	public static String getFullText(String alto) {

		try {
			return alto2Txt(alto);
		} catch (IOException | XMLStreamException e) {
			logger.error(e.getMessage(), e);
		}

		return null;
	}

	public static List<TagCount> getNERTags(String alto, NERTag.Type type) {
		List<TagCount> tags = new ArrayList<>();
		try {

			AltoDocument doc = AltoDocument.getDocumentFromString(alto);
			for (Tag tag : doc.getTags().getTagsAsList()) {
				if (type == null) {
					addTags(createNERTag(tag), tags);
				} else if (type.matches(tag.getType())) {
					addTags(createNERTag(tag), tags);
				}
			}
		} catch (Throwable e) {
			logger.trace("Error loading alto from \n\"" + alto + "\"");
		}
		return tags;
	}

	/**
	 * @param createNERTag
	 * @param tags
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
	 * @param solrDoc
	 * @return
	 */
	private static List<TagCount> createNERTag(Tag tag) {
		String value = tag.getLabel();
		value = value.replaceAll(TAG_LABEL_IGNORE_REGEX, "");
		Type type = Type.getType(tag.getType());
		ElementReference element = null;

		List<TagCount> nerTags = new ArrayList<TagCount>();
		for (GeometricData reference : tag.getReferences()) {

			String elementId = reference.getId();
			Rectangle elementCoordinates = reference.getRect().getBounds();
			String elementContent = reference.getContent();
			element = new ElementReference(elementId, elementCoordinates, elementContent);
			TagCount nerTag = new TagCount(value, type, element);
			nerTags.add(nerTag);
		}
		return nerTags;
	}

	/**
	 * 
	 * @param alto
	 * @return
	 * @throws IOException
	 * @throws XMLStreamException
	 * @should use extract fulltext correctly
	 */
	protected static String alto2Txt(String alto) throws IOException, XMLStreamException {
		if (alto == null) {
			throw new IllegalArgumentException("alto may not be null");
		}
		Map<String, String> neTypeMap = new HashMap<>();
		Map<String, String> neLabelMap = new HashMap<>();
		Map<String, String> neUriMap = new HashMap<>();
		Set<String> usedTags = new HashSet<>();
		StringBuilder strings = new StringBuilder(500);
		// boolean textline = false;
		try (InputStream is = new ByteArrayInputStream(alto.getBytes(StandardCharsets.UTF_8))) {
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLStreamReader parser = factory.createXMLStreamReader(is);

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
						String tagref = null;
						for (int i = 0; i < parser.getAttributeCount(); i++) {
							switch (parser.getAttributeLocalName(i)) {
							case CONTENT:
								content = parser.getAttributeValue(i);
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
									FacesContext context = FacesContext.getCurrentInstance();
									HttpServletRequest request = (HttpServletRequest) context.getExternalContext()
											.getRequest();
									String contextPath = request.getContextPath();
									strings.append("<span data-remotecontent=\"");
									strings.append(contextPath);
									strings.append("/api?action=normdata&amp;url=");
									strings.append(neUriMap.get(tagref));
									strings.append("&amp;lang=de\" class=\"ner-trigger\" title=\"");
									strings.append(neLabelMap.get(tagref));
									strings.append("\" tabindex=\"-1\"><span class=\"ner-popover-pointer\"></span>");
									switch (neTypeMap.get(tagref)) {
									case "person":
										strings.append("<span class=\"glyphicon glyphicon-user\"></span>");
										break;
									case "location":
										strings.append("<span class=\"glyphicon glyphicon-map-marker\"></span>");
										break;
									case "institution":
										strings.append("<span class=\"glyphicon glyphicon-home\"></span>");
										break;
									default:
										strings.append("<span></span>");
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
							strings.append(content);
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
						strings.append("<br>\n");
					}
					break;

				default:
					break;

				}
				parser.next();
			}
		}
		if (strings.length() > 0) {
			strings.deleteCharAt(strings.length() - 1);
		}

		return strings.toString();
	}

	public static List<String> getWordCoords(Document altoDoc, Set<String> searchTerms) {
		return getWordCoords(altoDoc, searchTerms, 0, 0);
	}

	public static String getRotatedCoordinates(String coords, int rotation, Dimension pageSize) {
		if (rotation != 0) {
			try {
				Rectangle wordRect = getRectangle(coords);
				wordRect = rotate(wordRect, rotation, pageSize);
				// wordRect = addImageFooter(wordRect, rotation,
				// imageFooterHeight);
				coords = getString(wordRect);
			} catch (NumberFormatException e) {
				logger.error("Cannot rotate coords {}: {}", coords, e.getMessage());
			}
		}
		return coords;
	}

	/**
	 * TODO Re-implement using stream
	 *
	 * @param altoDoc
	 * @param searchTerms
	 * @return
	 * @should throw IllegalArgumentException if altoDoc is null
	 */
	public static List<String> getWordCoords(Document altoDoc, Set<String> searchTerms, int rotation,
			int imageFooterHeight) {
		if (altoDoc == null) {
			throw new IllegalArgumentException("altoDoc may not be null");
		}
		List<Word> words = new ArrayList<>();
		Dimension pageSize = new Dimension(0, 0);
		try {
			AltoDocument document = new AltoDocument(altoDoc.getRootElement());
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
					if (eleWord.getHyphenationPartNext() != null
							&& eleWord.getHyphenationPartNext().getContent().matches("\\S+")) {
						wordIndex++;
						addWordCoords(rotation, pageSize, eleWord.getHyphenationPartNext(), tempList);
					}
					// Match next words if search term has more than one word
					if (totalHits < searchWords.length) {
						while (totalHits < searchWords.length && words.size() > wordIndex + 1) {
							wordIndex++;
							Word nextWord = words.get(wordIndex);
							int hits = ALTOTools.getMatchALTOWord(nextWord,
									Arrays.copyOfRange(searchWords, totalHits, searchWords.length));
							if (hits == 0) {
								match = false;
								break;
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

		return coordList;// new ArrayList<>();
	}

	private static String addWordCoords(int rotation, Dimension pageSize, Word eleWord, List<String> tempList) {
		String coords = ALTOTools.getALTOCoords(eleWord);
		if (rotation != 0) {
			try {
				Rectangle wordRect = getRectangle(coords);
				wordRect = rotate(wordRect, rotation, pageSize);
				// wordRect = addImageFooter(wordRect, rotation,
				// imageFooterHeight);
				coords = getString(wordRect);
			} catch (NumberFormatException e) {
				logger.error("Cannot rotate coords {}: {}", coords, e.getMessage());
			}
		}
		if (coords != null) {
			tempList.add(coords);
			logger.trace("ALTO word found: {} ({})", eleWord.getAttributeValue("CONTENT"), coords);
		}
		return coords;
	}

	/**
	 * @param wordRect
	 * @return
	 */
	private static String getString(Rectangle rect) {
		StringBuilder sb = new StringBuilder();
		sb.append(rect.x).append(",").append(rect.y).append(",").append(rect.x + rect.width).append(",")
				.append(rect.y + rect.height);
		return sb.toString();
	}

	/**
	 * @param rect
	 * @param rotation
	 * @return
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
			;
			break;
		default:
			// coordinates unchanged
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
	 * Reads rectangle coordinates from the given String. The String-coordinates
	 * are assumed to be int coordinates for left, top, right, bottom in that
	 * order
	 *
	 * @param coords
	 * @return
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

		Rectangle rect = new Rectangle(left, top, right - left, bottom - top);
		return rect;
	}

	/**
	 * An ALTO ComposedBlock can theoretically contain n levels of nested
	 * ComposedBlocks. Collect all words from contained TextBlocks recursively.
	 *
	 * @param eleComposedBlock
	 * @return
	 * @should return all words from nested ComposedBlocks
	 */
	public static List<Element> handleAltoComposedBlock(Element eleComposedBlock) {
		List<Element> words = new ArrayList<>();

		// Words from TextBlocks
		for (Element eleTextBlock : eleComposedBlock.getChildren("TextBlock", null)) {
			for (Element eleLine : eleTextBlock.getChildren("TextLine", null)) {
				words.addAll(eleLine.getChildren("String", null));
			}
		}

		// Nested ComposedBlocks
		List<Element> eleListNextedComposedBlocks = eleComposedBlock.getChildren("ComposedBlock", null);
		if (eleListNextedComposedBlocks != null) {
			for (Element eleNestedComposedBlock : eleComposedBlock.getChildren("ComposedBlock", null)) {
				words.addAll(handleAltoComposedBlock(eleNestedComposedBlock));
			}
		}

		return words;
	}

	/**
	 * @param eleWord
	 * @param string
	 * @return
	 */
	public static int getMatchALTOWord(Word eleWord, String[] words) {
		if (words != null && words.length > 0) {

			// Word exists and has content
			String content = eleWord.getContent();
			// Normalize (remove diacritical marks)
			content = Helper.removeDiacriticalMarks(content);
			// Clean up leading non-alphanumeric characters so that matching
			// works
			while (content.length() > 0 && !StringUtils.isAlphanumeric(content.substring(0, 1))) {
				content = content.substring(1);
			}
			// replace content with complete content of hyphenated word if
			// applicable
			if (content.matches("\\S+")) {
				String subsContent = eleWord.getAttributeValue("SUBS_CONTENT");
				if (subsContent != null && !subsContent.isEmpty()) {
					content = subsContent;
				}
			}

			if (content.trim().contains(" ")) {
				// not a word, but a line
				content = content.trim().replaceAll("\\s+", " ").toLowerCase();
				int hitCount = words.length;
				StringBuilder sbMatchString = new StringBuilder();
				for (String string : words) {
					if (sbMatchString.length() > 0) {
						sbMatchString.append(' ');
					}
					sbMatchString.append(string.toLowerCase());
				}
				String matchString = sbMatchString.toString();
				for (; hitCount > 0; hitCount--) {
					if (content.contains(matchString)) {
						break;
					} else if (!matchString.contains(" ")) {
						// last word didn't match, so no match
						return 0;
					} else {
						matchString = matchString.substring(0, matchString.lastIndexOf(' '));
					}
				}
				return hitCount;
			}
			String word = Helper.removeDiacriticalMarks(words[0].toLowerCase());
			if (content.trim().toLowerCase().startsWith(word)) {
				return 1;
			}
		}

		return 0;
	}

	/**
	 *
	 * @param eleWord
	 * @return
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
			coords = new StringBuilder().append(hpos).append(",").append(vpos).append(",").append(hpos + width)
					.append(",").append(vpos + height).toString();
		} catch (NumberFormatException e) {
			logger.error(e.getMessage());
		}

		return coords;
	}
}
