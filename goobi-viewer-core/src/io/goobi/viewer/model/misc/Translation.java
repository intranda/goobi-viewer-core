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
package io.goobi.viewer.model.misc;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Translation {

    /** Unique database ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "translation_id")
    protected Long id;

    /**
     * An additional optional field used to identify the purpose or categorization of a translation. Usefull if an object has more than one
     * relationship with Translation entities and needs to distinguish them in some way
     **/
    @Column(name = "tag", nullable = true, columnDefinition = "LONGTEXT")
    protected String tag;

    @Column(name = "language")
    protected String language;

    @Column(name = "value", nullable = true, columnDefinition = "LONGTEXT")
    protected String value;

    public Translation() {
    }

    /**
     * 
     * @param language
     * @param value
     */
    public Translation(String language, String value) {
        this.language = language;
        this.value = value;
    }

    /**
     * 
     * @param language
     * @param tag
     * @param value
     */
    public Translation(String language, String tag, String value) {
        this.language = language;
        this.tag = tag;
        this.value = value;
    }

    /**
     * 
     * @param tag
     * @param lang
     * @return
     */
    public static String getTranslation(List<? extends Translation> translations, String lang, String tag) {
        if (tag == null || lang == null) {
            return null;
        }

        for (Translation translation : translations) {
            if (translation.getTag().equals(tag) && translation.getLanguage().equals(lang)) {
                return translation.getValue();
            }
        }

        return "";
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
     * @return the tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * @param tag the tag to set
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * @return the language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @param language the language to set
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return value;
    }
}
