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
package io.goobi.viewer.model.translations.language;

import java.io.Serializable;
import java.util.Locale;

/**
 * Language class.
 */
public class Language implements Comparable<Language>, Serializable {

    private static final long serialVersionUID = 688690972248321229L;

    /**
     * Language code according to iso 639-2/B (based on English names).
     */
    private String isoCode6392B;
    /**
     * Language code according to iso 639-2/T (based on native names).
     */
    private String isoCode6392T;
    /**
     * Language code according to iso 639_1.
     */
    private String isoCode6391;
    private String englishName;
    private String frenchName;
    private String germanName;

    /**
     * getIsoCode639_1.
     *
     * @return the language code according to iso 639-1
     */
    public String getIsoCode6391() {
        return isoCode6391;
    }

    /**
     * getIsoCode639_2B.
     *
     * @return the language code according to iso 639-2/B
     */
    public String getIsoCode6392B() {
        return isoCode6392B;
    }

    /**
     * getIsoCode639_2T.
     *
     * @return the language code according to iso 639-2/T
     */
    public String getIsoCode6392T() {
        return isoCode6392T;
    }

    /**
     * Setter for the field <code>isoCode6391</code>.
     *
     * @param isoCode6391 the isoCode_639_1 to set
     */
    public void setIsoCode6391(String isoCode6391) {
        this.isoCode6391 = isoCode6391;
    }

    /**
     * setIsoCode_639_2B.
     *
     * @param isoCode6392B the isoCode_639_2_B to set
     */
    public void setIsoCode6392B(String isoCode6392B) {
        this.isoCode6392B = isoCode6392B;
    }

    /**
     * setIsoCode_639_2T.
     *
     * @param isoCode6392T the isoCode_639_2_T to set
     */
    public void setIsoCode6392T(String isoCode6392T) {
        this.isoCode6392T = isoCode6392T;
    }

    /**
     * getIsoCode.
     *
     * @return the language code according to iso 639-2/B
     */
    public String getIsoCode() {
        return getIsoCode6392B();
    }

    /**
     * setIsoCode.
     *
     * @param isoCode ISO 639-2/B language code to set
     */
    public void setIsoCode(String isoCode) {
        this.isoCode6392B = isoCode;
    }

    /**
     * getIsoCodeOld.
     *
     * @return the language code according to iso 639-1
     */
    public String getIsoCodeOld() {
        return getIsoCode6391();
    }

    /**
     * setIsoCodeOld.
     *
     * @param isoCodeOld ISO 639-1 language code to set
     */
    public void setIsoCodeOld(String isoCodeOld) {
        this.isoCode6391 = isoCodeOld;
    }

    /**
     * Getter for the field <code>englishName</code>.
     *
     * @return the englishName
     */
    public String getEnglishName() {
        return englishName;
    }

    /**
     * Setter for the field <code>englishName</code>.
     *
     * @param englishName the englishName to set
     */
    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    /**
     * Getter for the field <code>frenchName</code>.
     *
     * @return the frenchName
     */
    public String getFrenchName() {
        return frenchName;
    }

    /**
     * Setter for the field <code>frenchName</code>.
     *
     * @param frenchName the frenchName to set
     */
    public void setFrenchName(String frenchName) {
        this.frenchName = frenchName;
    }

    /**
     * Getter for the field <code>germanName</code>.
     *
     * @return the germanName
     */
    public String getGermanName() {
        return germanName;
    }

    /**
     * Setter for the field <code>germanName</code>.
     *
     * @param germanName the germanName to set
     */
    public void setGermanName(String germanName) {
        this.germanName = germanName;
    }

    public String getName(Locale locale) {
        switch (locale.getLanguage()) {
            case "ger":
            case "de":
                return getGermanName();
            case "fr":
            case "fra":
                return getFrenchName();
            default:
                return getEnglishName();
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Language other) {
        return this.getIsoCode().compareTo(other.getIsoCode());
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((isoCode6392B == null) ? 0 : isoCode6392B.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
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
        Language other = (Language) obj;
        if (isoCode6392B == null) {
            if (other.isoCode6392B != null) {
                return false;
            }
        } else if (!isoCode6392B.equals(other.isoCode6392B)) {
            return false;
        }

        return true;
    }
}
