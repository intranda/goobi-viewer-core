package io.goobi.viewer.dao.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.model.administration.legal.ConsentScope;
import io.goobi.viewer.model.administration.legal.DisplayScope;
import io.goobi.viewer.model.administration.legal.DisplayScope.PageScope;

@Converter
public class DisplayScopeConverter implements AttributeConverter<DisplayScope, String> {

    @Override
    public String convertToDatabaseColumn(DisplayScope attribute) {
        return attribute.getAsJson();
    }

    @Override
    public DisplayScope convertToEntityAttribute(String dbData) {
        if(StringUtils.isNotBlank(dbData)) {     
            try {                
                return new DisplayScope(dbData);
            } catch(IllegalArgumentException e) {
                return new DisplayScope(PageScope.RECORD, dbData);
            }
        } else {
            return new DisplayScope(PageScope.RECORD, "");
        }
    }


}
