package io.goobi.viewer.model.text.tei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FootnoteReplacer implements NoteReplacer {
    
    /**
     * 
     */
    private static final String SB_FOOTNOTE_REGEX =
            "<note(\\s+type=\"(footnote|endnote|gloss)\")?(\\s+n=\"([\\w\\d]+)\")?>(\\s*<p>\\s*)?([\\w\\W]+?)(\\s*<\\/p>\\s*)?<\\/note>";
    private static final String EDITORIAL_FOOTNOTE_REGEX =
            "<note(\\s+type=\"(editorial)\")(\\s+n=\"([\\w\\d]+)\")?>(\\s*<p>\\s*)?([\\w\\W]+?)(\\s*<\\/p>\\s*)?<\\/note>";

    /**
     * @param text
     * @return
     */
    public String replaceNotes(String text) {
        //Replace <note>
        int footnoteCount = 0;
        for (MatchResult r : TeiToHtmlConverter.findRegexMatches(SB_FOOTNOTE_REGEX, text)) {
            String note = r.group();
            String footnoteText = r.group(6);
            Optional<String> n = Optional.ofNullable(r.group(4));

            if (!n.isPresent()) {
                footnoteCount++;
            }

            String reference = "<sup><a href=\"#fn§§\" id=\"ref§§\">§§</a></sup>";
            String footnote = "<p class=\"footnoteText\" id=\"fn§§\">[§§] " + footnoteText + "<a href=\"#ref§§\">↩</a></p>";
            reference = reference.replace("§§", n.orElse(Integer.toString(footnoteCount)));
            footnote = footnote.replace("§§", n.orElse(Integer.toString(footnoteCount)));

            text = text.replace(note, reference) + footnote;
        }
        text = text + "<br />";
        for (MatchResult r : TeiToHtmlConverter.findRegexMatches(EDITORIAL_FOOTNOTE_REGEX, text)) {
            String note = r.group();
            String footnoteText = r.group(6);
            Optional<String> n = Optional.ofNullable(r.group(4));

            if (!n.isPresent()) {
                footnoteCount++;
            }

            String reference = "<sup><a href=\"#fn§§\" id=\"ref§§\">§§</a></sup>";
            String footnote = "<p class=\"footnoteText\" id=\"fn§§\">[§§] " + footnoteText + "<a href=\"#ref§§\">↩</a></p>";
            reference = reference.replace("§§", n.orElse(Integer.toString(footnoteCount)));
            footnote = footnote.replace("§§", n.orElse(Integer.toString(footnoteCount)));

            text = text.replace(note, reference) + footnote;
        }
        return text;
    }

}
