package io.goobi.viewer.model.iiif.search.parser;

public class MatchGroup {

    public final int start;
    public final int end;
    public final String text;
    
    public MatchGroup(int start, int end, String text) {
        this.start = start;
        this.end = end;
        this.text = text;
    }
}
