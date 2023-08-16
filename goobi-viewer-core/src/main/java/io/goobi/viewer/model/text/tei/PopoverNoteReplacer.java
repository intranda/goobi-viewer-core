package io.goobi.viewer.model.text.tei;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;

public class PopoverNoteReplacer implements NoteReplacer {

    private static final String NOTE_REGEX = "<span>([\\W\\w]*?)<note>\\s*<term>([\\W\\w]*?)<\\/term>([\\W\\w]*?)<\\/note>\\s*<\\/span>";
    private static final String NOTE_REPLACEMENT = "<a data-toggle=\"popover\" data-trigger=\"hover\" title=\"{title}\" data-content=\"{content}\">{word}</a>";
    
    
    @Override
    public String replaceNotes(String text) {

        Pattern p = Pattern.compile(NOTE_REGEX);
        Matcher m = p.matcher(text);
        
        
        while(m.find()) {
            String toReplace = m.group();
            String word = m.group(1);
            String title = m.group(2);
            String content = m.group(3);
            
            content = StringEscapeUtils.escapeHtml(content);
            
            String replacement = NOTE_REPLACEMENT.replace("{title}", title).replace("{content}", content).replace("{word}", word);
            text = text.replace(toReplace, replacement);
        }
        return text;
    }

}
