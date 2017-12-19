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
package de.intranda.digiverso.presentation.model.overviewpage;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.eclipse.persistence.annotations.Index;

import de.intranda.digiverso.presentation.model.security.user.User;

/**
 * Database entry for overview page history updates.
 */
@Entity
@Table(name = "overview_page_updates")
public class OverviewPageUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "overview_page_update_id")
    private Long id;

    @Index(name = "index_overview_page_updates_pi")
    @Column(name = "pi", nullable = false)
    private String pi;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_updated", nullable = false)
    private Date dateUpdated;

    @JoinColumn(name = "updated_by", nullable = false)
    private User updatedBy;

    @Column(name = "config", columnDefinition = "LONGTEXT")
    private String config;

    @Column(name = "metadataChanged", nullable = false)
    private boolean metadataChanged = false;

    @Column(name = "descriptionChanged", nullable = false)
    private boolean descriptionChanged = false;

    @Column(name = "publicationTextChanged", nullable = false)
    private boolean publicationTextChanged = false;

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
     * @return the dateUpdated
     */
    public Date getDateUpdated() {
        return dateUpdated;
    }

    /**
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * @param pi the pi to set
     */
    public void setPi(String pi) {
        this.pi = pi;
    }

    /**
     * @param dateUpdated the dateUpdated to set
     */
    public void setDateUpdated(Date dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    /**
     * @return the updatedBy
     */
    public User getUpdatedBy() {
        return updatedBy;
    }

    /**
     * @param updatedBy the updatedBy to set
     */
    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * @return the config
     */
    public String getConfig() {
        return config;
    }

    /**
     * @param config the config to set
     */
    public void setConfig(String config) {
        this.config = config;
    }

    /**
     * @return the metadataChanged
     */
    public boolean isMetadataChanged() {
        return metadataChanged;
    }

    /**
     * @param metadataChanged the metadataChanged to set
     */
    public void setMetadataChanged(boolean metadataChanged) {
        this.metadataChanged = metadataChanged;
    }

    /**
     * @return the descriptionChanged
     */
    public boolean isDescriptionChanged() {
        return descriptionChanged;
    }

    /**
     * @param descriptionChanged the descriptionChanged to set
     */
    public void setDescriptionChanged(boolean descriptionChanged) {
        this.descriptionChanged = descriptionChanged;
    }

    /**
     * @return the publicationTextChanged
     */
    public boolean isPublicationTextChanged() {
        return publicationTextChanged;
    }

    /**
     * @param publicationTextChanged the publicationTextChanged to set
     */
    public void setPublicationTextChanged(boolean publicationTextChanged) {
        this.publicationTextChanged = publicationTextChanged;
    }
}
