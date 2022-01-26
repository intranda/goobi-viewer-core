package io.goobi.viewer.faces.converters;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.model.cms.widgets.WidgetDisplayElement;
import io.goobi.viewer.model.cms.widgets.type.WidgetContentType;

@FacesConverter("widgetDisplayElementConverter")
public class WidgetDisplayElementConverter implements Converter<WidgetDisplayElement> {

    public static final String STRING_REPRESENTATION_PATTERN = "{contentType}|{id}";
    public static final String STRING_REPRESENTATION_REGEX = "(\\w+)|(\\d+)";

    
    @Override
    public WidgetDisplayElement getAsObject(FacesContext context, UIComponent component, String value) {
        Matcher matcher = Pattern.compile(STRING_REPRESENTATION_REGEX).matcher(value);
        if(matcher.matches()) {
            WidgetContentType type = WidgetContentType.valueOf(matcher.group(1));
            Long id = Long.valueOf(matcher.group(2));
            if(type != null) {
                WidgetDisplayElement element = new WidgetDisplayElement(new SimpleMetadataValue(), new SimpleMetadataValue(), Collections.emptyList(), WidgetContentType.getGenerationType(type), type, id);
                return element;
            }
            
        }
        return null;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, WidgetDisplayElement value) {
        String s = STRING_REPRESENTATION_PATTERN
                .replace("{contentType}", value.getContentType().getName())
                .replace("{id}", value.getId() != null ?  Long.toString(value.getId()) : "");
        return s;
    }

}
