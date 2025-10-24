package io.goobi.viewer.model.security;

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
public class LicenseTypePlaceholderInfo extends Translation implements CMSMediaHolder {

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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMediaTypes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasMediaItem() {
        return mediaItem != null;
    }

    @Override
    public CategorizableTranslatedSelectable<CMSMediaItem> getMediaItemWrapper() {
        // TODO Auto-generated method stub
        return null;
    }
}
