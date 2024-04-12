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
package io.goobi.viewer.model.administration.legal;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.goobi.viewer.dao.converter.NumberListConverter;
import io.goobi.viewer.dao.converter.TranslatedTextConverter;
import io.goobi.viewer.model.translations.TranslatedText;

/**
 * Class to persist settings for the cookie banner. Only one instance of this class should be persisted in the database
 *
 * @author florian
 *
 */
@Entity
@Table(name = "cookie_banner")
public class CookieBanner implements Serializable {

    private static final long serialVersionUID = 7200913959493405687L;

    /** Unique database ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cookie_banner_id")
    protected Long id;

    /**
     * The text shown on the banner. Must be pure text or valid (x)html
     */
    @Column(name = "text", nullable = true, columnDefinition = "MEDIUMTEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText text = new TranslatedText();

    /**
     * set if the banner should be shown at all
     */
    @Column(name = "active")
    private boolean active = false;

    /**
     * The time after which a user must have agreed to the banner. If the browser's local storage contains a different value, the cookie banner must
     * be accepted again
     */
    @Column(name = "requires_consent_after", nullable = false)
    private LocalDateTime requiresConsentAfter = LocalDateTime.now();

    /**
     * IDs of CMS Pages on which the cookie banner should not be displayed
     */
    @Column(name = "ignore_on")
    @Convert(converter = NumberListConverter.class)
    private List<Long> ignoreList = new ArrayList<>();

    /**
     * empty default constructor
     */
    public CookieBanner() {

    }

    /**
     * Cloning constructor
     * 
     * @param orig
     */
    public CookieBanner(CookieBanner orig) {
        this.id = orig.id;
        this.text = new TranslatedText(orig.text);
        this.active = orig.active;
        this.ignoreList = new ArrayList<>(orig.ignoreList);
        this.requiresConsentAfter = LocalDateTime.from(orig.requiresConsentAfter);
    }

    /**
     * @param active set the {@link #active} property
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * @return the {@link #active} property
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @return the {@link #text} property
     */
    public TranslatedText getText() {
        return this.text;
    }

    /**
     * @return the database id
     */
    public Long getId() {
        return id;
    }

    /**
     * set the database id
     * 
     * @param id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     *
     * @return the {@link CookieBanner#ignoreList}
     */
    public List<Long> getIgnoreList() {
        return ignoreList;
    }

    /**
     * set the {@link CookieBanner#ignoreList}
     * 
     * @param ignoreList
     */
    public void setIgnoreList(List<Long> ignoreList) {
        this.ignoreList = ignoreList;
    }

    /**
     *
     * @return the {@link #requiresConsentAfter}
     */
    public LocalDateTime getRequiresConsentAfter() {
        return requiresConsentAfter;
    }

    /**
     * set the {@link #requiresConsentAfter}
     * 
     * @param requiresConsentAfter
     */
    public void setRequiresConsentAfter(LocalDateTime requiresConsentAfter) {
        this.requiresConsentAfter = requiresConsentAfter;
    }
}
