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
package io.goobi.viewer.faces.components;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.component.UISelectItems;
import javax.faces.component.html.HtmlSelectBooleanCheckbox;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;

import com.sun.faces.facelets.component.UIRepeat;

import io.goobi.viewer.model.cms.Selectable;

/**
 * @author florian
 *
 */
@FacesComponent("io.goobi.viewer.faces.components.MultiSelectDropdown")
@SuppressWarnings("unchecked")
public class MultiSelectDropdown extends UINamingContainer {

    enum PropertyKeys {
        values, selection
    }
    

    public MultiSelectDropdown() {
        super();
    }
    
    /* (non-Javadoc)
     * @see javax.faces.component.UIComponentBase#encodeBegin(javax.faces.context.FacesContext)
     */
    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        super.encodeBegin(context);
         
        List<Object> values = (List<Object>)getValueExpression("value").getValue(context.getELContext());
        setValues(values);
        List<String> options = (List<String>)getValueExpression("items").getValue(context.getELContext());
//        SelectionManager<String> selection = new SelectionManager<>(options);
//        setSelection(selection);

    }
    
    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        super.encodeEnd(context);
//        visitTree(context);

        
    }

//    private Selectable createSelectable(Object object) {
////        SelectableWithCallback selectable = new SelectableWithCallback(object, values.contains(object), (item, selected) -> setItemSelected(item, selected));
//        Selectable selectable = new Selectable<Object>(object,  values.contains(object));
//        return selectable;
//    }
    
    /**
     * @param context
     */
//    private void visitTree(FacesContext context) {
//        VisitCallback visitor = new VisitCallback() {
//            
//            Iterator<Selectable> itemIterator = getItems().iterator();
//            
//            @SuppressWarnings("unused")
//            @Override
//            public VisitResult visit(VisitContext context, UIComponent target) {
////                System.out.println("Visit component of type " + target.getClass() + " and id " + target.getClientId());
////                
//                if(target instanceof UISelectItems) { 
//                    UISelectItems items = (UISelectItems)target;
//                    Object value = items.getValueExpression("value").getValue(context.getFacesContext().getELContext());
//                    Object itemValue = items.getValueExpression("itemValue").getValue(context.getFacesContext().getELContext());
//                    Object itemLabel = items.getValueExpression("itemLabel").getValue(context.getFacesContext().getELContext());
//                    Object var = items.getValueExpression("var").getValue(context.getFacesContext().getELContext());
//                }  else if(target instanceof HtmlSelectBooleanCheckbox) {
//                    HtmlSelectBooleanCheckbox checkbox = (HtmlSelectBooleanCheckbox)target;
//                    if(itemIterator.hasNext()) {                        
//                        Selectable item = itemIterator.next(); 
//                        checkbox.setSelected(item.isSelected());
//                        System.out.println("Set checkbox " + checkbox.getClientId() + "to " + item.isSelected());
//                        ((HtmlSelectBooleanCheckbox) target).addValueChangeListener((event) -> {
//                            boolean value = (boolean) event.getNewValue();
//                            boolean oldValue = (boolean) event.getOldValue();
//                            if(value != oldValue) {                                
//                                setItemSelected(item, value);
//                            }
//                        });
//                    }
//                    
//                }
//                
//                return VisitResult.ACCEPT;
//            }
//        };
//        
//        this.visitTree(VisitContext.createVisitContext(context), visitor);
//    }
    
    public void onSelectItem(ValueChangeEvent event) {
        System.out.println("Value changed on " + event.getComponent().getClientId() + " to " + event.getNewValue());
    }
    
    public boolean isItemSelected(Object item) {
        return getValues().contains(item);
    }

    public void setItemSelected(Object item, boolean selected) {
        List<Object> values = getValues();
        if(selected && !values.contains(item)) {
            values.add(item);
        } else if(!selected) {
            values.remove(item);
        }
    }

//    public SelectionManager<String> getSelection() {
//        return (SelectionManager<String>) getStateHelper().eval(PropertyKeys.selection, new SelectionManager<>());
//    }
//    
//    public void setSelection(SelectionManager<String> items) {
//        getStateHelper().put(PropertyKeys.selection, items);
//    }
//    
    public List getValues() {
        return (List) getStateHelper().eval(PropertyKeys.values, Collections.emptyList());
    }
    
    public void setValues(List values) {
        getStateHelper().put(PropertyKeys.values, values);
    }


    public static class SelectableWithCallback {
        
        private final Object value;
        private boolean selected;
        private final BiConsumer<Object, Boolean> valueChangeCallback;
        
        public SelectableWithCallback(Object value, boolean selected, BiConsumer<Object, Boolean> valueChangeCallback) {
            this.value = value;
            this.selected = selected;
            this.valueChangeCallback = valueChangeCallback;
        }
        
        /**
         * @return the value
         */
        public Object getValue() {
            return value;
        }
        
        public String getLabel() {
            return value.toString();
        }
        
        /**
         * @return the selected
         */
        public boolean isSelected() {
            return selected;
        }

        /**
         * @param selected the selected to set
         */
        public void setSelected(boolean selected) {
            if(this.selected != selected && this.valueChangeCallback != null) {
                this.valueChangeCallback.accept(value, selected);
            }
            this.selected = selected;
        }
    }

}
