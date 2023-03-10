package io.goobi.viewer.model.cms;

import java.net.URI;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;

import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.CmsMediaBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.media.CMSMediaHolder;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.translations.IPolyglott;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "cms_sliders")
public class HighlightedObject implements CMSMediaHolder, IPolyglott {

    private static final long serialVersionUID = 2632497568590266830L;
    
    private final HighlightedObjectData data;
    private final ThumbnailHandler thumbs;
    
    public HighlightedObject(HighlightedObjectData data, ThumbnailHandler thumbs) {
        if(data == null || thumbs == null) {
            throw new NullPointerException("Constructor arguments may not be null");
        }
        this.data = data;
        this.thumbs = thumbs;
    }
    
    public HighlightedObject(HighlightedObjectData data) {
        this(data, BeanUtils.getImageDeliveryBean().getThumbs());
    }

    @Override
    public boolean isComplete(Locale locale) {
        return this.data.getName().isComplete(locale);
    }

    @Override
    public boolean isValid(Locale locale) {
        return this.data.getName().isValid(locale);
    }

    @Override
    public boolean isEmpty(Locale locale) {
        return this.data.getName().isEmpty();
    }

    @Override
    public Locale getSelectedLocale() {
        return this.data.getName().getSelectedLocale();
    }

    @Override
    public void setSelectedLocale(Locale locale) {
        this.data.getName().setSelectedLocale(locale);
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
        return this.data.getMediaItem() != null;
    }
    
    @Override
    public CMSMediaItem getMediaItem() {
        return this.data.getMediaItem();
    }

    @Override
    public void setMediaItem(CMSMediaItem item) {
        this.data.setMediaItem(item);
    }

    @Override
    public CategorizableTranslatedSelectable<CMSMediaItem> getMediaItemWrapper() {
        if (hasMediaItem()) {
            return new CategorizableTranslatedSelectable<>(getMediaItem(), true,
                    getMediaItem().getFinishedLocales().stream().findFirst().orElse(BeanUtils.getLocale()), Collections.emptyList());
        }

        return null;
    }

    /**
     * Check whether an image is set for this object
     * @return  true if the image is taken from the record identifier or if it is taken from a media item and the media item is set
     */
    public boolean hasImageURI() {
        switch (this.data.getImageMode()) {
            case RECORD_REPRESENTATIVE:
                return true;
            case UPLOADED_IMAGE:
                return hasMediaItem();
            default:
                return false;
        }
    }

    /**
     * Get the URI to the image representing the object
     * @return  true if {@link #hasImageURI()} returns true and, if the image is taken from the record identifier, the record identifier points to a records which has a representative image
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ViewerConfigurationException
     */
    public URI getImageURI() throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        switch (this.data.getImageMode()) {
            case UPLOADED_IMAGE:
                return Optional.ofNullable(this.getMediaItem()).map(CMSMediaItem::getIconURI).orElse(null);
            case RECORD_REPRESENTATIVE:
                return Optional.ofNullable(this.thumbs.getThumbnailUrl(this.data.getRecordIdentifier())).map(URI::create).orElse(null);
            default:
                return null;
                
        }
    }

}
