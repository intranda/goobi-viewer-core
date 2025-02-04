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

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.goobi.viewer.dao.converter.ConsentScopeConverter;
import io.goobi.viewer.dao.converter.DisplayScopeConverter;
import io.goobi.viewer.dao.converter.TranslatedTextConverter;
import io.goobi.viewer.model.administration.legal.DisplayScope.PageScope;
import io.goobi.viewer.model.translations.TranslatedText;

/**
 * Class to persist settings for the disclaimer modal. Only one instance of this class should be persisted in the database
 *
 * @author florian
 *
 */
@Entity
@Table(name = "disclaimer")
public class Disclaimer {

    /** Unique database ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "disclaimer_id")
    protected Long id;

    /**
     * Disclaimer text. Must be pure text or valid (x)html
     */
    @Column(name = "text", nullable = true, columnDefinition = "MEDIUMTEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText text = new TranslatedText();

    /**
     * set if the disclaimer should be shown at all
     */
    @Column(name = "active")
    private boolean active = false;

    /**
     * The time after which a user must have agreed to the disclaimer. If the browser's local storage contains a different value, the cookie banner
     * must be accepted again
     */
    @Column(name = "requires_consent_after", nullable = false)
    private LocalDateTime requiresConsentAfter = LocalDateTime.now();

    /**
     * The scope within which accepting the disclaimer modal is valid for any user
     */
    @Column(name = "acceptance_scope", nullable = false)
    @Convert(converter = ConsentScopeConverter.class)
    private ConsentScope acceptanceScope = new ConsentScope();

    /**
     * The scope within which accepting the disclaimer modal is valid for any user
     */
    @Column(name = "display_scope", nullable = true)
    @Convert(converter = DisplayScopeConverter.class)
    private DisplayScope displayScope = new DisplayScope(PageScope.RECORD, "");

    /**
     * Empty default constructor
     */
    public Disclaimer() {

    }

    /**
     * cloning constructor
     * 
     * @param orig
     */
    public Disclaimer(Disclaimer orig) {
        this.active = orig.active;
        this.id = orig.id;
        this.requiresConsentAfter = LocalDateTime.from(orig.requiresConsentAfter);
        this.text = new TranslatedText(orig.text);
        this.displayScope = new DisplayScope(orig.displayScope.getAsJson());
        this.acceptanceScope = new ConsentScope(orig.acceptanceScope.toString());
    }

    /**
     * database id
     * 
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * set the database id
     * 
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * the text to show in the disclaimer
     * 
     * @return the text
     */
    public TranslatedText getText() {
        return text;
    }

    /**
     * set the disclaimer text
     * 
     * @param text the text to set
     */
    public void setText(TranslatedText text) {
        this.text = text;
    }

    /**
     * get if the disclaimer is active
     * 
     * @return true if the disclaimer is to be shown at all
     */
    public boolean isActive() {
        return active;
    }

    /**
     * set the disclaimer to active/inactive state
     * 
     * @param active the active to set
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * get the date after which acceptance of the disclaimer must have happened to be valid
     * 
     * @return a date
     */
    public LocalDateTime getRequiresConsentAfter() {
        return requiresConsentAfter;
    }

    /**
     * set the date after which acceptance of the disclaimer must have happened to be valid
     * 
     * @param requiresConsentAfter the requiresConsentAfter to set
     */
    public void setRequiresConsentAfter(LocalDateTime requiresConsentAfter) {
        this.requiresConsentAfter = requiresConsentAfter;
    }

    public DisplayScope getDisplayScope() {
        return displayScope;
    }

    public void setDisplayScope(DisplayScope displayScope) {
        this.displayScope = displayScope;
    }

    /**
     * get the {@link #acceptanceScope} of the disclaimer
     * 
     * @return the {@link ConsentScope}
     */
    public ConsentScope getAcceptanceScope() {
        return this.acceptanceScope;
    }

    /**
     * set the {@link #acceptanceScope} of the disclaimer
     * 
     * @param acceptanceScope a {@link ConsentScope}
     */
    public void setAcceptanceScope(ConsentScope acceptanceScope) {
        this.acceptanceScope = acceptanceScope;
    }

}
