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
 * <p>
 * Language class.
 * </p>
 */
public class Language implements Comparable<Language>, Serializable {

    private static final long serialVersionUID = 688690972248321229L;

    /**
     * language code according to iso 639-2/B (based on English names)
     */
    private String isoCode_639_2_B;
    /**
     * language code according to iso 639-2/T (based on native names)
     */
    private String isoCode_639_2_T;
    /**
     * language code according to iso 639_1
     */
    private String isoCode_639_1;
    private String englishName;
    private String frenchName;
    private String germanName;

    /**
     * <p>
     * getIsoCode639_1.
     * </p>
     *
     * @return the language code according to iso 639-1
     */
    public String getIsoCode639_1() {
        return isoCode_639_1;
    }

    /**
     * <p>
     * getIsoCode639_2B.
     * </p>
     *
     * @return the language code according to iso 639-2/B
     */
    public String getIsoCode639_2B() {
        return isoCode_639_2_B;
    }

    /**
     * <p>
     * getIsoCode639_2T.
     * </p>
     *
     * @return the language code according to iso 639-2/T
     */
    public String getIsoCode639_2T() {
        return isoCode_639_2_T;
    }

    /**
     * <p>
     * Setter for the field <code>isoCode_639_1</code>.
     * </p>
     *
     * @param isoCode_639_1 the isoCode_639_1 to set
     */
    public void setIsoCode_639_1(String isoCode_639_1) {
        this.isoCode_639_1 = isoCode_639_1;
    }

    /**
     * <p>
     * setIsoCode_639_2B.
     * </p>
     *
     * @param isoCode_639_2_B the isoCode_639_2_B to set
     */
    public void setIsoCode_639_2B(String isoCode_639_2_B) {
        this.isoCode_639_2_B = isoCode_639_2_B;
    }

    /**
     * <p>
     * setIsoCode_639_2T.
     * </p>
     *
     * @param isoCode_639_2_T the isoCode_639_2_T to set
     */
    public void setIsoCode_639_2T(String isoCode_639_2_T) {
        this.isoCode_639_2_T = isoCode_639_2_T;
    }

    /**
     * <p>
     * getIsoCode.
     * </p>
     *
     * @return the language code according to iso 639-2/B
     */
    public String getIsoCode() {
        return isoCode_639_2_B;
    }

    /**
     * <p>
     * setIsoCode.
     * </p>
     *
     * @param isoCode a {@link java.lang.String} object.
     */
    public void setIsoCode(String isoCode) {
        this.isoCode_639_2_B = isoCode;
    }

    /**
     * <p>
     * getIsoCodeOld.
     * </p>
     *
     * @return the language code according to iso 639-1
     */
    public String getIsoCodeOld() {
        return isoCode_639_1;
    }

    /**
     * <p>
     * setIsoCodeOld.
     * </p>
     *
     * @param isoCodeOld a {@link java.lang.String} object.
     */
    public void setIsoCodeOld(String isoCodeOld) {
        this.isoCode_639_1 = isoCodeOld;
    }

    /**
     * <p>
     * Getter for the field <code>englishName</code>.
     * </p>
     *
     * @return the englishName
     */
    public String getEnglishName() {
        return englishName;
    }

    /**
     * <p>
     * Setter for the field <code>englishName</code>.
     * </p>
     *
     * @param englishName the englishName to set
     */
    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    /**
     * <p>
     * Getter for the field <code>frenchName</code>.
     * </p>
     *
     * @return the frenchName
     */
    public String getFrenchName() {
        return frenchName;
    }

    /**
     * <p>
     * Setter for the field <code>frenchName</code>.
     * </p>
     *
     * @param frenchName the frenchName to set
     */
    public void setFrenchName(String frenchName) {
        this.frenchName = frenchName;
    }

    /**
     * <p>
     * Getter for the field <code>germanName</code>.
     * </p>
     *
     * @return the germanName
     */
    public String getGermanName() {
        return germanName;
    }

    /**
     * <p>
     * Setter for the field <code>germanName</code>.
     * </p>
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
        result = prime * result + ((isoCode_639_2_B == null) ? 0 : isoCode_639_2_B.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Language other = (Language) obj;
        if (isoCode_639_2_B == null) {
            if (other.isoCode_639_2_B != null)
                return false;
        } else if (!isoCode_639_2_B.equals(other.isoCode_639_2_B))
            return false;
        return true;
    }
}
