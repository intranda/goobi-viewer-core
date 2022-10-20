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
package io.goobi.viewer.model.metadata;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.solr.common.SolrDocumentList;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.translations.language.Language;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrConstants.MetadataGroupType;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

/**
 * <p>
 * MetadataTools class.
 * </p>
 */
public class MetadataTools {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(MetadataTools.class);

    /**
     * <p>
     * generateDublinCoreMetaTags.
     * </p>
     *
     * @param structElement a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @return String containing meta tags
     */
    public static String generateDublinCoreMetaTags(StructElement structElement) {
        if (structElement == null) {
            return "";
        }

        StringBuilder result = new StringBuilder(100);

        String title = "-";
        String creators = "-";
        String publisher = "-";
        String yearpublish = "-";
        String placepublish = "-";
        String date = null;
        String identifier = null;
        String rights = null;
        String language = null;
        String isoLanguage = null;

        // schema
        result.append("\r\n<link rel=\"schema.DCTERMS\" href=\"http://purl.org/dc/terms/\" />");
        result.append("\r\n<link rel=\"schema.DC\" href=\"http://purl.org/dc/elements/1.1/\" />");

        // Determine language and ISO-2 language code
        if (structElement.getMetadataValue("MD_LANGUAGE") != null) {
            language = structElement.getMetadataValue("MD_LANGUAGE");
            isoLanguage = convertLanguageToIso2(language);
        }

        if (structElement.getMetadataValue("MD_TITLE") != null) {
            title = structElement.getMetadataValues("MD_TITLE").iterator().next();
            result.append("\r\n<meta name=\"DC.title\" content=\"").append(title).append("\"");
            if (isoLanguage != null && isoLanguage.length() == 2) {
                result.append(" xml:lang=\"").append(isoLanguage).append('"');
            }
            result.append(" />");
        }

        if (structElement.getMetadataValue("MD_CREATOR") != null) {
            for (Object fieldValue : structElement.getMetadataValues("MD_CREATOR")) {
                String value = (String) fieldValue;
                if (StringUtils.isEmpty(creators)) {
                    creators = value;
                } else {
                    creators = new StringBuilder(creators).append(", ").append(value).toString();
                }
            }
            result.append("\r\n<meta name=\"DC.creator\" content=\"").append(creators).append("\"");
            if (isoLanguage != null && isoLanguage.length() == 2) {
                result.append(" xml:lang=\"").append(isoLanguage).append('"');
            }
            result.append(" />");
        }
        // DC.publisher
        if (structElement.getMetadataValue("MD_PUBLISHER") != null) {
            publisher = structElement.getMetadataValue("MD_PUBLISHER");
            result.append("\r\n<meta name=\"DC.publisher\" content=\"").append(publisher).append("\"");
            if (isoLanguage != null && isoLanguage.length() == 2) {
                result.append(" xml:lang=\"").append(isoLanguage).append('"');
            }
            result.append(" />");
        }
        // DC.date
        if (structElement.getMetadataValue("MD_YEARPUBLISH") != null) {
            date = structElement.getMetadataValue("MD_YEARPUBLISH");
            result.append("\r\n<meta name=\"DC.date\" content=\"").append(date).append("\"");
            if (isoLanguage != null && isoLanguage.length() == 2) {
                result.append(" xml:lang=\"").append(isoLanguage).append('"');
            }
            result.append(" scheme=\"W3CTF\" />");
        }
        // DC.language
        if (language != null) {
            if (language.length() != 2) {
                // non-iso2
                result.append("\r\n<meta name=\"DC.language\" content=\"").append(language).append("\"");
                if (isoLanguage != null && isoLanguage.length() == 2) {
                    result.append(" xml:lang=\"").append(isoLanguage).append('"');
                }
                result.append(" />");
            }
            if (isoLanguage != null && isoLanguage.length() == 2) {
                // iso2
                result.append("\r\n<meta name=\"DC.language\" content=\"")
                        .append(isoLanguage)
                        .append("\" xml:lang=\"")
                        .append(isoLanguage)
                        .append("\" scheme=\"DCTERMS.RFC1766\" />");
            }

        }
        // DC.identifier
        if (structElement.getMetadataValue(SolrConstants.URN) != null) {
            identifier = structElement.getMetadataValue(SolrConstants.URN);
            result.append("\r\n<meta name=\"DC.identifier\" content=\"").append(identifier).append("\" scheme=\"DCTERMS.URI\" />");
        }
        if (structElement.getMetadataValue("MD_PI_DOI_URL") != null) {
            identifier = structElement.getMetadataValue("MD_PI_DOI_URL");
            result.append("\r\n<meta name=\"DC.identifier\" content=\"").append(identifier).append("\" scheme=\"DCTERMS.URI\" />");
        }
        // DCTERMS.abstract
        if (structElement.getMetadataValue("MD_INFORMATION") != null) {
            String value = structElement.getMetadataValue("MD_INFORMATION");
            result.append("\r\n<meta name=\"DCTERMS.abstract\" content=\"").append(StringEscapeUtils.escapeHtml4(value)).append("\"");
            if (isoLanguage != null && isoLanguage.length() == 2) {
                result.append(" xml:lang=\"").append(isoLanguage).append('"');
            }
            result.append(" />");
        }

        String sourceString = new StringBuilder(creators).append(": ")
                .append(title)
                .append(", ")
                .append(placepublish)
                .append(": ")
                .append(publisher)
                .append(' ')
                .append(yearpublish)
                .append('.')
                .toString();

        result.append("\r\n<meta name=\"DC.source\" content=\"").append(sourceString).append("\" />");

        if (structElement.getMetadataValue(SolrConstants.ACCESSCONDITION) != null) {
            rights = structElement.getMetadataValue(SolrConstants.ACCESSCONDITION);
            if (!SolrConstants.OPEN_ACCESS_VALUE.equals(rights)) {
                result.append("\r\n<meta name=\"DC.rights\" content=\"").append(rights).append("\" />");
            }
        }

        return result.toString();
    }

    /**
     * <p>
     * generateHighwirePressMetaTags.
     * </p>
     *
     * @param structElement a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param pages a {@link java.util.List} object.
     * @return String containing meta tags
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public static String generateHighwirePressMetaTags(StructElement structElement, List<PhysicalElement> pages)
            throws IndexUnreachableException, ViewerConfigurationException, PresentationException {
        if (structElement == null) {
            return "";
        }

        StructElement anchorElement = structElement.getParent();
        StringBuilder result = new StringBuilder(100);

        // citation_title
        String title = "";
        if (anchorElement != null && anchorElement.getMetadataValue("MD_TITLE") != null) {
            title = StringEscapeUtils.escapeHtml4(anchorElement.getMetadataValue("MD_TITLE")) + ": ";
        }
        if (structElement.getMetadataValue("MD_TITLE") != null) {
            title += StringEscapeUtils.escapeHtml4(structElement.getMetadataValue("MD_TITLE"));
        }
        result.append("\r\n<meta name=\"citation_title\" content=\"").append(title).append("\" />");

        // citation_author
        if (structElement.getMetadataValue("MD_CREATOR") != null) {
            for (Object fieldValue : structElement.getMetadataValues("MD_CREATOR")) {
                String value = StringEscapeUtils.escapeHtml4((String) fieldValue);
                result.append("\r\n<meta name=\"citation_author\" content=\"").append(value).append("\" />");
            }
        }
        // citation_publication_date
        if (structElement.getMetadataValue(SolrConstants.YEARPUBLISH) != null) {
            String value = structElement.getMetadataValue(SolrConstants.YEARPUBLISH);
            List<String> normalizedValues = structElement.getMetadataValues(SolrConstants._CALENDAR_YEAR);
            if (normalizedValues != null && !normalizedValues.isEmpty()) {
                for (String normalizedValue : normalizedValues) {
                    if (value.contains(normalizedValue)) {
                        result.append("\r\n<meta name=\"citation_publication_date\" content=\"").append(normalizedValue).append("\" />");
                        break;
                    }
                }
            }
        }
        // citation_isbn
        if (structElement.getMetadataValue("MD_ISBN") != null) {
            String value = StringEscapeUtils.escapeHtml4(structElement.getMetadataValue("MD_ISBN"));
            result.append("\r\n<meta name=\"citation_isbn\" content=\"").append(value).append("\" />");
        }
        // citation_issn
        if (structElement.getMetadataValue("MD_ISSN") != null) {
            String value = StringEscapeUtils.escapeHtml4(structElement.getMetadataValue("MD_ISSN"));
            result.append("\r\n<meta name=\"citation_issn\" content=\"").append(value).append("\" />");
        }
        // citation_volume
        if (structElement.getMetadataValue(SolrConstants.CURRENTNO) != null) {
            String value = StringEscapeUtils.escapeHtml4(structElement.getMetadataValue(SolrConstants.CURRENTNO));
            result.append("\r\n<meta name=\"citation_volume\" content=\"").append(value).append("\" />");
        }
        // citation_language
        if (structElement.getMetadataValue("MD_LANGUAGE") != null) {
            String value = StringEscapeUtils.escapeHtml4(structElement.getMetadataValue("MD_LANGUAGE"));
            value = convertLanguageToIso2(value);
            result.append("\r\n<meta name=\"citation_language\" content=\"").append(value).append("\" />");
        }
        //  citation_pdf_url
        if (pages != null && !pages.isEmpty()) {
            for (PhysicalElement page : pages) {
                if (page == null) {
                    continue;
                }
                String value = StringEscapeUtils.escapeHtml4(page.getUrl());
                result.append("\r\n<meta name=\"citation_pdf_url\" content=\"").append(value).append("\" />");
            }
        }
        // abstract
        if (structElement.getMetadataValue("MD_INFORMATION") != null) {
            // citation_abstract_html_url
            result.append("\r\n<meta name=\"citation_abstract_html_url\" content=\"").append(structElement.getMetadataUrl()).append("\" />");

            // description (non-highwire)
            String value = StringEscapeUtils.escapeHtml4(structElement.getMetadataValue("MD_INFORMATION"));
            result.append("\r\n<meta name=\"description\" content=\"").append(value).append("\" />");
        }
        // citation_doi
        if (structElement.getMetadataValue("MD_PI_DOI_URL") != null) {
            String value = StringEscapeUtils.escapeHtml4(structElement.getMetadataValue("MD_PI_DOI_URL"));
            result.append("\r\n<meta name=\"citation_doi\" content=\"").append(value).append("\" />");
        }


        return result.toString();
    }

    /**
     * <p>
     * generateRIS.
     * </p>
     *
     * @param structElement a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @return a {@link java.lang.String} object.
     */
    public static String generateRIS(StructElement structElement) {
        if (structElement == null) {
            return null;
        }

        StringBuilder result = new StringBuilder(100);
        result.append("TY  - ").append(getRISTypeMapping(structElement.getDocStructType())).append("\r\n");
        for (String field : structElement.getMetadataFields().keySet()) {
            List<String> values = structElement.getMetadataFields().get(field);
            if (values == null || values.isEmpty()) {
                continue;
            }
            String risTag = null;
            switch (field) {
                case "CURRENTNO":
                    risTag = "VL";
                    break;
                case "MD_ABSTRACT":
                case "MD_INFORMATION":
                    risTag = "AB";
                    break;
                case "MD_ALTERNATETITLE":
                    risTag = "J2";
                    break;
                case "MD_AUTHOR":
                case "MD_CREATOR":
                    risTag = "AU";
                    break;
                case "MD_EDITION":
                    risTag = "ET";
                    break;
                case "MD_EDITOR":
                    risTag = "ED";
                    break;
                case "MD_GEOKEYWORD":
                case "MD_PERSONKEYWORD":
                case "MD_WORKKEYWORD":
                    risTag = "KW";
                    break;
                case "MD_ISBN":
                case "MD_ISSN":
                    risTag = "SN";
                    break;
                case "MD_LANGUAGE":
                    risTag = "LA";
                    break;
                case "MD_NOTE":
                    risTag = "N1";
                    break;
                case "MD_PLACEPUBLISH":
                    risTag = "PP";
                    break;
                case "MD_PUBLISHER":
                    risTag = "PB";
                    break;
                case "MD_TITLE":
                    risTag = "TI";
                    break;
                case "MD_YEARPUBLISH":
                    risTag = "PY";
                    break;
                case "NUMPAGES":
                    risTag = "SP";
                    break;
                case "NUMVOLUMES":
                    risTag = "NV";
                    break;
                case "PI_TOPSTRUCT":
                    risTag = "CN";
                    break;
            }
            if (risTag == null) {
                continue;
            }
            int count = 1;
            Set<String> usedValues = new HashSet<>(values.size());
            for (String value : values) {
                if (usedValues.contains(value)) {
                    continue;
                }
                String useRisTag = risTag;
                if (useRisTag.length() == 1) {
                    useRisTag += count;
                    count++;
                }
                result.append(useRisTag).append("  - ").append(value).append("\r\n");
                usedValues.add(value);
            }

        }

        result.append("ER  - \r\n");

        return result.toString();
    }

    /**
     *
     * @param docstructType
     * @return
     */
    static String getRISTypeMapping(String docstructType) {
        if (docstructType == null) {
            return null;
        }

        switch (docstructType.toLowerCase()) {
            case "abstract":
                return "ABST";
            case "article":
                return "MGZN";
            case "audio":
                return "AUDIO";
            case "chapter":
                return "CHAP";
            case "figure":
            case "picture":
                return "FIGURE";
            case "manuscript":
                return "MANSCPT";
            case "monograph":
                return "BOOK";
            case "map":
                return "MAP";
            case "mutivolumework":
            case "multivolume_work":
                return "SER";
            case "periodical":
                return "JFULL";
            case "periodicalvolume":
            case "periodical_volume":
                return "JOUR";
            case "sheetmusic":
                return "MUSIC";
            case "video":
                return "VIDEO";
            default:
                return "GEN";
        }
    }

    /**
     * Converts given language name or ISO-3 code to ISO-2, if possible.
     *
     * @param language a {@link java.lang.String} object.
     * @return ISO-2 representation; original string if none found
     * @should return original value if language not found
     */
    public static String convertLanguageToIso2(String language) {
        if (language == null) {
            return null;
        }

        if (language.length() == 3) {
            Language lang = null;
            try {
                lang = DataManager.getInstance().getLanguageHelper().getLanguage(language);
            } catch (IllegalArgumentException e) {
                logger.warn("No language found for " + lang);
            }
            if (lang != null) {
                return lang.getIsoCodeOld();
            }
        }

        // dirty ISO-2 conversion
        switch (language.toLowerCase()) {
            case "english":
                return "en";
            case "deutsch":
            case "deu":
            case "ger":
                return "de";
            case "französisch":
            case "franz.":
            case "fra":
            case "fre":
                return "fr";
        }

        return language;
    }

    /**
     * <p>
     * applyReplaceRules.
     * </p>
     *
     * @param value Metadata value to modify
     * @param replaceRules List of <code>MetadataReplaceRule</code> objects
     * @param pi Record identifier to whose context the value belongs; used for checking conditions
     * @return a {@link java.lang.String} object.
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should apply rules correctly
     * @should apply conditional rules correctly
     */
    public static String applyReplaceRules(String value, List<MetadataReplaceRule> replaceRules, String pi)
            throws IndexUnreachableException, PresentationException {
        if (value == null) {
            return null;
        }
        if (replaceRules == null) {
            return value;
        }

        String ret = value;
        for (MetadataReplaceRule replaceRule : replaceRules) {
            // Skip rules that have non-matching conditions
            if (StringUtils.isNotEmpty(replaceRule.getConditions()) && StringUtils.isNotEmpty(pi)) {
                String conditions = SolrTools.getProcessedConditions(replaceRule.getConditions());
                String query = "+" + SolrConstants.PI_TOPSTRUCT + ":" + pi + " +(" + conditions + ")";
                long count = DataManager.getInstance().getSearchIndex().getHitCount(query);
                if (count == 0) {
                    continue;
                }
            }

            switch (replaceRule.getType()) {
                case CHAR:
                    StringBuffer sb = new StringBuffer();
                    sb.append(replaceRule.getKey());
                    ret = ret.replace(sb.toString(), replaceRule.getReplacement());
                    break;
                case STRING:
                    ret = ret.replace((String) replaceRule.getKey(), replaceRule.getReplacement());
                    break;
                case REGEX:
                    ret = ret.replaceAll(((String) replaceRule.getKey()), replaceRule.getReplacement());
                    break;
                default:
                    logger.error("Unknown replacement key type of '{}: {}", replaceRule.getKey(), replaceRule.getKey().getClass().getName());
                    break;
            }
        }

        return ret;
    }

    /**
     * <p>
     * findMetadataGroupType.
     * </p>
     *
     * @param gndspec a {@link java.lang.String} object.
     * @return MetadataGroupType value corresponding to the given gndspec type
     * @should map values correctly
     */
    public static String findMetadataGroupType(String gndspec) {
        if (gndspec == null) {
            return null;
        }
        if (gndspec.length() == 3) {
            String ret = null;
            switch (gndspec.substring(0, 2)) {
                case "gi":
                    ret = MetadataGroupType.LOCATION.name();
                    break;
                case "ki":
                    ret = MetadataGroupType.CORPORATION.name();
                    break;
                case "pi":
                    ret = MetadataGroupType.PERSON.name();
                    break;
                case "sa":
                    ret = MetadataGroupType.SUBJECT.name();
                    break;
                case "vi":
                    ret = MetadataGroupType.CONFERENCE.name();
                    break;
                case "wi":
                    ret = MetadataGroupType.RECORD.name();
                    break;
            }
            if (ret != null) {
                logger.trace("Authority data type determined from 075$b (gndspec): {}", ret);
                return ret;
            }
        }

        logger.trace("Authority data type could not be determined for '{}'.", gndspec);
        return null;
    }

    /**
     *
     * @param ownerIddoc owner IDDOC
     * @param subQuery Optional additional subQuery for filtering
     * @param sortFields Optional field/order pairs for sorting
     * @return SolrDocumentList
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @should return grouped metadata docs correctly
     */
    public static SolrDocumentList getGroupedMetadata(String ownerIddoc, String subQuery, List<StringPair> sortFields)
            throws PresentationException, IndexUnreachableException {
        if (StringUtils.isEmpty(ownerIddoc)) {
            throw new IllegalArgumentException("ownerIddoc may not be null or empty");
        }

        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append('+')
                .append(SolrConstants.IDDOC_OWNER)
                .append(':')
                .append(ownerIddoc)
                .append(" +")
                .append(SolrConstants.DOCTYPE)
                .append(':')
                .append(DocType.METADATA.name());
        if (StringUtils.isNotEmpty(subQuery)) {
            sbQuery.append(' ').append(subQuery);
        }

        // logger.trace("getGroupedMetadata query: {}", sbQuery.toString());
        return DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), SolrSearchIndex.MAX_HITS, sortFields, null);
    }
}
