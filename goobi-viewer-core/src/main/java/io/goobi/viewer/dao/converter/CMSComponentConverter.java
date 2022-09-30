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
package io.goobi.viewer.dao.converter;

import java.util.Optional;

import io.goobi.viewer.model.cms.CMSTemplateManager;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSPageContentManager;
import io.goobi.viewer.model.cms.pages.content.PersistentCMSComponent;
import jakarta.persistence.AttributeConverter;

public class CMSComponentConverter implements AttributeConverter<CMSComponent, PersistentCMSComponent> {

    private final CMSPageContentManager contentManager = CMSTemplateManager.getInstance().getContentManager();
    
    @Override
    public PersistentCMSComponent convertToDatabaseColumn(CMSComponent attribute) {
        PersistentCMSComponent perComponent = new PersistentCMSComponent(attribute);
        return perComponent;
    }

    @Override
    public CMSComponent convertToEntityAttribute(PersistentCMSComponent dbData) {
        CMSComponent template = contentManager.getComponents().stream().filter(c -> c.getTemplateFilename().equals(dbData.getTemplateFilename()))
                .findAny().orElse(null);
        if(template != null) {            
            CMSComponent component = new CMSComponent(template, Optional.of(dbData));
            return component;
        } else {
            throw new IllegalArgumentException(String.format("PersistentCMSComponent with template_filename %s doesn't match any xml-template", dbData.getTemplateFilename()));
        }
    }

}
