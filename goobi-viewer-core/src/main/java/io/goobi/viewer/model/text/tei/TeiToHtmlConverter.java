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
package io.goobi.viewer.model.text.tei;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.ocr.tei.convert.HtmlToTEIConvert.ConverterMode;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.managedbeans.utils.BeanUtils;

public class TeiToHtmlConverter {

    private static final Logger logger = LoggerFactory.getLogger(TeiToHtmlConverter.class);

    private final ConverterMode mode;
    private final NoteReplacer noteReplacer;
    private final Map<Integer, Integer> hierarchyLevels = new HashMap<>();

    /**
     * 
     * @param mode
     * @param noteReplacer
     */
    public TeiToHtmlConverter(ConverterMode mode, NoteReplacer noteReplacer) {
        this.mode = mode;
        this.noteReplacer = noteReplacer;
    }

    /**
     * 
     * @param text
     * @should map hierarchy levels correctly
     */
    void populateHierarchyLevels(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text may not be null");
        }

        hierarchyLevels.clear();
        final Set<Integer> divOpenedIndexes = new HashSet<>();
        final Set<Integer> divClosedIndexes = new HashSet<>();
        for (MatchResult r : findRegexMatches("<div.*?>", text)) {
            divOpenedIndexes.add(r.start());
            //            logger.trace("start at {}-{}", r.start(), r.end());
        }
        for (MatchResult r : findRegexMatches("</div>", text)) {
            divClosedIndexes.add(r.end() - 1);
            // logger.trace("end at {}-{}", r.start(), r.end() - 1);
        }
        // logger.trace(text);
        List<Integer> allIndexes = new ArrayList<>();
        allIndexes.addAll(divOpenedIndexes);
        allIndexes.addAll(divClosedIndexes);
        if (allIndexes.isEmpty()) {
            return;
        }
        Collections.sort(allIndexes);
        int level = 0;
        logger.trace("index: {}-{}", allIndexes.get(0), allIndexes.get(allIndexes.size() - 1));
        for (int i = allIndexes.get(0); i <= allIndexes.get(allIndexes.size() - 1); ++i) {
            if (divOpenedIndexes.contains(i)) {
                level++;
                // logger.trace("increase at {}: {}", i, level);
            } else if (divClosedIndexes.contains(i)) {
                level--;
                // logger.trace("decrease at {}: {}", i, level);
            }
            hierarchyLevels.put(i, level);
        }

    }

    int getHierarchyLevel(int index) {
        // logger.trace("getHierarchyLevel: {}", index);
        return hierarchyLevels.get(index);
    }

    public String convert(String text, String language) {
        text = removeUrlEncoding(text);
        text = TeiToHtmlConverter.removeComments(text);

        // Remove TEI namespace from divs
        text = text.replace(" xmlns=\"http://www.tei-c.org/ns/1.0\"", "");

        // Remove type attributes from divs
        //        text = text.replaceAll("<div type=\"(.*?)\">", "<div>");

        // remove empty <p>'s
        // text = text.replace("<p />", "").replace("<p/>", "").replace("<p></p>", "");

        // images
        for (MatchResult r : findRegexMatches("<figure.*?>[\\s\\S]*?</figure>", text)) {
            String xmlSnippet = r.group();
            try {
                Document doc = XmlTools.getDocumentFromString(xmlSnippet, StringTools.DEFAULT_ENCODING);
                if (doc != null && doc.hasRootElement()) {
                    StringBuilder sb = new StringBuilder();
                    // Image header
                    String head = doc.getRootElement()
                            .getChildText("head");
                    if (head != null) {
                        sb.append("<h4>")
                                .append(head)
                                .append("</h4>");
                    }
                    Element eleGraphic = doc.getRootElement()
                            .getChild("graphic");
                    StringBuilder imageTextBuilder = new StringBuilder();
                    if (eleGraphic != null && eleGraphic.getAttribute("url") != null) {
                        // <graphic> exists
                        sb.append("<img src=\"")
                                .append(eleGraphic.getAttributeValue("url"))
                                .append('"');
                        if (doc.getRootElement()
                                .getChild("figDesc") != null) {
                            String figDesc = doc.getRootElement()
                                    .getChildText("figDesc");
                            sb.append(" alt=\"")
                                    .append(figDesc)
                                    .append('"');
                        }
                        sb.append(" />");
                    } else if (doc.getRootElement()
                            .getChild("figDesc") != null) {
                        // no <graphic> found, use <figDesc>
                        NavigationHelper nh = BeanUtils.getNavigationHelper();
                        sb.append("<img class=\"img-responsive img-placeholder-tei\" src=\"")
                                .append(nh != null ? nh.getApplicationUrl() : "/")
                                .append("resources/themes/geiwv/images/geiwv_placeholder.jpg")
                                .append("\"/>");
                        if (doc.getRootElement()
                                .getChild("figDesc") != null) {
                            String figDesc = doc.getRootElement()
                                    .getChildText("figDesc");
                            imageTextBuilder.append("<p class=\"img-placeholder-tei-desc\">")
                                    .append(figDesc)
                                    .append("</p>");
                        }
                    }
                    // <p> elements
                    List<Element> eleListP = doc.getRootElement()
                            .getChildren("p");
                    if (eleListP != null && !eleListP.isEmpty()) {
                        for (Element eleP : eleListP) {
                            String p = XmlTools.getStringFromElement(eleP, StringTools.DEFAULT_ENCODING);
                            imageTextBuilder.append(p.replace("<lb/>", "<br/>")
                                    .replace("<lb />", "<br/>"));
                        }
                    }
                    if (StringUtils.isNotBlank(imageTextBuilder.toString())) {
                        sb.append("<span class=\"img-placeholder-tei-text\">")
                                .append(imageTextBuilder.toString())
                                .append("</span>");
                    }

                    // Replace the entire block with the new HTML code
                    text = text.replace(r.group(), sb.toString());
                }
            } catch (IOException | JDOMException e) {
                logger.error(e.getMessage(), e);
            }
        }

        // headers (after images)
        populateHierarchyLevels(text);
        for (MatchResult r : findRegexMatches("<head>[\\s]*([\\s\\S]*?)[\\s]*</head>", text)) {
            int level = getHierarchyLevel(r.start());
            // Replace <head> tags with placeholder tags of the same length so that the hierarchy level indexes are still valid
            text = text.replace(r.group(), "<~h~" + level + ">" + r.group(1) + "</~h~" + level + ">");
        }
        // replace placeholders with <h> tags
        text = text.replace("~h~", "h");

        // bold+italic+underline
        for (MatchResult r : findRegexMatches(
                "<hi rend=\"bold\">[\\s]*<hi rend=\"italic\">[\\s]*<hi rend=\"underline\">([\\s\\S]*?)</hi>[\\s]*</hi>[\\s]*</hi>", text)) {
            text = text.replace(r.group(), "<strong><hi rend=\"italic\"><hi rend=\"underline\">" + r.group(1) + "</hi></hi></strong>");
        }
        // bold+italic
        for (MatchResult r : findRegexMatches("<hi rend=\"bold\">[\\s]*<hi rend=\"italic\">[\\s]*([\\s\\S]*?)[\\s]*</hi>[\\s]*</hi>", text)) {
            text = text.replace(r.group(), "<strong><hi rend=\"italic\">" + r.group(1) + "</hi></strong>");
        }
        // bold
        for (MatchResult r : findRegexMatches("<hi rend=\"bold\">[\\s]*([\\s\\S]*?)[\\s]*</hi>", text)) {
            text = text.replace(r.group(), "<strong>" + r.group(1) + "</strong>");
        }
        // italic
        for (MatchResult r : findRegexMatches("<hi rend=\"italic\">[\\s]*([\\s\\S]*?)[\\s]*</hi>", text)) {
            text = text.replace(r.group(), "<em>" + r.group(1) + "</em>");
        }
        // underline
        for (MatchResult r : findRegexMatches("<hi rend=\"underline\">[\\s]*([\\s\\S]*?)[\\s]*</hi>", text)) {
            text = text.replace(r.group(), "<span style=\"text-decoration: underline;\">" + r.group(1) + "</span>");
        }
        // strikethrough
        for (MatchResult r : findRegexMatches("<hi rend=\"strikethrough\">[\\s]*([\\s\\S]*?)[\\s]*</hi>", text)) {
            text = text.replace(r.group(), "<s>" + r.group(1) + "</s>");
        }
        // blockCapitals
        for (MatchResult r : findRegexMatches("<hi rend=\"blockCapitals\">[\\s]*([\\s\\S]*?)[\\s]*</hi>", text)) {
            text = text.replace(r.group(), "<span style=\"text-transform: uppercase;\">" + r.group(1) + "</span>");
        }
        // smallCapitals
        for (MatchResult r : findRegexMatches("<hi rend=\"smallCapitals\">[\\s]*([\\s\\S]*?)[\\s]*</hi>", text)) {
            text = text.replace(r.group(), "<span style=\"font-variant: small-caps;\">" + r.group(1) + "</span>");
        }
        // TODO spaceOut

        // TODO replace anm
        for (MatchResult r : findRegexMatches("\\[anm\\](.*?)\\[/anm\\]", text)) {
            text = text.replace(r.group(), "<note type=\"editorial\"><p>" + r.group(1) + "</p></note>");
        }

        // tables
        //        text = text.replaceAll("<table.*?>", "<table>").replace("<tbody>", "").replace("</tbody>", "");
        // text = text.replace("<head>", "<h2>").replace("</head>", "</h2>");
        //        text = text.replace("<tbody>", "").replace("</tbody>", "");
        //        text = text.replace("<thead>", "").replace("</thead>", "");
        text = text.replaceAll("<row>", "<tr>")
                .replace("<tr>", "<row>")
                .replace("</row>", "</tr>");
        text = text.replaceAll("<cell>", "<td>")
                .replace("</cell>", "</td>");

        //  lists
        //            text = text.replaceAll("<list rend=\"bulleted\">", "<ul>").replace("</list>", "</ul>");
        for (MatchResult r : findRegexMatches("<list rend=\"bulleted\">(.*?)</list>", text)) {
            text = text.replace(r.group(), "<ul>" + r.group(1) + "</ul>");
        }
        text = text.replace("<item>", "<li>")
                .replace("</item>", "</li>");
        //            text = text.replaceAll("<list rend=\"alphabetical\">", "<ol style=\"list-style-type: alpha\">").replace("</list>", "</ol>");
        //            text = text.replaceAll("<list rend=\"alphabetical\">", "<ol.*?style=\".*?-greek.*?>").replace( "</list>", "</ol>");
        for (MatchResult r : findRegexMatches("<list rend=\"alphabetical\">(.*?)</list>", text)) {
            text = text.replace(r.group(), "<ol style=\"list-style-type: alpha\">" + r.group(1) + "</ol>");
        }
        //            text = text.replaceAll("<list rend=\"numbered\">", "<ol>").replace("</list>", "</ol>");
        for (MatchResult r : findRegexMatches("<list rend=\"numbered\">(.*?)</list>", text)) {
            text = text.replace(r.group(), "<ol>" + r.group(1) + "</ol>");
        }

        // listBibl
        for (MatchResult r : findRegexMatches("<listBibl>[\\s]*([\\s\\S]*?)[\\s]*</listBibl>", text)) {
            text = text.replace(r.group(), "<ul>" + r.group(1) + "</ul>");
        }
        for (MatchResult r : findRegexMatches("<bibl>[\\s]*([\\s\\S]*?)[\\s]*</bibl>", text)) {
            text = text.replace(r.group(), "<li>" + r.group(1) + "</li>");
        }

        // TODO Blockquote (old)
        for (MatchResult r : findRegexMatches("<blockquote>\\s*<p>\\[Q=(.*?)\\](.*?)\\[/Q\\]</p>\\s*</blockquote>", text)) {
            text = text.replace(r.group(), "<cit><q source=\"#" + r.group(1) + "\">" + r.group(2) + "</q></cit>");
        }

        // TODO Blockquote (with reference)
        int quoteRefCounter = 1;
        for (MatchResult r : findRegexMatches("<blockquote\\s+cite=\"(.*?)\">\\s*([\\s\\S]*?)\\s*<\\/blockquote>", text)) {
            StringBuilder replacement = new StringBuilder();
            replacement.append("<cit> ")
                    .append(mode.equals(ConverterMode.resource) ? "<q" : "<quote")
                    .append(" source=\"#quoteref")
                    .append(quoteRefCounter)
                    .append("\">")
                    .append(r.group(2))
                    .append(mode.equals(ConverterMode.resource) ? "</q>" : "</quote>")
                    .append(" <ref type=\"bibl\" xml:id=\"quoteref")
                    .append(quoteRefCounter)
                    .append("\" target=\"#ref")
                    .append(quoteRefCounter)
                    .append("\">")
                    .append(r.group(1))
                    .append("</ref>")
                    .append("</cit>");
            text = text.replace(r.group(), replacement.toString());
            quoteRefCounter++;
        }

        // TODO Blockquote (no reference)
        //		for (MatchResult r : findRegexMatches("<blockquote>\\s*(<p>)*([\\s\\S]*?)(<\\/p>)*\\s*<\\/blockquote>",
        for (MatchResult r : findRegexMatches("<blockquote>\\s*([\\s\\S]*?)\\s*<\\/blockquote>", text)) {
            StringBuilder replacement = new StringBuilder();
            replacement.append("<cit>")
                    .append(mode.equals(ConverterMode.resource) ? "<q>" : "<quote source=\"#\">")
                    //				.append( " source=\"#\"")
                    //				.append(">")
                    .append(r.group(1))
                    .append(mode.equals(ConverterMode.resource) ? "</q>" : "</quote>")
                    .append("</cit>");
            text = text.replace(r.group(), replacement.toString());
        }

        for (MatchResult r : findRegexMatches("\\[Q=(.*?)\\](.*?)\\[/Q\\]", text)) {
            text = text.replace(r.group(), "<q source=\"#" + r.group(1) + "\">" + r.group(2) + "</q>");
        }

        // TODO q with cite
        for (MatchResult r : findRegexMatches("<q\\s+cite=\"(.*?)\">([\\s\\S]*?)<\\/q>", text)) {
            if (mode.equals(ConverterMode.annotation)) {
                text = text.replace(r.group(),
                        "<quote source=\"#quoteref" + quoteRefCounter + "\" type=\"direct\">" + r.group(2) + "</quote>"
                                + "(<ref type=\"bibl\" xml:id=\"quoteref" + quoteRefCounter + "\" target=\"#ref" + quoteRefCounter + "\">"
                                + r.group(1) + "</ref>)");
            } else {
                text = text.replace(r.group(),
                        "<q source=\"#quoteref" + quoteRefCounter + "\" type=\"direct\">" + r.group(2) + "</q>"
                                + "(<ref type=\"bibl\" xml:id=\"quoteref" + quoteRefCounter + "\" target=\"#ref" + quoteRefCounter + "\">"
                                + r.group(1) + "</ref>)");
            }
            quoteRefCounter++;
        }

        for (MatchResult r : findRegexMatches("<q>(.*?)</q>", text)) {
            text = text.replace(r.group(), "[q]" + r.group(1) + "[/q]");
        }
        for (MatchResult r : findRegexMatches("\\[q\\](.*?)\\[/q\\]", text)) {
            text = text.replace(r.group(), "<q>" + r.group(1) + "</q>");
        }

        // Hyperlinks
        for (MatchResult r : findRegexMatches("<ref target=\"(.*?)>\\s*(.*?)\\s*</ref>", text)) {
            String href = r.group(1)
                    .startsWith("wvg:") ? "http://gei-worldviews.gei.de/glossary/#" + r.group(1) : r.group(1);
            text = text.replace(r.group(), "<a target=\"_blank\" href=\"" + href + "\">" + r.group(2) + "</a>");
        }

        //        for (MatchResult r : findRegexMatches("<a\\s*(\\w+=\".*\"\\s*)*>(.*?)</a>", text)) {
        //            text = text.replace(r.group(), r.group(2));
        //        }

        //column breaks
        String tableRegex = "(<cb type=\"start\"(\\/>|><\\/cb>))([\\w\\W]*?)(<cb type=\"end\"(\\/>|><\\/cb>))";
        String columnSwitchRegex = "<cb\\s*\\/>|<cb><\\/cb>";
        String tableStartString = "<table class=\"tei-text-table\"><tbody><tr><td>";
        //        String columnSwitchString = "</td><td>";
        String tableEndString = "</td></tr></tbody></table>";
        for (MatchResult r : findRegexMatches(tableRegex, text)) {
            String table = r.group();
            String tableText = r.group(3);
            int tableStartIndex = text.indexOf(r.group());

            tableText = tableStartString + tableText + tableEndString;
            tableText = tableText.replaceAll(columnSwitchRegex, "");

            text = text.replace(table, tableText);

        }

        // page breaks
        char pageChar = 'p';
        if ("ger".equals(language)) {
            pageChar = 'S';
        }
        for (MatchResult r : findRegexMatches("<pb n=\"(.*?)\"></pb>", text)) {

            text = text.replace(r.group(), "<p style=\"page-break-after: always;\"></p>\n<p style=\"page-break-before: always;\">[" + pageChar + ". "
                    + r.group(1) + "]</p>");
        }

        // Page numbers
        for (MatchResult r : findRegexMatches("<pb n=\"(.*?)\"\\s/>", text)) {
            if ("ger".equals(language)) {
                text = text.replace(r.group(), "[S. " + r.group(1) + "]");
            } else {
                text = text.replace(r.group(), "[p. " + r.group(1) + "]");
            }
        }

        //correct note attribute order
        String noteWrongAttributeOrderRegex = "<note\\s+(n=\"[\\w\\d]+\")\\s+(type=\"[\\w\\d]+\")>";
        String noteCorrectionString = "<note {2} {1}>";
        for (MatchResult r : findRegexMatches(noteWrongAttributeOrderRegex, text)) {
            text = text.replace(r.group(), noteCorrectionString.replace("{1}", r.group(1))
                    .replace("{2}", r.group(2)));
        }

        text = this.noteReplacer.replaceNotes(text);

        // line breaks
        text = text.replace("<lb />", "<br />")
                .replace("<lb></lb>", "<br />");

        // text = text.replace("<p />", "");

        return text.trim();

    }

    public static String removeComments(String text) {
        text = text.replaceAll("<!--[\\w\\W]*?-->", "");
        return text;
    }

    /**
     * @param text
     * @return
     * @throws IOException
     */
    public static String removeUrlEncoding(String text) {
        StringWriter writer = new StringWriter();
        try {
            StringEscapeUtils.unescapeHtml(writer, text);
            return writer.toString();
        } catch (IOException e) {
            logger.error(e.toString(), e);
            return text;
        }

        //		text = text.replace("&amp;", "&");
        //		text = text.replace("&Auml;", "Ä");
        //		text = text.replace("&Ouml;", "Ö");
        //		text = text.replace("&Uuml;", "Ü");
        //
        //		text = text.replace("&auml;", "ä");
        //		text = text.replace("&ouml;", "ö");
        //		text = text.replace("&uuml;", "ü");
        //
        //		text = text.replace("&szlig;", "ß");
        //		text = text.replace("&nbsp;", "");
        //		text = text.replace("&shy;", "-");
        //		return text;
    }

    public static Iterable<MatchResult> findRegexMatches(String pattern, CharSequence s) {
        List<MatchResult> results = new ArrayList<>();
        for (Matcher m = Pattern.compile(pattern)
                .matcher(s); m.find();) {
            results.add(m.toMatchResult());
        }
        return results;
    }
}
