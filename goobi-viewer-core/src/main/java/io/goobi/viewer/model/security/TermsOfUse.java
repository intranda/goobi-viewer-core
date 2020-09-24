/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.security;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jcajce.provider.symmetric.DES;
import org.eclipse.persistence.annotations.PrivateOwned;


import io.goobi.viewer.model.misc.Translation;

/**
 * @author florian
 *
 */
public class TermsOfUse {

    //labels for the translations
    private static final String TITLE_TAG = "label";
    private static final String DESCRIPTION_TAG = "description";
    
    /**
     * Contains texts and titles
     */
    @OneToMany(mappedBy = "owner", fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
    @PrivateOwned
    private List<Translation> translations = new ArrayList<>();
    
    @Column(name = "active")
    private boolean active = false;
    
    /**
     * @param active the active to set
     */
    public void setActive(boolean active) {
        this.active = active;
    }
    
    /**
     * @return the active
     */
    public boolean isActive() {
        return active;
    }
    
    public Translation getTitle(String language) {
        Translation translation = getForLanguage(getTitles(), language).findAny().orElse(null);
        return translation;
    }
    
    public Translation setTitle(String language, String value) {
        Translation translation = getTitle(language);
        if(translation == null) {
            translation = new TermsOfUseTranslation(language, value);
            translation.setTag(TITLE_TAG);
            this.translations.add(translation);
        } else {
            translation.setValue(value);
        }
        return translation;
    }
    
    public Translation getDescription(String language) {
        Translation translation = getForLanguage(getDescriptions(), language).findAny().orElse(null);
        return translation;
    }
    
    public Translation setDescription(String language, String value) {
        Translation translation = getDescription(language);
        if(translation == null) {
            translation = new TermsOfUseTranslation(language, value);
            translation.setTag(DESCRIPTION_TAG);
            this.translations.add(translation);
        } else {
            translation.setValue(value);
        }
        return translation;
    }
    
    private Stream<Translation> getTitles() {
        return this.translations.stream().filter(t -> TITLE_TAG.equals(t.getTag()));
    }
    
    private Stream<Translation> getDescriptions() {
        return this.translations.stream().filter(t -> DESCRIPTION_TAG.equals(t.getTag()));
    }

    private Stream<Translation> getForLanguage(Stream<Translation> translations, String language) {
        if(StringUtils.isBlank(language)) {
            throw new IllegalArgumentException("Must provide non-empty language parameter to filter translations for language");
        }
        return translations.filter(t -> language.equals(t.getLanguage()));
    }
}
