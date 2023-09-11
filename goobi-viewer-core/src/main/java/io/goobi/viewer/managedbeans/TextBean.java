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
package io.goobi.viewer.managedbeans;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;

import de.intranda.digiverso.ocr.tei.TEIBuilder;
import de.intranda.digiverso.ocr.tei.convert.HtmlToTEIConvert.ConverterMode;
import de.intranda.digiverso.ocr.tei.convert.TeiToHtmlConvert;
import de.intranda.digiverso.ocr.tei.convert.wiener.PopoverNoteReplacer;
import de.intranda.digiverso.ocr.tei.convert.wiener.TeiToHtmlConverter;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.TEITools;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.translations.language.Language;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Bean representing the primary source record for image display. Whether a source record or an annotation record is currently loaded in
 * ActiveDocumentBean, this bean will always contain the source record.
 */
@Named
@SessionScoped
public class TextBean implements Serializable {

    private static final long serialVersionUID = 7458534493098897433L;

    private static final Logger logger = LogManager.getLogger(TextBean.class);

    /** Empty constructor. */
    public TextBean() {
        // the emptiness inside
    }

    /**
     * Returns the schoolbook abstract portion of the TEI document, converting TEI markup to HTML.
     * 
     * @param topDocument
     * @param language ISO 639-1 language code
     */
    public String getAbstractSchoolbook(StructElement topDocument, String language) {
        return getAbstract(topDocument, "ProfileDescAbstractSchoolbook", language);
    }

    /**
     * Returns the abstract portion of the TEI document, converting TEI markup to HTML.
     * 
     * @param topDocument
     * @param language ISO 639-1 language code
     * @return
     */
    public String getAbstract(StructElement topDocument, String language) {
        return getAbstract(topDocument, "ProfileDescAbstractLong", language);
    }

    /**
     * Returns the abstract portion of the given type of the TEI document, converting TEI markup to HTML.
     * 
     * @param topDocument
     * @param abstractType
     * @param language
     * @return
     * @should return abstract correctly
     * @should throw IllegalArgumentException if language null
     * @should return null if topDocument null
     * @should return null if topDocument has no tei for language
     */
    public String getAbstract(StructElement topDocument, String abstractType, String language) {
        logger.trace("getAbstract: {}", language);
        if (language == null) {
            throw new IllegalArgumentException("language may not be null");
        }

        if (topDocument == null) {
            logger.trace("topDocument is null");
            return null;
        }
        if (!topDocument.isHasTeiForLanguage(language)) {
            logger.trace("Field not found: {}{}{}", SolrConstants.FILENAME_TEI, SolrConstants.MIDFIX_LANG, language);
            return null;
        }

        String fileName = topDocument.getMetadataValue(SolrConstants.FILENAME_TEI + SolrConstants.MIDFIX_LANG + language.toUpperCase());
        try {
            String filePath = DataFileTools.getTextFilePath(topDocument.getPi(), fileName, SolrConstants.FILENAME_TEI);
            logger.trace(StringConstants.LOG_LOADING, filePath);
            Document doc = XmlTools.getDocumentFromString(FileTools.getStringFromFilePath(filePath), null);
            if (doc == null || doc.getRootElement() == null) {
                logger.trace("Could not construct XML document");
                return null;
            }
            Language lang = DataManager.getInstance().getLanguageHelper().getLanguage(language);
            if (lang == null) {
                return null;
            }
            List<Element> eleListAbstract = XmlTools.evaluateToElements("tei:teiHeader/tei:profileDesc/tei:abstract[@xml:id='" + abstractType
                    + "'][@xml:lang='" + lang.getIsoCode() + "']", doc.getRootElement(), Collections.singletonList(TEITools.NAMESPACE_TEI));
            if (eleListAbstract == null || eleListAbstract.isEmpty()) {
                eleListAbstract = XmlTools.evaluateToElements(
                        // Fallback to English
                        "tei:teiHeader/tei:profileDesc/tei:abstract[@xml:id='" + abstractType + "'][@xml:lang='eng']", doc.getRootElement(),
                        Collections.singletonList(TEIBuilder.NAMESPACE_TEI));
                if (eleListAbstract == null || eleListAbstract.isEmpty()) {
                    logger.debug("No abstract found");
                    return null;
                }
            }
            String abstractRaw = XmlTools.getStringFromElement(eleListAbstract.get(0), StringTools.DEFAULT_ENCODING);
            String abstractConverted = new TeiToHtmlConvert().convert(abstractRaw);
            abstractConverted = abstractConverted.replaceAll("<abstract[^>]*?>", "").replace("</abstract>", "");

            // Check whether the text contains just empty tags with no visible text and return null if that is the case
            String abstractReduced = removeEmptyParagraphs(abstractConverted);
            if (!StringUtils.isBlank(abstractReduced)) {
                return abstractConverted;
            }
        } catch (FileNotFoundException | PresentationException e) {
            logger.error(e.getMessage());
        } catch (IndexUnreachableException | IOException | JDOMException e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * Loads and returns the TEI text for the given record and language if full-text access is granted to the client.
     * 
     * @param topDocument
     * @param language
     * @return TEI full-text or access denied message
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     * @throws RecordNotFoundException
     * @throws IOException
     * @throws FileNotFoundException
     * @should return text correctly
     * @should return null if topDocument null
     */
    public String getTeiText(StructElement topDocument, String language)
            throws IndexUnreachableException, DAOException, PresentationException, RecordNotFoundException {
        if (topDocument == null) {
            logger.trace("topDocument is null");
            return null;
        }

        AccessPermission permission = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(topDocument.getPi(), null,
                IPrivilegeHolder.PRIV_VIEW_FULLTEXT, BeanUtils.getRequest());
        if (!permission.isGranted()) {
            logger.trace("Access denied for TEI text for {}", topDocument.getPi());
            return ViewerResourceBundle.getTranslation("fulltextAccessDenied", null);
        }

        String currentFulltext = null;
        if (StringUtils.isNotEmpty(language) && topDocument.getMetadataFields()
                .containsKey(SolrConstants.FILENAME_TEI + SolrConstants.MIDFIX_LANG
                        + language.toUpperCase())) {
            String fileName = topDocument.getMetadataValue(SolrConstants.FILENAME_TEI + SolrConstants.MIDFIX_LANG + language.toUpperCase());
            String filePath = DataFileTools.getTextFilePath(topDocument.getPi(), fileName, SolrConstants.FILENAME_TEI);
            logger.trace(StringConstants.LOG_LOADING, filePath);
            currentFulltext = loadTeiFulltext(filePath);
        } else if (topDocument.getMetadataFields().containsKey(SolrConstants.FILENAME_TEI)) {
            String fileName = topDocument.getMetadataValue(SolrConstants.FILENAME_TEI);
            String filePath = DataFileTools.getTextFilePath(topDocument.getPi(), fileName, SolrConstants.FILENAME_TEI);
            logger.trace(StringConstants.LOG_LOADING, filePath);
            currentFulltext = loadTeiFulltext(filePath);
        }

        return currentFulltext;
    }

    /**
     * 
     * @param topDocument
     * @return
     * @should return return all tei languages
     */
    public List<String> getRecordLanguages(StructElement topDocument) {
        return topDocument.getMetadataFields()
                .keySet()
                .stream()
                .filter(field -> field.matches(SolrConstants.FILENAME_TEI + SolrConstants.MIDFIX_LANG
                        + "\\w{1,3}"))
                .map(field -> Optional.ofNullable(DataManager.getInstance()
                        .getLanguageHelper()
                        .getLanguage(field.substring(field.lastIndexOf("_") + 1).toLowerCase()))
                        .map(Language::getIsoCode).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Removes all
     * <p>
     * tags that contain no text.
     * 
     * @param text
     * @return
     * @should remove empty paragraph tags correctly
     */
    static String removeEmptyParagraphs(String text) {
        return text.replaceAll("<p>\\s*</p>", "").replaceAll("<p\\s*/>", "").replaceAll("<br\\s*/>", "").trim();
    }

    /**
     * Loads TEI full-text from the given file path. The text portion is cut out of the main document and its markup is converted to HTML.
     * 
     * @param filePath
     * @return
     * @should load text correctly
     * @should return null if file not found
     */
    public static String loadTeiFulltext(String filePath) {
        try {
            Document doc = XmlTools.getDocumentFromString(FileTools.getStringFromFilePath(filePath), null);
            if (doc != null && doc.getRootElement() != null) {
                Element eleText = doc.getRootElement().getChild("text", null);
                if (eleText != null && eleText.getChild("body", null) != null) {
                    String language = eleText.getAttributeValue("lang", Namespace.getNamespace("xml", "http://www.w3.org/XML/1998/namespace"));
                    Element eleBody = eleText.getChild("body", null);
                    Element eleNewRoot = new Element("tempRoot");
                    for (Element ele : eleBody.getChildren()) {
                        eleNewRoot.addContent(ele.clone());
                    }
                    String html = XmlTools.getStringFromElement(eleNewRoot, null).replace("<tempRoot>", "").replace("</tempRoot>", "").trim();
                    // return new TeiToHtmlConvert().setLanguage(language).convert(html);
                    return new TeiToHtmlConverter(ConverterMode.resource, new PopoverNoteReplacer()).convert(html, language);
                }
            }
        } catch (FileNotFoundException e) {
            logger.warn(e.getMessage());
        } catch (IOException | JDOMException e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }
}
