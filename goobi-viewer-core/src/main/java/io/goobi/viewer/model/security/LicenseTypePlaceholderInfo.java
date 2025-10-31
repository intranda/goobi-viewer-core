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
package io.goobi.viewer.model.security;

import java.io.Serializable;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import io.goobi.viewer.managedbeans.CmsMediaBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.CategorizableTranslatedSelectable;
import io.goobi.viewer.model.cms.media.CMSMediaHolder;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.translations.Translation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "license_type_placeholder_info")
public class LicenseTypePlaceholderInfo extends Translation implements CMSMediaHolder, Serializable {

    private static final long serialVersionUID = -6171783524157218760L;

    public enum LicenseTypeImageMode {
        DEFAULT,
        UPLOADED_IMAGE;

        /**
         * 
         * @param name
         * @return {@link LicenseTypeImageMode} matchin name; otherwise null
         */
        public static LicenseTypeImageMode getByName(String name) {
            for (LicenseTypeImageMode mode : LicenseTypeImageMode.values()) {
                if (mode.name().equalsIgnoreCase(name)) {
                    return mode;
                }
            }

            return null;
        }
    }

    /** Reference to the owning Object. */
    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private LicenseType owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_mode")
    private LicenseTypeImageMode imageMode = LicenseTypeImageMode.DEFAULT;

    @JoinColumn(name = "media_item_id")
    private CMSMediaItem mediaItem;

    /**
     * Zero-arg constructor for JPA.
     */
    public LicenseTypePlaceholderInfo() {
    }

    /**
     * 
     * @param language
     * @param tag
     * @param owner
     */
    public LicenseTypePlaceholderInfo(String language, String tag, LicenseType owner) {
        super();
        this.language = language;
        this.tag = tag;
        this.owner = owner;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(language);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LicenseTypePlaceholderInfo other = (LicenseTypePlaceholderInfo) obj;
        return language == other.language;
    }

    public String getMediaImageURI() {
        return Optional.ofNullable(getMediaItem()).map(CMSMediaItem::getImageURI).orElse(null);
    }

    public String getMediaThumbnailURI() {
        return Optional.ofNullable(getMediaItem()).map(item -> item.getIconURI(0,0).toString()).orElse(null);
    }

    /**
     * @return the owner
     */
    public LicenseType getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(LicenseType owner) {
        this.owner = owner;
    }

    /**
     * @return the imageMode
     */
    public LicenseTypeImageMode getImageMode() {
        if (imageMode == null) {
            imageMode = LicenseTypeImageMode.DEFAULT;
        }
        return imageMode;
    }

    /**
     * @param imageMode the imageMode to set
     */
    public void setImageMode(LicenseTypeImageMode imageMode) {
        this.imageMode = imageMode;
    }

    @Override
    public CMSMediaItem getMediaItem() {
        return mediaItem;
    }

    @Override
    public void setMediaItem(CMSMediaItem item) {
        this.mediaItem = item;
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
        return mediaItem != null;
    }

    @Override
    public CategorizableTranslatedSelectable<CMSMediaItem> getMediaItemWrapper() {
        if (hasMediaItem()) {
            return new CategorizableTranslatedSelectable<>(mediaItem, true,
                    mediaItem.getFinishedLocales().stream().findFirst().orElse(BeanUtils.getLocale()), Collections.emptyList());
        }

        return null;
    }
}
