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
package io.goobi.viewer.model.cms;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.CmsMediaBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.media.CMSMediaHolder;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.translations.IPolyglott;

public class HighlightedObject implements CMSMediaHolder, IPolyglott {

    private final HighlightedObjectData data;
    private final ThumbnailHandler thumbs;

    public HighlightedObject(HighlightedObjectData data, ThumbnailHandler thumbs) {
        if (data == null || thumbs == null) {
            throw new NullPointerException("Constructor arguments may not be null");
        }
        this.data = new HighlightedObjectData(data);
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

    public HighlightedObjectData getData() {
        return data;
    };

    /**
     * Check whether an image is set for this object
     * 
     * @return true if the image is taken from the record identifier or if it is taken from a media item and the media item is set
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
     * 
     * @return true if {@link #hasImageURI()} returns true and, if the image is taken from the record identifier, the record identifier points to a
     *         records which has a representative image
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ViewerConfigurationException
     */
    public URI getImageURI(int width, int height) throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        switch (this.data.getImageMode()) {
            case UPLOADED_IMAGE:
                return Optional.ofNullable(this.getMediaItem()).map(mediaItem -> this.thumbs.getThumbnailUrl(mediaItem, width, height)).map(URI::create).orElse(null);
            case RECORD_REPRESENTATIVE:
                return Optional.ofNullable(this.thumbs.getThumbnailUrl(this.data.getRecordIdentifier(), width, height)).map(URI::create).orElse(null);
            default:
                return null;

        }
    }

    public URI getImageURI() throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        return getImageURI(DataManager.getInstance().getConfiguration().getThumbnailsWidth(),
                DataManager.getInstance().getConfiguration().getThumbnailsHeight());
    }
    
    /**
     * alias for {@link #isCurrent()}
     * @return  true if startTime is before now (or null) and endTime is after now (or null)
     */
    public boolean isPresent() {
        return isCurrent();
    }
    
    /**
     * Check if the object is active now. Mutually exclusive with {@link HighlightedObject#isPast()} and {@link #isFuture()}
     * @return  true if startTime is before now (or null) and endTime is after now (or null)
     */
    public boolean isCurrent() {
        return isCurrent(LocalDateTime.now());
    }
    
    /**
     * Check if the object is active at the given date
     * @return  true if startTime is before now (or null) and endTime is after now (or null)
     */
    public boolean isCurrent(LocalDateTime date) {
        return (data.getTimeStart() == null || date.isAfter(data.getTimeStart())) && (data.getTimeEnd() == null || date.isBefore(data.getTimeEnd()));
    }
    
    /**
     * Check if this object is no longer active. Mutually exclusive with {@link #isCurrent()} and {@link #isFuture()}
     * @return  true if timeEnd is not null and before now
     */
    public boolean isPast() {
        return isPast(LocalDateTime.now());
    }
    
    /**
     * Check if this object was only active before the given date.
     * @return  true if timeEnd is not null and before now
     */
    public boolean isPast(LocalDateTime date) {
        return  data.getTimeEnd() != null && date.isAfter(data.getTimeEnd());
    }
    
    /**
     * Check if this object is not currently active but will be active in the future. Mutually exclusive with {@link #isCurrent()} and {@link #isPast()}
     * @return true if startTime is not null and after now and timeEnd is either null or after now
     */
    public boolean isFuture() {
        return isFuture(LocalDateTime.now());
    }
    
    /**
     * Check if this object is not currently active but will be active in the future of the given date. 
     * @return true if startTime is not null and after now and timeEnd is either null or after now
     */
    public boolean isFuture(LocalDateTime date) {
        return data.getTimeStart() != null && date.isBefore(data.getTimeStart());
    }
    
    /**
     * Check if this object may be active at all
     * @return
     */
    public boolean isEnabled() {
        return this.data.isEnabled();
    }
    
    @Override
    public String toString() {
        return "Data: " + this.getData().toString();
    }

}
