package io.goobi.viewer.model.administration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.persistence.annotations.PrivateOwned;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.goobi.viewer.api.rest.serialization.TranslationListSerializer;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.translations.Translation;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "maintenance_mode")
public class MaintenanceMode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maintenance_mode_id")
    private Long id;

    private boolean enabled = false;

    @OneToMany(mappedBy = "owner", fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
    @PrivateOwned
    @JsonSerialize(using = TranslationListSerializer.class)
    private List<MaintenanceModeTranslation> translations = new ArrayList<>();

    @Transient
    @JsonIgnore
    private Locale selectedLocale;

    /**
     * Default constructor.
     */
    public MaintenanceMode() {
        this.selectedLocale = BeanUtils.getLocale();
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return the translations
     */
    public List<MaintenanceModeTranslation> getTranslations() {
        return translations;
    }

    /**
     * @param translations the translations to set
     */
    public void setTranslations(List<MaintenanceModeTranslation> translations) {
        this.translations = translations;
    }

    /**
     *
     * @return Text value in the current language
     * @should return correct value
     */
    public String getText() {
        return Translation.getTranslation(translations, selectedLocale.getLanguage(), "text");
    }

    /**
     * <p>
     * setText.
     * </p>
     *
     * @should set value correctly
     * @param description a {@link java.lang.String} object.
     */
    public void setText(String text) {
        MaintenanceModeTranslation.setTranslation(translations, selectedLocale.getLanguage(), text, "text", this);
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
        MaintenanceMode other = (MaintenanceMode) obj;
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
