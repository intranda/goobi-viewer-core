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
package io.goobi.viewer.model.administration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.persistence.annotations.PrivateOwned;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.goobi.viewer.api.rest.serialization.TranslationListSerializer;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.translations.IPolyglott;
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
public class MaintenanceMode implements IPolyglott {

    private static final Logger logger = LogManager.getLogger(MaintenanceMode.class);

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

    
    public Long getId() {
        return id;
    }

    
    public void setId(Long id) {
        this.id = id;
    }

    
    public boolean isEnabled() {
        return enabled;
    }

    
    public void setEnabled(boolean enabled) {
        logger.trace("setEnabled: {}", enabled);
        this.enabled = enabled;
    }

    
    public List<MaintenanceModeTranslation> getTranslations() {
        return translations;
    }

    
    public void setTranslations(List<MaintenanceModeTranslation> translations) {
        this.translations = translations;
    }

    /**
     *
     * @return Text value in the current language
     */
    public String getText() {
        return getText(selectedLocale.getLanguage());
    }

    /**
     * @param language locale for the text translation to retrieve
     * @return Text value in the given language
     */
    public String getText(String language) {
        return Translation.getTranslation(translations, language, "text");
    }

    /**
     * @param language locale for the text translation to retrieve, with fallback to default
     * @return Text value in the given language
     * @should return translation correctly
     */
    public String getTextOrDefault(String language) {
        return Translation.getTranslation(translations, language, "text", true);
    }

    /**
     * setText.
     *
     * @param text maintenance message text for the current locale
     */
    public void setText(String text) {
        setText(text, selectedLocale.getLanguage());
    }

    /**
     * 
     * @param text maintenance message text to store
     * @param language locale for which to set the text
     */
    public void setText(String text, String language) {
        MaintenanceModeTranslation.setTranslation(translations, language, text, "text", this);
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

    /** {@inheritDoc} */
    @Override
    public boolean isComplete(Locale locale) {
        return StringUtils.isNotBlank(getText(locale.getLanguage()));
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValid(Locale locale) {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEmpty(Locale locale) {
        return StringUtils.isBlank(getText(locale.getLanguage()));
    }

    /** {@inheritDoc} */
    @Override
    public Locale getSelectedLocale() {
        return selectedLocale;
    }

    @Override
    public void setSelectedLocale(Locale locale) {
        this.selectedLocale = locale;

    }
}
