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

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TeiToHtmlConverter {

    /**
     * 
     */
    private static final String SB_FOOTNOTE_REGEX = "<note(\\s+type=\"(footnote|endnote|gloss)\")?(\\s+n=\"([\\w\\d]+)\")?>(\\s*<p>\\s*)?([\\w\\W]+?)(\\s*<\\/p>\\s*)?<\\/note>";
    private static final String EDITORIAL_FOOTNOTE_REGEX = "<note(\\s+type=\"(editorial)\")(\\s+n=\"([\\w\\d]+)\")?>(\\s*<p>\\s*)?([\\w\\W]+?)(\\s*<\\/p>\\s*)?<\\/note>";

    public static enum ConverterMode {
        annotation,
        resource
    }

    private static final Logger logger = LoggerFactory.getLogger(TeiToHtmlConverter.class);

    private final ConverterMode mode;
    private final Map<Integer, Integer> hierarchyLevels = new HashMap<>();

    public TeiToHtmlConverter(ConverterMode mode) {
        this.mode = mode;
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

    public String convert(String text) {
        text = removeUrlEncoding(text);
        text = TeiToHtmlConverter.removeComments(text);

        // Remove TEI namespace from divs
        text = text.replace(" xmlns=\"http://www.tei-c.org/ns/1.0\"", "");

        // Remove type attributes from divs
        //        text = text.replaceAll("<div type=\"(.*?)\">", "<div>");

        // remove empty <p>'s
        // text = text.replace("<p />", "").replace("<p/>", "").replace("<p></p>", "");

        // images
        // <img src="none" alt="Bildbeschriftung" />
        //        for (MatchResult r : findRegexMatches("<figure.*?>\\s*<head.*?>(.*?)</head>[\\s\\S]*?<graphic url=\"(.*?)\" */>\\s*</figure>", text)) {
        //            text = text.replace(r.group(), "\"<img src=\"" + r.group(2) + "\" alt=\"" + r.group(1) + "\"/>");
        //        }
        for (MatchResult r : findRegexMatches("<figure.*?>[\\s\\S]*?</figure>", text)) {
            String xmlSnippet = r.group();
            try {
                Document doc = FileTools.getDocumentFromString(xmlSnippet, Helper.DEFAULT_ENCODING);
                if (doc != null && doc.hasRootElement()) {
                    StringBuilder sb = new StringBuilder();
                    Element eleGraphic = doc.getRootElement().getChild("graphic");
                    if (eleGraphic != null && eleGraphic.getAttribute("url") != null) {
                        sb.append("<img src=\"").append(eleGraphic.getAttributeValue("url")).append('"');
                        String head = doc.getRootElement().getChildText("head");
                        if (head != null) {
                            sb.append(" alt=\"").append(head).append('"');
                        }
                        sb.append(" />");
                    } else if (doc.getRootElement().getChild("figDesc") != null) {
                        String figDesc = doc.getRootElement().getChildText("figDesc");
                        sb.append("<p>").append(figDesc).append("</p>");
                    }
                    text = text.replace(r.group(), sb.toString());
                }
            } catch (JDOMException e) {
                logger.error(e.getMessage(), e);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        // headers (after images)
        populateHierarchyLevels(text);
        for (MatchResult r : findRegexMatches("<head>[\\s]*(.*?)[\\s]*</head>", text)) {
            int level = getHierarchyLevel(r.start());
            // Replace <head> tags with placeholder tags of the same length so that the hierarchy level indexes are still valid
            text = text.replace(r.group(), "<~h~" + level + ">" + r.group(1) + "</~h~" + level + ">");
        }
        // replace placeholders with <h> tags
        text = text.replace("~h~", "h");

        // bold+italic+underline
        for (MatchResult r : findRegexMatches(
                "<hi rend=\"bold\">[\\s]*<hi rend=\"italic\">[\\s]*<hi rend=\"underline\">([\\s\\S]*?)</hi>[\\s]*</hi>[\\s]*</hi>", text)) {
            text = text.replace(r.group(), "<strong><em><span style=\"text-decoration: underline;\">" + r.group(1) + "</span></em></strong>");
        }
        // bold+italic
        for (MatchResult r : findRegexMatches("<hi rend=\"bold\">[\\s]*<hi rend=\"italic\">[\\s]*([\\s\\S]*?)[\\s]*</hi>[\\s]*</hi>", text)) {
            text = text.replace(r.group(), "<strong><em>" + r.group(1) + "</em></strong>");
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
        text = text.replaceAll("<row>", "<tr>").replace("<tr>", "<row>").replace("</row>", "</tr>");
        text = text.replaceAll("<cell>", "<td>").replace("</cell>", "</td>");

        //  lists
        //            text = text.replaceAll("<list rend=\"bulleted\">", "<ul>").replace("</list>", "</ul>");
        for (MatchResult r : findRegexMatches("<list rend=\"bulleted\">(.*?)</list>", text)) {
            text = text.replace(r.group(), "<ul>" + r.group(1) + "</ul>");
        }
        text = text.replace("<item>", "<li>").replace("</item>", "</li>");
        //            text = text.replaceAll("<list rend=\"alphabetical\">", "<ol style=\"list-style-type: alpha\">").replace("</list>", "</ol>");
        //            text = text.replaceAll("<list rend=\"alphabetical\">", "<ol.*?style=\".*?-greek.*?>").replace( "</list>", "</ol>");
        for (MatchResult r : findRegexMatches("<list rend=\"alphabetical\">(.*?)</list>", text)) {
            text = text.replace(r.group(), "<ol style=\"list-style-type: alpha\">" + r.group(1) + "</ol>");
        }
        //            text = text.replaceAll("<list rend=\"numbered\">", "<ol>").replace("</list>", "</ol>");
        for (MatchResult r : findRegexMatches("<list rend=\"numbered\">(.*?)</list>", text)) {
            text = text.replace(r.group(), "<ol>" + r.group(1) + "</ol>");
        }

        // TODO Blockquote (old)
        for (MatchResult r : findRegexMatches("<blockquote>\\s*<p>\\[Q=(.*?)\\](.*?)\\[/Q\\]</p>\\s*</blockquote>", text)) {
            text = text.replace(r.group(), "<cit><q source=\"#" + r.group(1) + "\">" + r.group(2) + "</q></cit>");
        }

        // TODO Blockquote (with reference)
        int quoteRefCounter = 1;
        for (MatchResult r : findRegexMatches("<blockquote\\s+cite=\"(.*?)\">\\s*([\\s\\S]*?)\\s*<\\/blockquote>", text)) {
            StringBuilder replacement = new StringBuilder();
            replacement.append("<cit> ").append(mode.equals(ConverterMode.resource) ? "<q" : "<quote").append(" source=\"#quoteref").append(
                    quoteRefCounter).append("\">").append(r.group(2)).append(mode.equals(ConverterMode.resource) ? "</q>" : "</quote>").append(
                            " <ref type=\"bibl\" xml:id=\"quoteref").append(quoteRefCounter).append("\" target=\"#ref").append(quoteRefCounter)
                    .append("\">").append(r.group(1)).append("</ref>").append("</cit>");
            text = text.replace(r.group(), replacement.toString());
            quoteRefCounter++;
        }

        // TODO Blockquote (no reference)
        //		for (MatchResult r : findRegexMatches("<blockquote>\\s*(<p>)*([\\s\\S]*?)(<\\/p>)*\\s*<\\/blockquote>",
        for (MatchResult r : findRegexMatches("<blockquote>\\s*([\\s\\S]*?)\\s*<\\/blockquote>", text)) {
            StringBuilder replacement = new StringBuilder();
            replacement.append("<cit>").append(mode.equals(ConverterMode.resource) ? "<q>" : "<quote source=\"#\">")
                    //				.append( " source=\"#\"")
                    //				.append(">")
                    .append(r.group(1)).append(mode.equals(ConverterMode.resource) ? "</q>" : "</quote>").append("</cit>");
            text = text.replace(r.group(), replacement.toString());
        }

        for (MatchResult r : findRegexMatches("\\[Q=(.*?)\\](.*?)\\[/Q\\]", text)) {
            text = text.replace(r.group(), "<q source=\"#" + r.group(1) + "\">" + r.group(2) + "</q>");
        }

        // TODO q with cite
        for (MatchResult r : findRegexMatches("<q\\s+cite=\"(.*?)\">([\\s\\S]*?)<\\/q>", text)) {
            if (mode.equals(ConverterMode.annotation)) {
                text = text.replace(r.group(), "<quote source=\"#quoteref" + quoteRefCounter + "\" type=\"direct\">" + r.group(2) + "</quote>"
                        + "(<ref type=\"bibl\" xml:id=\"quoteref" + quoteRefCounter + "\" target=\"#ref" + quoteRefCounter + "\">" + r.group(1)
                        + "</ref>)");
            } else {
                text = text.replace(r.group(), "<q source=\"#quoteref" + quoteRefCounter + "\" type=\"direct\">" + r.group(2) + "</q>"
                        + "(<ref type=\"bibl\" xml:id=\"quoteref" + quoteRefCounter + "\" target=\"#ref" + quoteRefCounter + "\">" + r.group(1)
                        + "</ref>)");
            }
            quoteRefCounter++;
        }

        for (MatchResult r : findRegexMatches("<q>(.*?)</q>", text)) {
            text = text.replace(r.group(), "[q]" + r.group(1) + "[/q]");
        }
        for (MatchResult r : findRegexMatches("\\[q\\](.*?)\\[/q\\]", text)) {
            text = text.replace(r.group(), "<q>" + r.group(1) + "</q>");
        }

        for (MatchResult r : findRegexMatches("<a\\s*(\\w+=\".*\"\\s*)*href=\"(.*)\">(.*)<\\/a>", text)) {
            text = text.replace(r.group(), "<ref target=\"" + r.group(2) + "\" type=\"url\">" + r.group(3) + "</ref>");
        }

        for (MatchResult r : findRegexMatches("<a\\s*(\\w+=\".*\"\\s*)*>(.*?)</a>", text)) {
            text = text.replace(r.group(), r.group(2));
        }

        // page breaks
        for (MatchResult r : findRegexMatches("<pb n=\"(.*?)\"></pb>", text)) {
            text = text.replace(r.group(), "<p style=\"page-break-after: always;\"></p>\n<p style=\"page-break-before: always;\">" + r.group(1)
                    + "</p>");
        }

        
        
        //Replace <note>
        int footnoteCount = 0;
        for (MatchResult r : findRegexMatches(SB_FOOTNOTE_REGEX, text)) {
            String note = r.group();
            String footnoteText = r.group(6);
            Optional<String> n = Optional.ofNullable(r.group(4));
            
            if(!n.isPresent()) {                
                footnoteCount++;
            }
            
            String reference = "<sup><a href=\"#fn§§\" id=\"ref§§\">§§</a></sup>";
            String footnote = "<p class=\"footnoteText\" id=\"fn§§\">[§§] " + footnoteText + "<a href=\"#ref§§\">↩</a></p>";
            reference = reference.replace("§§", n.orElse(Integer.toString(footnoteCount)));
            footnote = footnote.replace("§§", n.orElse(Integer.toString(footnoteCount)));

            text = text.replace(note, reference) + footnote;
        }
        text = text + "<br />";
        for (MatchResult r : findRegexMatches(EDITORIAL_FOOTNOTE_REGEX, text)) {
            String note = r.group();
            String footnoteText = r.group(6);
            Optional<String> n = Optional.ofNullable(r.group(4));
            
            if(!n.isPresent()) {                
                footnoteCount++;
            }
            
            String reference = "<sup><a href=\"#fn§§\" id=\"ref§§\">§§</a></sup>";
            String footnote = "<p class=\"footnoteText\" id=\"fn§§\">[§§] " + footnoteText + "<a href=\"#ref§§\">↩</a></p>";
            reference = reference.replace("§§", n.orElse(Integer.toString(footnoteCount)));
            footnote = footnote.replace("§§", n.orElse(Integer.toString(footnoteCount)));

            text = text.replace(note, reference) + footnote;
        }

        // line breaks
        text = text.replace("<lb />", "<br />").replace("<lb></lb>", "<br />");

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
        for (Matcher m = Pattern.compile(pattern).matcher(s); m.find();) {
            results.add(m.toMatchResult());
        }
        return results;
    }
}
