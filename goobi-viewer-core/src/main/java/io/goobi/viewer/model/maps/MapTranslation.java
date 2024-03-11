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
package io.goobi.viewer.model.maps;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.io.Serializable;

import io.goobi.viewer.model.translations.Translation;

/**
 * @author florian
 *
 */
@Entity
@Table(name = "cms_geomap_translation")
public class MapTranslation extends Translation implements Serializable {

    private static final long serialVersionUID = 764535999827518666L;

    /** Reference to the owning Object. */
    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private GeoMap owner;

    /**
     * Empty Constructor
     */
    public MapTranslation() {
        super();
    }

    /**
     * Constructor without value
     *
     * @param language
     * @param tag
     * @param owner
     */
    public MapTranslation(String language, String tag, GeoMap owner) {
        super();
        this.language = language;
        this.tag = tag;
        this.owner = owner;
    }

    /**
     * @param t
     */
    public MapTranslation(MapTranslation t) {
        super(t);
        this.owner = t.owner;
    }

    /**
     * @return the owner
     */
    public GeoMap getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(GeoMap owner) {
        this.owner = owner;
    }

}
