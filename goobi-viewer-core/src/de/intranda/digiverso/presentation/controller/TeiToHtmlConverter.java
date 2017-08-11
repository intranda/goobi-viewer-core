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
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

public class TeiToHtmlConverter {

    private static final Logger logger = Logger.getLogger(TeiToHtmlConverter.class);

    private static final int HEADER_HIERARCHY_DEPTH = 9;
    private static final String HEADER_DIV_REGEX = "(<hx[\\S\\s]*?)(?=((<h\\d)|$))";

    private ConverterMode mode;

    public TeiToHtmlConverter(ConverterMode mode) {
        this.mode = mode;
    }

    public String convert(String text) {
        text = removeUrlEncoding(text);
        text = TeiToHtmlConverter.removeComments(text);
        // text = "<div xmlns=\"http://www.tei-c.org/ns/1.0\">" + text + "</div>";

        for (int i = HEADER_HIERARCHY_DEPTH; i > 0; i--) {
            String regex = HEADER_DIV_REGEX.replace("x", Integer.toString(i));
            for (MatchResult r : findRegexMatches(regex, text)) {
                text = text.replace(r.group(), "<div>" + r.group() + "</div>");
            }
            // TODO replace header
            for (MatchResult r : findRegexMatches("<h" + i + ".*?>(.*?)</h" + i + ">", text)) {
                text = text.replace(r.group(), "<head>" + r.group(1) + "</head>");
            }
        }

        // remove empty <p>'s
        // text = text.replace("<p />", "").replace("<p/>", "").replace("<p></p>", "");

        // replace bold
        for (MatchResult r : findRegexMatches("<hi rend=\"bold\">(.*?)</hi>", text)) {
            text = text.replace(r.group(), "<strong>" + r.group(1) + "</strong>");
        }
        // replace italic
        for (MatchResult r : findRegexMatches("<hi rend=\"italic\">(.*?)</hi>", text)) {
            text = text.replace(r.group(), "<em>" + r.group(1) + "</em>");
        }
        // replace underline
        for (MatchResult r : findRegexMatches("<hi rend=\"underline\">(.*?)</hi>", text)) {
            text = text.replace(r.group(), "<span style=\"text-decoration: underline;\">" + r.group(1) + "</span>");
        }

        // TODO replace anm
        for (MatchResult r : findRegexMatches("\\[anm\\](.*?)\\[/anm\\]", text)) {
            text = text.replace(r.group(), "<note type=\"editorial\"><p>" + r.group(1) + "</p></note>");
        }

        // tables
        //        text = text.replaceAll("<table.*?>", "<table>").replace("<tbody>", "").replace("</tbody>", "");
        text = text.replace("<head>", "<caption>").replace("</head>", "</caption>");
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

        // images
        // <img src="none" alt="Bildbeschriftung" />
        for (MatchResult r : findRegexMatches("<figure><head>(.*?)</head><graphic url=\"(.*?)\" /></figure>", text)) {
            text = text.replace(r.group(), "\"<img src=\"" + r.group(2) + "\" alt=\"" + r.group(1) + "\"/>");
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

        // text = text.replace("<br />", "");
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

    public static enum ConverterMode {
        annotation,
        resource
    }

}
