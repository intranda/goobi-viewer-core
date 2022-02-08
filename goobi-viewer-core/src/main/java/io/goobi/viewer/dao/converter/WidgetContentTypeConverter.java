package io.goobi.viewer.dao.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import io.goobi.viewer.model.cms.widgets.type.WidgetContentType;

@Converter
public class WidgetContentTypeConverter implements AttributeConverter<WidgetContentType, String> {

    @Override
    public String convertToDatabaseColumn(WidgetContentType attribute) {
        return attribute.getName();
    }

    @Override
    public WidgetContentType convertToEntityAttribute(String dbData) {
        return WidgetContentType.valueOf(dbData);
    }
    


}
