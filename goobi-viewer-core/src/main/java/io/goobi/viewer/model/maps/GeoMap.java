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

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.persistence.annotations.PrivateOwned;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import io.goobi.viewer.api.rest.serialization.TranslationListSerializer;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.translations.IPolyglott;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * @author florian
 *
 */
@Entity
@Table(name = "cms_geomap")
public class GeoMap implements Serializable {

    private static final long serialVersionUID = -117775783802522686L;

    private static final Logger logger = LogManager.getLogger(GeoMap.class);

    /**
     * Placeholder User if the actual creator could not be determined
     */
    private static final User CREATOR_UNKNOWN = new User("Unknown");

    private static final String METADATA_TAG_TITLE = "Title";
    private static final String METADATA_TAG_DESCRIPTION = "Description";

    public enum GeoMapType {
        SOLR_QUERY,
        MANUAL
    }

    @Transient
    private final transient Object lockTranslations = new Object();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "geomap_id")
    private Long id;

    @Column(name = "creator_id")
    private Long creatorId;

    @Transient
    private User creator;

    /** Translated metadata. */
    @OneToMany(mappedBy = "owner", fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
    @PrivateOwned
    @JsonSerialize(using = TranslationListSerializer.class)
    private Set<MapTranslation> translations = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
    @JoinColumn(name = "owner_geomap")
    @PrivateOwned
    private List<FeatureSet> featureSets = new ArrayList<>();

    @Column(name = "date_created", nullable = false)
    @JsonIgnore
    private LocalDateTime dateCreated;

    @Column(name = "date_updated")
    @JsonIgnore
    private LocalDateTime dateUpdated;

    @Column(name = "initial_view")
    private String initialView = null;

    @Transient
    private boolean showPopover = true;

    /**
     * Empty Constructor
     */
    public GeoMap() {
        //
    }

    /**
     * Clone constructor
     *
     * @param blueprint
     */
    public GeoMap(GeoMap blueprint) {
        this.creatorId = blueprint.creatorId;
        this.creator = blueprint.creator;
        this.dateCreated = blueprint.dateCreated;
        this.dateUpdated = blueprint.dateUpdated;
        this.id = blueprint.id;
        this.translations = blueprint.translations; //.stream().filter(t -> !t.isEmpty()).map(t -> new MapTranslation(t)).collect(Collectors.toSet());
        this.featureSets = blueprint.featureSets.stream().map(FeatureSet::copy).collect(Collectors.toList());
        this.initialView = blueprint.initialView;

    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @return the creatorId
     */
    public Long getCreatorId() {
        return creatorId;
    }

    /**
     * @param creatorId the creatorId to set
     */
    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
        this.creator = null;
    }

    public User getCreator() {
        if (this.creator == null) {
            this.creator = CREATOR_UNKNOWN;
            try {
                this.creator = DataManager.getInstance().getDao().getUser(this.creatorId);
            } catch (DAOException e) {
                logger.error("Error getting creator ", e);
            }
        }
        return this.creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
        if (creator != null) {
            this.creatorId = creator.getId();
        } else {
            this.creatorId = null;
        }
    }

    /**
     * @return the dateCreated
     */
    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    /**
     * @return the dateUpdated
     */
    public LocalDateTime getDateUpdated() {
        return dateUpdated;
    }

    /**
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * @param dateUpdated the dateUpdated to set
     */
    public void setDateUpdated(LocalDateTime dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    public String getTitle() {
        MapTranslation title = getTitle(IPolyglott.getCurrentLocale().getLanguage());
        if (title.isEmpty()) {
            title = getTitle(IPolyglott.getDefaultLocale().getLanguage());
        }
        return title.getValue();
    }

    public String getDescription() {
        MapTranslation desc = getDescription(BeanUtils.getNavigationHelper().getLocale().getLanguage());
        if (desc.isEmpty()) {
            desc = getDescription(BeanUtils.getNavigationHelper().getDefaultLocale().getLanguage());
        }
        return desc.getValue();
    }

    public MapTranslation getTitle(String language) {
        synchronized (lockTranslations) {
            MapTranslation title = translations.stream()
                    .filter(t -> METADATA_TAG_TITLE.equals(t.getTag()))
                    .filter(t -> language.equals(t.getLanguage()))
                    .findFirst()
                    .orElse(null);
            if (title == null) {
                title = new MapTranslation(language, METADATA_TAG_TITLE, this);
                translations.add(title);
            }
            return title;
        }
    }

    public MapTranslation getDescription(String language) {
        synchronized (lockTranslations) {
            MapTranslation title = translations.stream()
                    .filter(t -> METADATA_TAG_DESCRIPTION.equals(t.getTag()))
                    .filter(t -> language.equals(t.getLanguage()))
                    .findFirst()
                    .orElse(null);
            if (title == null) {
                title = new MapTranslation(language, METADATA_TAG_DESCRIPTION, this);
                translations.add(title);
            }
            return title;
        }
    }

    /**
     * @param initialView the initialView to set
     */
    public void setInitialView(String initialView) {
        this.initialView = initialView;
    }

    /**
     * @return the initialView or the default view from the config if no initial view has been set
     */
    public String getInitialView() {
        if (StringUtils.isBlank(initialView)) {
            return DataManager.getInstance().getConfiguration().getGeomapDefaultView().getGeoJson();
        }
        return initialView;
    }

    /**
     * Link to the html page to render for oembed
     *
     * @return {@link URI}
     */
    public URI getOEmbedLink() {
        return URI.create(BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/embed/map/" + getId() + "/");
    }

    /**
     * 
     * @return {@link URI}
     */
    public URI getOEmbedURI() {
        return getOEmbedURI(null);
    }

    /**
     * 
     * @param linkTarget
     * @return {@link URI}
     */
    public URI getOEmbedURI(String linkTarget) {
        try {
            String linkURI = getOEmbedLink().toString();
            if (StringUtils.isNotBlank(linkTarget)) {
                linkURI += "?linkTarget=" + linkTarget;
            }
            String escLinkURI = URLEncoder.encode(linkURI, "utf-8");
            return URI.create(BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/oembed?url=" + escLinkURI + "&format=json");
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * @param showPopover the showPopover to set
     */
    public void setShowPopover(boolean showPopover) {
        this.showPopover = showPopover;
    }

    /**
     * @return the showPopover
     */
    public boolean isShowPopover() {
        return showPopover;
    }

    /**
     * Resets the cached feature string.
     * 
     * @return {@link IMetadataValue}
     */
    public IMetadataValue getTitles() {
        synchronized (lockTranslations) {
            Map<String, String> titles = translations.stream()
                    .filter(t -> METADATA_TAG_TITLE.equals(t.getTag()))
                    .filter(t -> !t.isEmpty())
                    .collect(Collectors.toMap(MapTranslation::getLanguage, MapTranslation::getValue));
            return new MultiLanguageMetadataValue(titles);
        }
    }

    public IMetadataValue getDescriptions() {
        synchronized (lockTranslations) {
            Map<String, String> titles = translations.stream()
                    .filter(t -> METADATA_TAG_DESCRIPTION.equals(t.getTag()))
                    .filter(t -> !t.isEmpty())
                    .collect(Collectors.toMap(MapTranslation::getLanguage, MapTranslation::getValue));
            return new MultiLanguageMetadataValue(titles);
        }
    }

    public List<FeatureSet> getFeatureSets() {
        return featureSets;
    }

    public void setFeatureSets(List<FeatureSet> featureSets) {
        this.featureSets = new ArrayList<>(featureSets);
    }

    public void addFeatureSet(FeatureSet set) {
        this.featureSets.add(set);
    }

    public void removeFeatureSet(FeatureSet set) {
        this.featureSets.remove(set);
    }

    public boolean hasFeatures() {
        return this.featureSets.stream().anyMatch(FeatureSet::hasFeatures);
    }

    public boolean shouldOpenPopoversOnHover() {
        return this.featureSets.stream().allMatch(f -> f.isQueryResultSet());
    }
}
