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
package de.intranda.digiverso.presentation.controller.language;

public class Language {

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
     * @return the language code according to iso 639-1
     */
    public String getIsoCode639_1() {
        return isoCode_639_1;
    }
    
    /**
     * @return the language code according to iso 639-2/B
     */
    public String getIsoCode639_2B() {
        return isoCode_639_2_B;
    }
    
    /**
     * @return the language code according to iso 639-2/T
     */
    public String getIsoCode639_2T() {
        return isoCode_639_2_T;
    }
    
    /**
     * @param isoCode_639_1 the isoCode_639_1 to set
     */
    public void setIsoCode_639_1(String isoCode_639_1) {
        this.isoCode_639_1 = isoCode_639_1;
    }
    
    /**
     * @param isoCode_639_2_B the isoCode_639_2_B to set
     */
    public void setIsoCode_639_2B(String isoCode_639_2_B) {
        this.isoCode_639_2_B = isoCode_639_2_B;
    }
    
    /**
     * @param isoCode_639_2_T the isoCode_639_2_T to set
     */
    public void setIsoCode_639_2T(String isoCode_639_2_T) {
        this.isoCode_639_2_T = isoCode_639_2_T;
    }
    
    /**
     * @return the language code according to iso 639-2/B
     */
    public String getIsoCode() {
        return isoCode_639_2_B;
    }

    /**
     * @param the language code according to iso 639-2/B
     */
    public void setIsoCode(String isoCode) {
        this.isoCode_639_2_B = isoCode;
    }

    /**
     * @return the language code according to iso 639-1
     */
    public String getIsoCodeOld() {
        return isoCode_639_1;
    }

    /**
     * @param the language code according to iso 639-1
     */
    public void setIsoCodeOld(String isoCodeOld) {
        this.isoCode_639_1 = isoCodeOld;
    }

    /**
     * @return the englishName
     */
    public String getEnglishName() {
        return englishName;
    }

    /**
     * @param englishName the englishName to set
     */
    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    /**
     * @return the frenchName
     */
    public String getFrenchName() {
        return frenchName;
    }

    /**
     * @param frenchName the frenchName to set
     */
    public void setFrenchName(String frenchName) {
        this.frenchName = frenchName;
    }

    /**
     * @return the germanName
     */
    public String getGermanName() {
        return germanName;
    }

    /**
     * @param germanName the germanName to set
     */
    public void setGermanName(String germanName) {
        this.germanName = germanName;
    }

}
