package io.goobi.viewer.dao.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import io.goobi.viewer.model.administration.legal.ConsentScope;

@Converter
public class ConsentScopeConverter implements AttributeConverter<ConsentScope, String> {

    @Override
    public String convertToDatabaseColumn(ConsentScope attribute) {
        return attribute.toString();
    }

    @Override
    public ConsentScope convertToEntityAttribute(String dbData) {
        return new ConsentScope(dbData);
    }


}
