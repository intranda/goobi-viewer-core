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
package io.goobi.viewer.model.administration.legal;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.eclipse.jdt.internal.compiler.ast.IPolyExpression;

import io.goobi.viewer.dao.converter.NumberListConverter;
import io.goobi.viewer.dao.converter.TranslatedTextConverter;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;

/**
 * @author florian
 *
 */
@Entity
@Table(name = "cookie_banner")
public class CookieBanner {
    
    /** Unique database ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cookie_banner_id")
    protected Long id;
    
    @Column(name = "text", nullable = true, columnDefinition = "MEDIUMTEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText text = new TranslatedText();
    
    @Column(name = "active")
    private boolean active = false;
    
    /**
     * IDs of CMS Pages on which the cookie banner should not be displayed
     */
    @Column(name="ignore_on")
    @Convert(converter = NumberListConverter.class)
    private List<Long> ignoreList = new ArrayList<>();
    
    public CookieBanner() {
        
    }
    
    public CookieBanner(CookieBanner orig) {
        this.id = orig.id;
        this.text = new TranslatedText(orig.text);
        this.active = orig.active;
        this.ignoreList = new ArrayList<>(orig.ignoreList);
    }
    
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
    
    public TranslatedText getText() {
        return this.text;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public List<Long> getIgnoreList() {
        return ignoreList;
    }
    
    public void setIgnoreList(List<Long> ignoreList) {
        this.ignoreList = ignoreList;
    }


}
