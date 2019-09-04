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
package io.goobi.viewer.model.crowdsourcing.campaigns;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.eclipse.persistence.annotations.PrivateOwned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.crowdsourcing.questions.Question;
import io.goobi.viewer.model.misc.Translation;

@Entity
@Table(name = "cs_campaigns")
@JsonInclude(Include.NON_EMPTY)
public class Campaign {

    public enum CampaignVisibility {
        PRIVATE,
        PUBLIC
    }

    private static final Logger logger = LoggerFactory.getLogger(Campaign.class);

    private static final String URI_ID_TEMPLATE = DataManager.getInstance().getConfiguration().getRestApiUrl() + "crowdsourcing/campaigns/{id}";
    private static final String URI_ID_REGEX = ".*/crowdsourcing/campaigns/(\\d+)/?$";
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "campaign_id")
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_created", nullable = false)
    private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_updated")
    private Date dateUpdated;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private CampaignVisibility visibility = CampaignVisibility.PRIVATE;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_start")
    private Date dateStart;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_end")
    private Date dateEnd;

    @Column(name = "image_file_name")
    private String imageFileName;

    @Column(name = "solr_query", nullable = false)
    private String solrQuery;

    @Column(name = "permalink")
    private String permalink;

    @OneToMany(mappedBy = "owner", fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
    @PrivateOwned
    private List<CampaignTranslation> translations = new ArrayList<>();

    @OneToMany(mappedBy = "owner", fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
    @PrivateOwned
    private List<Question> questions = new ArrayList<>();

    @Transient
    private Locale selectedLocale;

    public Campaign() {
        this.selectedLocale = Locale.ENGLISH;
    }

    public Campaign(Locale selectedLocale) {
        this.selectedLocale = selectedLocale;
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getTitle() {
        return Translation.getTranslation(translations, selectedLocale.getLanguage(), "title");
    }

    /**
     * 
     * @param title
     * @should set value correctly
     */
    public void setTitle(String title) {
        CampaignTranslation.setTranslation(translations, selectedLocale.getLanguage(), title, "title", this);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getMenuTitle() {
        return Translation.getTranslation(translations, selectedLocale.getLanguage(), "menu_title");
    }

    /**
     * 
     * @param title
     * @should set value correctly
     */
    public void setMenuTitle(String menuTitle) {
        CampaignTranslation.setTranslation(translations, selectedLocale.getLanguage(), menuTitle, "menu_title", this);
    }

    /**
     * 
     * @return
     * @should return correct value
     */
    public String getDescription() {
        return Translation.getTranslation(translations, selectedLocale.getLanguage(), "description");
    }

    /**
     * 
     * @param title
     * @should set value correctly
     */
    public void setDescription(String description) {
        CampaignTranslation.setTranslation(translations, selectedLocale.getLanguage(), description, "description", this);
    }

    /**
     * 
     * @param lang
     * @return
     */
    public String getTitle(String lang) {
        return Translation.getTranslation(translations, lang, "title");
    }

    /**
     * 
     * @param lang
     * @return
     */
    public String getDescription(String lang) {
        return Translation.getTranslation(translations, lang, "description");
    }

    /**
     * 
     * @param lang
     * @return
     */
    public String getMenuTitle(String lang) {
        return Translation.getTranslation(translations, lang, "menu_title");
    }

    /**
     * @return the id
     */
    @JsonIgnore
    public Long getId() {
        return id;
    }
    
    public static Long getId(URI idAsURI) {
        Matcher matcher = Pattern.compile(URI_ID_REGEX).matcher(idAsURI.toString());
        if(matcher.find()) {
            String idString = matcher.group(1);
            return Long.parseLong(idString);
        } else {
            return null;
        }
    }
    
    @JsonIgnore
    public URI getIdAsURI() {
        return URI.create(URI_ID_TEMPLATE.replace("{id}", this.getId().toString()));
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the dateCreated
     */
    public Date getDateCreated() {
        return dateCreated;
    }

    /**
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * @return the dateUpdated
     */
    public Date getDateUpdated() {
        return dateUpdated;
    }

    /**
     * @param dateUpdated the dateUpdated to set
     */
    public void setDateUpdated(Date dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    /**
     * @return the visibility
     */
    public CampaignVisibility getVisibility() {
        return visibility;
    }

    /**
     * @param visibility the visibility to set
     */
    public void setVisibility(CampaignVisibility visibility) {
        this.visibility = visibility;
    }

    /**
     * @return the dateStart
     */
    public Date getDateStart() {
        return dateStart;
    }

    /**
     * @param dateStart the dateStart to set
     */
    public void setDateStart(Date dateStart) {
        this.dateStart = dateStart;
    }

    /**
     * @return the dateEnd
     */
    public Date getDateEnd() {
        return dateEnd;
    }

    /**
     * @param dateEnd the dateEnd to set
     */
    public void setDateEnd(Date dateEnd) {
        this.dateEnd = dateEnd;
    }

    /**
     * @return the imageFileName
     */
    public String getImageFileName() {
        return imageFileName;
    }

    /**
     * @param imageFileName the imageFileName to set
     */
    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
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

    /**
     * @return the permalink
     */
    public String getPermalink() {
        return permalink;
    }

    /**
     * @param permalink the permalink to set
     */
    public void setPermalink(String permalink) {
        this.permalink = permalink;
    }

    /**
     * @return the translations
     */
    public List<CampaignTranslation> getTranslations() {
        return translations;
    }

    /**
     * @param translations the translations to set
     */
    public void setTranslations(List<CampaignTranslation> translations) {
        this.translations = translations;
    }

    /**
     * @return the questions
     */
    public List<Question> getQuestions() {
        return questions;
    }

    /**
     * @param questions the questions to set
     */
    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    /**
     * @return the selectedLocale
     */
    public Locale getSelectedLocale() {
        return selectedLocale;
    }

    /**
     * @param selectedLocale the selectedLocale to set
     */
    public void setSelectedLocale(Locale selectedLocale) {
        this.selectedLocale = selectedLocale;
    }
}
