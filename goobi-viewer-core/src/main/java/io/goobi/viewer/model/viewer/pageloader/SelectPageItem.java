package io.goobi.viewer.model.viewer.pageloader;

import javax.faces.model.SelectItem;

public class SelectPageItem extends SelectItem {

    private boolean doublePageMode = false;
    
    /**
     * 
     */
    public SelectPageItem() {
        super();
    }
    
    public void setDoublePageMode(boolean doublePageMode) {
        this.doublePageMode = doublePageMode;
    }
    
    public boolean isDoublePageMode() {
        return doublePageMode;
    }
    
    @Override
    public String getLabel() {
        // TODO Auto-generated method stub
        return super.getLabel();
    }
    
    @Override
    public Object getValue() {
        String value = super.getValue().toString();
        if(doublePageMode) {
            return value + "-" + value;
        } else {
            return value;
        }
    }

}
