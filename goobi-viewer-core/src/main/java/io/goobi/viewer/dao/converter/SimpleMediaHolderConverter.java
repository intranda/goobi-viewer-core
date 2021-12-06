package io.goobi.viewer.dao.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.CMSMediaItem;
import io.goobi.viewer.model.cms.SimpleMediaHolder;

@Converter
public class SimpleMediaHolderConverter implements AttributeConverter<SimpleMediaHolder, Long> {

    private static final Logger logger = LoggerFactory.getLogger(SimpleMediaHolderConverter.class);

    
    @Override
    public Long convertToDatabaseColumn(SimpleMediaHolder media) {
        if(media != null && media.hasMediaItem()) {
            return media.getMediaItem().getId();
        } else {
            return null;
        }
    }

    @Override
    public SimpleMediaHolder convertToEntityAttribute(Long id) {
        if(id != null) {            
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
