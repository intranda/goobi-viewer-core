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

    private String isoCode;
    private String isoCodeOld;
    private String englishName;
    private String frenchName;
    private String germanName;

    /**
     * @return the language code according to iso 639-2/B
     */
    public String getIsoCode() {
        return isoCode;
    }

    /**
     * @param the language code according to iso 639-2/B
     */
    public void setIsoCode(String isoCode) {
        this.isoCode = isoCode;
    }

    /**
     * @return the language code according to iso 639-1
     */
    public String getIsoCodeOld() {
        return isoCodeOld;
    }

    /**
     * @param the language code according to iso 639-1
     */
    public void setIsoCodeOld(String isoCodeOld) {
        this.isoCodeOld = isoCodeOld;
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
