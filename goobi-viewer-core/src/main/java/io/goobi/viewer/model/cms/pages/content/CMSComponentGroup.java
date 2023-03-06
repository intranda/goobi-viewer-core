package io.goobi.viewer.model.cms.pages.content;

import java.util.ArrayList;
import java.util.List;

public class CMSComponentGroup {

    private final String name;
    private final List<CMSComponent> components = new ArrayList<>();
    
    public CMSComponentGroup(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public List<CMSComponent> getComponents() {
        return components;
    }
    
    public void addComponent(CMSComponent component) {
        this.components.add(component);
    }
}
