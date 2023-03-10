package io.goobi.viewer.model.cms;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.goobi.viewer.dao.converter.TranslatedTextConverter;
import io.goobi.viewer.managedbeans.CmsMediaBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.media.CMSMediaHolder;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "cms_sliders")
public class HighlightedObject implements CMSMediaHolder, IPolyglott {

    private static final long serialVersionUID = 2632497568590266830L;
    
    private final HighlightedObjectData data;

    public HighlightedObject(HighlightedObjectData data, )

    @Override
    public boolean isComplete(Locale locale) {
        return this.name.isComplete(locale);
    }

    @Override
    public boolean isValid(Locale locale) {
        return this.name.isValid(locale);
    }

    @Override
    public boolean isEmpty(Locale locale) {
        return this.name.isEmpty();
    }

    @Override
    public Locale getSelectedLocale() {
        return this.name.getSelectedLocale();
    }

    @Override
    public void setSelectedLocale(Locale locale) {
        this.name.setSelectedLocale(locale);
    }

    @Override
    public String getMediaFilter() {
        return CmsMediaBean.getImageFilter();
    }

    @Override
    public String getMediaTypes() {
        return CmsMediaBean.getImageTypes();
    }

    @Override
    public boolean hasMediaItem() {
        return this.mediaItem != null;
    }

    @Override
    public CategorizableTranslatedSelectable<CMSMediaItem> getMediaItemWrapper() {
        if (hasMediaItem()) {
            return new CategorizableTranslatedSelectable<>(mediaItem, true,
                    mediaItem.getFinishedLocales().stream().findFirst().orElse(BeanUtils.getLocale()), Collections.emptyList());
        }

        return null;
    }

    public boolean hasImageURI() {
        switch (this.imageMode) {
            case RECORD_REPRESENTATIVE:
                return true;
            case UPLOADED_IMAGE:
                return hasMediaItem();
            default:
                return false;
        }
    }

    public URI getImageURI() {
        switch (this.imageMode) {
            case UPLOADED_IMAGE:
                return this.mediaItem.getIconURI();
            case RECORD_REPRESENTATIVE:
                
        }
    }

}
