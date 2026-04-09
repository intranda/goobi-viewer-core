/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.faces.converters;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Florian Alpers
 */
@FacesConverter("localDateConverter")
public class LocalDateConverter implements Converter<LocalDate> {

    private static final Logger logger = LogManager.getLogger(LocalDateConverter.class);

    private static final String ATTRIBUTE_DATA_FORMAT = "data-format";

    @Override
    public LocalDate getAsObject(FacesContext context, UIComponent component, String value) {
        if (StringUtils.isNotBlank(value)) {
            if (component != null && component.getAttributes().get(ATTRIBUTE_DATA_FORMAT) != null) {
                String format = (String) component.getAttributes().get(ATTRIBUTE_DATA_FORMAT);
                logger.debug("getAsObject: value='{}', data-format='{}', componentId='{}'",
                        value, format, component.getClientId(context));
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(format);
                return LocalDate.parse(value, dateTimeFormatter);
            }
            logger.debug("getAsObject: value='{}', no data-format, ISO fallback", value);
            return LocalDate.parse(value);
        }

        return null;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, LocalDate value) {
        if (value != null) {
            if (component != null && component.getAttributes().get(ATTRIBUTE_DATA_FORMAT) != null) {
                String format = (String) component.getAttributes().get(ATTRIBUTE_DATA_FORMAT);
                String result = value.format(DateTimeFormatter.ofPattern(format));
                logger.debug("getAsString: value='{}', data-format='{}', result='{}'",
                        value, format, result);
                return result;
            }
            logger.debug("getAsString: value='{}', no data-format, ISO fallback '{}'", value, value.toString());
            return value.toString();
        }

        return null;
    }

}
