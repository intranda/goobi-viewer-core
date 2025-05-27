package io.goobi.viewer.model.administration;

import java.util.List;

import io.goobi.viewer.model.translations.Translation;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "maintenance_mode_translations")
public class MaintenanceModeTranslation extends Translation {

    /** Reference to the owning {@link MaintenanceMode}. */
    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private MaintenanceMode owner;
    
    /**
     * <p>
     * Default constructor.
     * </p>
     */
    public MaintenanceModeTranslation() {
        super();
    }

    /**
     * <p>
     * Constructor for MaintenanceModeTranslation.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     * @param tag a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     * @param owner
     */
    public MaintenanceModeTranslation(String language, String tag, String value, MaintenanceMode owner) {
        super(language, tag, value);
        this.owner = owner;
    }

    /**
     * <p>
     * setTranslation.
     * </p>
     *
     * @param translations a {@link java.util.List} object.
     * @param lang a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     * @param tag a {@link java.lang.String} object.
     * @param owner a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     */
    public static void setTranslation(List<MaintenanceModeTranslation> translations, String lang, String value, String tag, MaintenanceMode owner) {
        if (lang == null) {
            throw new IllegalArgumentException("lang may not be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value may not be null");
        }

        for (MaintenanceModeTranslation translation : translations) {
            if (translation.getTag().equals(tag) && translation.getLanguage().equals(lang)) {
                translation.setTranslationValue(value);
                return;
            }
        }
        translations.add(new MaintenanceModeTranslation(lang, tag, value, owner));
    }

    /**
     * @return the owner
     */
    public MaintenanceMode getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(MaintenanceMode owner) {
        this.owner = owner;
    }
    
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MaintenanceModeTranslation other = (MaintenanceModeTranslation) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        
        return true;
    }
}
