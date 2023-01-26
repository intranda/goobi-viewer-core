package io.goobi.viewer.model.search;

public enum HitListView {

    DETAILS("searchListDetailView", "details"),
    TILES("searchListTileView", "grid"),
    LIST("searchListListView", "list-view");
    
    private final String cssClass;
    private final String label;
    
    private HitListView(String label, String cssClass) {
        this.cssClass = cssClass;
        this.label = label;
    }
    
    public String getCssClass() {
        return cssClass;
    }
    
    public String getLabel() {
        return label;
    }
    
}
