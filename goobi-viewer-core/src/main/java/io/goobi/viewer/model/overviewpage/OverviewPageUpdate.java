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
package io.goobi.viewer.model.overviewpage;

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

import io.goobi.viewer.model.security.user.User;

/**
 * Database entry for overview page history updates.
 */
@Deprecated
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
     * <p>Getter for the field <code>id</code>.</p>
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * <p>Setter for the field <code>id</code>.</p>
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * <p>Getter for the field <code>dateUpdated</code>.</p>
     *
     * @return the dateUpdated
     */
    public Date getDateUpdated() {
        return dateUpdated;
    }

    /**
     * <p>Getter for the field <code>pi</code>.</p>
     *
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * <p>Setter for the field <code>pi</code>.</p>
     *
     * @param pi the pi to set
     */
    public void setPi(String pi) {
        this.pi = pi;
    }

    /**
     * <p>Setter for the field <code>dateUpdated</code>.</p>
     *
     * @param dateUpdated the dateUpdated to set
     */
    public void setDateUpdated(Date dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    /**
     * <p>Getter for the field <code>updatedBy</code>.</p>
     *
     * @return the updatedBy
     */
    public User getUpdatedBy() {
        return updatedBy;
    }

    /**
     * <p>Setter for the field <code>updatedBy</code>.</p>
     *
     * @param updatedBy the updatedBy to set
     */
    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * <p>Getter for the field <code>config</code>.</p>
     *
     * @return the config
     */
    public String getConfig() {
        return config;
    }

    /**
     * <p>Setter for the field <code>config</code>.</p>
     *
     * @param config the config to set
     */
    public void setConfig(String config) {
        this.config = config;
    }

    /**
     * <p>isMetadataChanged.</p>
     *
     * @return the metadataChanged
     */
    public boolean isMetadataChanged() {
        return metadataChanged;
    }

    /**
     * <p>Setter for the field <code>metadataChanged</code>.</p>
     *
     * @param metadataChanged the metadataChanged to set
     */
    public void setMetadataChanged(boolean metadataChanged) {
        this.metadataChanged = metadataChanged;
    }

    /**
     * <p>isDescriptionChanged.</p>
     *
     * @return the descriptionChanged
     */
    public boolean isDescriptionChanged() {
        return descriptionChanged;
    }

    /**
     * <p>Setter for the field <code>descriptionChanged</code>.</p>
     *
     * @param descriptionChanged the descriptionChanged to set
     */
    public void setDescriptionChanged(boolean descriptionChanged) {
        this.descriptionChanged = descriptionChanged;
    }

    /**
     * <p>isPublicationTextChanged.</p>
     *
     * @return the publicationTextChanged
     */
    public boolean isPublicationTextChanged() {
        return publicationTextChanged;
    }

    /**
     * <p>Setter for the field <code>publicationTextChanged</code>.</p>
     *
     * @param publicationTextChanged the publicationTextChanged to set
     */
    public void setPublicationTextChanged(boolean publicationTextChanged) {
        this.publicationTextChanged = publicationTextChanged;
    }
}
