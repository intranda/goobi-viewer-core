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

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.SimpleMediaHolder;
import io.goobi.viewer.model.cms.media.CMSMediaItem;

@Converter
public class SimpleMediaHolderConverter implements AttributeConverter<SimpleMediaHolder, Long> {

    private static final Logger logger = LogManager.getLogger(SimpleMediaHolderConverter.class);

    @Override
    public Long convertToDatabaseColumn(SimpleMediaHolder media) {
        if (media != null && media.hasMediaItem()) {
            return media.getMediaItem().getId();
        } else {
            return null;
        }
    }

    @Override
    public SimpleMediaHolder convertToEntityAttribute(Long id) {
        if (id != null) {
            try {
                CMSMediaItem item = DataManager.getInstance().getDao().getCMSMediaItem(id);
                return new SimpleMediaHolder(item);
            } catch (DAOException e) {
                logger.warn("Trying to load media holder for media item id {} which cannot be found in database.", id);
                return new SimpleMediaHolder();
            }
        } else {
            return new SimpleMediaHolder();
        }
    }

}
