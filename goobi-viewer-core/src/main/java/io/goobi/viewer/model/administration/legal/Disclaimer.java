package io.goobi.viewer.model.administration.legal;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import io.goobi.viewer.dao.converter.TranslatedTextConverter;
import io.goobi.viewer.model.translations.TranslatedText;

/**
 * Class to persist settings for the disclaimer modal.
 * Only one instance of this class should be persisted in the database
 * 
 * @author florian
 *
 */
@Entity
@Table(name = "disclaimer")
public class Disclaimer {
    
    /** Unique database ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
     * The time after which a user must have agreed to the disclaimer. If the browser's local storage contains a different value, the cookie banner must be accepted again
     */
    @Column(name = "requires_consent_after", nullable = false)
    private LocalDateTime requiresConsentAfter = LocalDateTime.now();
  
    
    @Column(name="acceptance_scope")
    private ConsentScope acceptanceScope = new ConsentScope();
    
    @Column(name="solr_query")
    private String solrQuery = "";

    /**
     * Empty default constructor
     */
    public Disclaimer() {
        
    }
    
    public Disclaimer(Disclaimer orig) {
        this.acceptanceScope = orig.acceptanceScope;
        this.active = orig.active;
        this.id = orig.id;
        this.requiresConsentAfter = LocalDateTime.from(orig.requiresConsentAfter);
        this.text = new TranslatedText(orig.text);
        this.solrQuery = orig.solrQuery;
        this.acceptanceScope = orig.acceptanceScope;
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
     * @return the text
     */
    public TranslatedText getText() {
        return text;
    }




    /**
     * @param text the text to set
     */
    public void setText(TranslatedText text) {
        this.text = text;
    }




    /**
     * @return the active
     */
    public boolean isActive() {
        return active;
    }




    /**
     * @param active the active to set
     */
    public void setActive(boolean active) {
        this.active = active;
    }




    /**
     * @return the requiresConsentAfter
     */
    public LocalDateTime getRequiresConsentAfter() {
        return requiresConsentAfter;
    }




    /**
     * @param requiresConsentAfter the requiresConsentAfter to set
     */
    public void setRequiresConsentAfter(LocalDateTime requiresConsentAfter) {
        this.requiresConsentAfter = requiresConsentAfter;
    }

    /**
     * @return the solrQuery
     */
    public String getSolrQuery() {
        return solrQuery;
    }

    /**
     * @param solrQuery the solrQuery to set
     */
    public void setSolrQuery(String solrQuery) {
        this.solrQuery = solrQuery;
    }

    public ConsentScope getAcceptanceScope() {
        return this.acceptanceScope;
    }
    
}
