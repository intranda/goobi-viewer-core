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
package io.goobi.viewer.model.security;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.persistence.annotations.PrivateOwned;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.converter.ConsentScopeConverter;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.administration.legal.ConsentScope;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.Selectable;
import io.goobi.viewer.model.cms.pages.CMSPageTemplate;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.security.clients.ClientApplication;
import io.swagger.v3.oas.annotations.media.Schema;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Represents an access licence assigned to a user, user group, or IP range, controlling permissions for specific access conditions.
 */
@Entity
@Table(name = "licenses")
public class License extends AbstractPrivilegeHolder implements Serializable {

    public enum AccessType {
        USER("admin__users"),
        USER_GROUP("admin__groups"),
        IP_RANGE("admin__ip_ranges"),
        CLIENT("admin__clients");

        private final String labelKey;

        /**
         *
         * @param labelKey Message key for the access type label
         */
        private AccessType(String labelKey) {
            this.labelKey = labelKey;
        }

        
        public String getLabelKey() {
            return labelKey;
        }
    }

    private static final long serialVersionUID = 1363557138283960150L;

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(License.class);

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        License other = (License) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "license_id")
    private Long id;

    @JoinColumn(name = "license_type_id") // TODO nullable = false?
    private LicenseType licenseType;

    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE })
    @PrivateOwned
    private List<LicenseRightsHolder> licensees = new ArrayList<>();

    @Deprecated(since = "2026.01")
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Deprecated(since = "2026.01")
    @ManyToOne
    @JoinColumn(name = "user_group_id")
    private UserGroup userGroup;

    @Deprecated(since = "2026.01")
    @ManyToOne
    @JoinColumn(name = "ip_range_id")
    private IpRange ipRange;

    @Deprecated(since = "2026.01")
    @ManyToOne
    @JoinColumn(name = "client_id")
    // Hide from OpenAPI schema to break the circular reference: ClientApplication -> licenses -> License -> client -> ClientApplication
    @Schema(hidden = true)
    private ClientApplication client;

    @Column(name = "date_start")
    private LocalDateTime start;

    @Column(name = "date_end")
    private LocalDateTime end;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "license_privileges", joinColumns = @JoinColumn(name = "license_id"))
    @Column(name = "privilege_name")
    private Set<String> privileges = new HashSet<>();

    @Column(name = "conditions")
    private String conditions;

    @Column(name = "description")
    private String description;

    @Column(name = "ticket_required")
    private boolean ticketRequired = false;

    /** List of allowed subtheme discriminator values for CMS pages. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "license_cms_subthemes", joinColumns = @JoinColumn(name = "license_id"))
    @Column(name = "subtheme_discriminator_value")
    private List<String> subthemeDiscriminatorValues = new ArrayList<>();

    /** List of allowed CMS categories. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "license_cms_categories", joinColumns = @JoinColumn(name = "license_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private List<CMSCategory> allowedCategories = new ArrayList<>();

    /** List of allowed CMS templates. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "license_cms_templates", joinColumns = @JoinColumn(name = "license_id"),
            inverseJoinColumns = @JoinColumn(name = "template_id"))
    private List<CMSPageTemplate> allowedCmsTemplates = new ArrayList<>();

    /** List of allowed crowdsourcing campaigns. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "license_crowdsourcing_campaigns", joinColumns = @JoinColumn(name = "license_id"),
            inverseJoinColumns = @JoinColumn(name = "campaign_id"))
    private List<Campaign> allowedCrowdsourcingCampaigns = new ArrayList<>();

    @Column(name = "legal_disclaimer_scope", nullable = true)
    @Convert(converter = ConsentScopeConverter.class)
    private ConsentScope disclaimerScope = new ConsentScope();

    @Transient
    private Set<String> privilegesCopy = new HashSet<>();

    @Transient
    private transient List<Selectable<String>> selectableSubthemes = null;

    @Transient
    private transient List<Selectable<CMSCategory>> selectableCategories = null;

    @Transient
    private transient List<Selectable<CMSPageTemplate>> selectableTemplates = null;

    /**
     * Zero-arg constructor.
     */
    public License() {
        if (licensees.isEmpty()) {
            licensees.add(new LicenseRightsHolder(this));
        }
    }

    /**
     * Checks the validity of this license. A valid license is either not time limited (start and/or end) or the current date lies between the
     * license's start and and dates.
     *
     * @return true if valid; false otherwise;
     * @should return correct value
     */
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return (start == null || start.isBefore(now)) && (end == null || end.isAfter(now));
    }

    /**
     * Adds the given privilege to the working set.
     *
     * @param privilege Privilege name to add to working set
     * @return true if successful; false otherwise
     */
    @Override
    public boolean addPrivilege(String privilege) {
        logger.debug("addPrivilege: {}", privilege);
        return privilegesCopy.add(privilege);
    }

    /**
     * Removes the given privilege from the working set.
     *
     * @param privilege Privilege name to remove from working set
     * @return true if successful; false otherwise
     */
    @Override
    public boolean removePrivilege(String privilege) {
        logger.debug("removePrivilege: {}", privilege);
        return privilegesCopy.remove(privilege);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasPrivilege(String privilege) {
        return privileges.contains(privilege);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPrivCmsPages() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_PAGES);
    }

    /** {@inheritDoc} */
    @Override
    public void setPrivCmsPages(boolean priv) {
        if (priv) {
            privilegesCopy.add(IPrivilegeHolder.PRIV_CMS_PAGES);
        } else {
            privilegesCopy.remove(IPrivilegeHolder.PRIV_CMS_PAGES);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPrivCmsAllSubthemes() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_ALL_SUBTHEMES);
    }

    /** {@inheritDoc} */
    @Override
    public void setPrivCmsAllSubthemes(boolean priv) {
        if (priv) {
            privilegesCopy.add(IPrivilegeHolder.PRIV_CMS_ALL_SUBTHEMES);
        } else {
            privilegesCopy.remove(IPrivilegeHolder.PRIV_CMS_ALL_SUBTHEMES);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPrivCmsAllCategories() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_ALL_CATEGORIES);
    }

    /** {@inheritDoc} */
    @Override
    public void setPrivCmsAllCategories(boolean priv) {
        if (priv) {
            privilegesCopy.add(IPrivilegeHolder.PRIV_CMS_ALL_CATEGORIES);
        } else {
            privilegesCopy.remove(IPrivilegeHolder.PRIV_CMS_ALL_CATEGORIES);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPrivCmsAllTemplates() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_ALL_TEMPLATES);
    }

    /** {@inheritDoc} */
    @Override
    public void setPrivCmsAllTemplates(boolean priv) {
        if (priv) {
            privilegesCopy.add(IPrivilegeHolder.PRIV_CMS_ALL_TEMPLATES);
        } else {
            privilegesCopy.remove(IPrivilegeHolder.PRIV_CMS_ALL_TEMPLATES);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPrivCmsMenu() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_MENU);
    }

    /** {@inheritDoc} */
    @Override
    public void setPrivCmsMenu(boolean priv) {
        logger.trace("setPrivCmsMenu: {}", priv);
        if (priv) {
            privilegesCopy.add(IPrivilegeHolder.PRIV_CMS_MENU);
        } else {
            privilegesCopy.remove(IPrivilegeHolder.PRIV_CMS_MENU);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPrivCmsStaticPages() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_STATIC_PAGES);
    }

    /** {@inheritDoc} */
    @Override
    public void setPrivCmsStaticPages(boolean priv) {
        if (priv) {
            privilegesCopy.add(IPrivilegeHolder.PRIV_CMS_STATIC_PAGES);
        } else {
            privilegesCopy.remove(IPrivilegeHolder.PRIV_CMS_STATIC_PAGES);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPrivCmsCollections() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_COLLECTIONS);
    }

    /** {@inheritDoc} */
    @Override
    public void setPrivCmsCollections(boolean priv) {
        if (priv) {
            privilegesCopy.add(IPrivilegeHolder.PRIV_CMS_COLLECTIONS);
        } else {
            privilegesCopy.remove(IPrivilegeHolder.PRIV_CMS_COLLECTIONS);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPrivCmsCategories() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_CATEGORIES);
    }

    /** {@inheritDoc} */
    @Override
    public void setPrivCmsCategories(boolean priv) {
        if (priv) {
            privilegesCopy.add(IPrivilegeHolder.PRIV_CMS_CATEGORIES);
        } else {
            privilegesCopy.remove(IPrivilegeHolder.PRIV_CMS_CATEGORIES);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPrivCrowdsourcingAllCampaigns() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CROWDSOURCING_ALL_CAMPAIGNS);
    }

    /** {@inheritDoc} */
    @Override
    public void setPrivCrowdsourcingAllCampaigns(boolean priv) {
        if (priv) {
            privilegesCopy.add(IPrivilegeHolder.PRIV_CROWDSOURCING_ALL_CAMPAIGNS);
        } else {
            privilegesCopy.remove(IPrivilegeHolder.PRIV_CROWDSOURCING_ALL_CAMPAIGNS);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPrivCrowdsourcingAnnotateCampaign() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CROWDSOURCING_ANNOTATE_CAMPAIGN);
    }

    /** {@inheritDoc} */
    @Override
    public void setPrivCrowdsourcingAnnotateCampaign(boolean priv) {
        if (priv) {
            privilegesCopy.add(IPrivilegeHolder.PRIV_CROWDSOURCING_ANNOTATE_CAMPAIGN);
        } else {
            privilegesCopy.remove(IPrivilegeHolder.PRIV_CROWDSOURCING_ANNOTATE_CAMPAIGN);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPrivCrowdsourcingReviewCampaign() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CROWDSOURCING_REVIEW_CAMPAIGN);
    }

    /** {@inheritDoc} */
    @Override
    public void setPrivCrowdsourcingReviewCampaign(boolean priv) {
        if (priv) {
            privilegesCopy.add(IPrivilegeHolder.PRIV_CROWDSOURCING_REVIEW_CAMPAIGN);
        } else {
            privilegesCopy.remove(IPrivilegeHolder.PRIV_CROWDSOURCING_REVIEW_CAMPAIGN);
        }
    }

    @Override
    public boolean isPrivViewUgc() {
        return hasPrivilege(IPrivilegeHolder.PRIV_VIEW_UGC);
    }

    @Override
    public void setPrivViewUgc(boolean priv) {
        if (priv) {
            privilegesCopy.add(IPrivilegeHolder.PRIV_VIEW_UGC);
        } else {
            privilegesCopy.remove(IPrivilegeHolder.PRIV_VIEW_UGC);
        }
    }

    /**
     * Resets all working copies of lists of various privileges.
     */
    public void resetTempData() {
        privilegesCopy.clear();
        selectableSubthemes = null;
        selectableCategories = null;
        selectableTemplates = null;
    }

    /**
     * Returns the list of available record privileges for adding to this license (using the working copy while editing).
     *
     * @return Values in IPrivilegeHolder.PRIVS_RECORD minus the privileges already added
     */
    public List<String> getAvailablePrivileges() {
        return getAvailablePrivileges(privilegesCopy);
    }

    /**
     * Returns the list of available record privileges for adding to this license (using the given privileges list).
     *
     * @param privileges Currently assigned privileges to exclude from result
     * @return Values in IPrivilegeHolder.PRIVS_RECORD minus the privileges already added
     * @should return cms privileges if licenseType cms type
     * @should only return priv view ugc if licenseType ugc type
     * @should return record privileges if licenseType regular
     */
    public List<String> getAvailablePrivileges(Set<String> privileges) {
        if (licenseType != null) {
            if (licenseType.isCmsType()) {
                return getAvailablePrivileges(privileges, Arrays.asList(PRIVS_CMS));
            } else if (licenseType.isUgcType()) {
                return getAvailablePrivileges(privileges, Collections.singletonList(IPrivilegeHolder.PRIV_VIEW_UGC));
            }
        }

        return getAvailablePrivileges(privileges, Arrays.asList(PRIVS_RECORD));
    }

    /**
     *
     * @param excludePrivileges Privileges to exclude from the result
     * @param sourcePrivileges Full list of privileges to select from
     * @return List<String>
     */
    List<String> getAvailablePrivileges(Set<String> excludePrivileges, List<String> sourcePrivileges) {
        if (excludePrivileges == null) {
            throw new IllegalArgumentException("excludePrivileges may not be null");
        }
        if (sourcePrivileges == null || sourcePrivileges.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> ret = new ArrayList<>(sourcePrivileges);
        // Remove existing privileges
        ret.removeAll(excludePrivileges);
        // Remove privileges inherited from the license type
        if (licenseType != null) {
            ret.removeAll(licenseType.getPrivileges());
        }
        return ret;
    }

    /**
     * Returns a sorted list (according to the static array of privileges, either for records or CMS) based on the given set of privileges.
     *
     * @param privileges Listed privileges
     * @return Sorted list of privileges contained in <code>privileges</code>
     */
    @Override
    public List<String> getSortedPrivileges(Set<String> privileges) {
        if (privileges == null || privileges.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> orderList = (licenseType != null && licenseType.isCmsType()) ? Arrays.asList(PRIVS_CMS)
                : Arrays.asList(PRIVS_RECORD);
        List<String> ret = new ArrayList<>(orderList.size());
        for (String priv : orderList) {
            // Skip PRIV_CMS_PAGES
            if (privileges.contains(priv) && !IPrivilegeHolder.PRIV_CMS_PAGES.equals(priv)) {
                ret.add(priv);
            }
        }

        return ret;
    }

    /**
     *
     * @return List&lt;Selectablei&lt;String&gt;&gt;
     * @throws DAOException
     * @throws PresentationException
     */
    public List<Selectable<String>> getSelectableSubthemes() throws PresentationException {
        if (selectableSubthemes == null) {
            List<String> allSubthemes = BeanUtils.getCmsBean().getSubthemeDiscriminatorValues();
            selectableSubthemes =
                    allSubthemes.stream()
                            .map(sub -> new Selectable<>(sub, this.subthemeDiscriminatorValues.contains(sub)))
                            .toList();
        }
        return selectableSubthemes;
    }

    /**
     *
     * @return List&lt;Selectable&lt;CMSCategory&gt;&gt;
     * @throws DAOException
     */
    public List<Selectable<CMSCategory>> getSelectableCategories() throws DAOException {
        if (selectableCategories == null) {
            List<CMSCategory> allCategories = DataManager.getInstance().getDao().getAllCategories();
            selectableCategories =
                    allCategories.stream().map(cat -> new Selectable<>(cat, this.allowedCategories.contains(cat))).toList();
        }
        return selectableCategories;
    }

    /**
     *
     * @return List&lt;Selectable&lt;CMSPageTemplate&gt;&gt;
     * @throws DAOException
     */
    public List<Selectable<CMSPageTemplate>> getSelectableTemplates() throws DAOException {
        if (selectableTemplates == null) {
            List<CMSPageTemplate> allTemplates = DataManager.getInstance().getDao().getAllCMSPageTemplates();
            selectableTemplates =
                    allTemplates.stream()
                            .map(template -> new Selectable<>(template, this.allowedCmsTemplates.contains(template)))
                            .toList();
        }
        return selectableTemplates;
    }

    /**
     * Getter for the field <code>id</code>.
     *

     */
    public Long getId() {
        return id;
    }

    /**
     * Setter for the field <code>id</code>.
     *
     * @param id the database primary key for this license
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Getter for the field <code>licenseType</code>.
     *

     */
    public LicenseType getLicenseType() {
        return licenseType;
    }

    /**
     * Setter for the field <code>licenseType</code>.
     *
     * @param licenseType the license type defining the access conditions and privileges for this license
     */
    public void setLicenseType(LicenseType licenseType) {
        this.licenseType = licenseType;
    }

    
    public List<LicenseRightsHolder> getLicensees() {
        return licensees;
    }

    
    public void setLicensees(List<LicenseRightsHolder> licensees) {
        this.licensees = licensees;
    }

    /**
     * Getter for the field <code>user</code>.
     *

     */
    @Deprecated(since = "2026.01")
    public User getUser() {
        return user;
    }

    /**
     * Setter for the field <code>user</code>.
     *
     * @param user the user this license is granted to (clears userGroup and ipRange when non-null)
     * @should set userGroup and ipRange to null if user not null
     * @should not set userGroup and ipRange to null if user null
     */
    @Deprecated(since = "2026.01")
    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.userGroup = null;
            this.ipRange = null;
        }
    }

    /**
     * Getter for the field <code>userGroup</code>.
     *

     */
    @Deprecated(since = "2026.01")
    public UserGroup getUserGroup() {
        return userGroup;
    }

    /**
     * Setter for the field <code>userGroup</code>.
     *
     * @param userGroup the user group this license is granted to (clears user and ipRange when non-null)
     * @should set user and ipRange to null if userGroup not null
     * @should not set user and ipRange to null if userGroup null
     */
    @Deprecated(since = "2026.01")
    public void setUserGroup(UserGroup userGroup) {
        this.userGroup = userGroup;
        if (userGroup != null) {
            this.user = null;
            this.ipRange = null;
        }
    }

    /**
     * Getter for the field <code>ipRange</code>.
     *

     */
    @Deprecated(since = "2026.01")
    public IpRange getIpRange() {
        return ipRange;
    }

    /**
     * Setter for the field <code>ipRange</code>.
     *
     * @param ipRange the IP range this license is granted to (clears user and userGroup when non-null)
     * @should set user and userGroup to null if ipRange not null
     * @should not set user and userGroup to null if ipRange null
     */
    @Deprecated(since = "2026.01")
    public void setIpRange(IpRange ipRange) {
        this.ipRange = ipRange;
        if (ipRange != null) {
            this.user = null;
            this.userGroup = null;
        }
    }

    /**
     * Getter for the field <code>start</code>.
     *

     */
    public LocalDateTime getStart() {
        return start;
    }

    /**
     * Setter for the field <code>start</code>.
     *
     * @param start the date/time from which this license becomes valid; null means no start restriction
     */
    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    /**
     * Getter for the field <code>end</code>.
     *

     */
    public LocalDateTime getEnd() {
        return end;
    }

    /**
     * Setter for the field <code>end</code>.
     *
     * @param end the date/time after which this license expires; null means no end restriction
     */
    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

    /**
     * Getter for the field <code>privileges</code>.
     *

     */
    public Set<String> getPrivileges() {
        return privileges;
    }

    /**
     * Setter for the field <code>privileges</code>.
     *
     * @param privileges the set of privilege names granted by this license
     */
    public void setPrivileges(Set<String> privileges) {
        this.privileges = privileges;
    }

    /**
     * Getter for the field <code>conditions</code>.
     *

     */
    public String getConditions() {
        return conditions;
    }

    /**
     * Setter for the field <code>conditions</code>.
     *
     * @param conditions the Solr query expression restricting the records this license applies to
     */
    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    /**
     * Getter for the field <code>description</code>.
     *

     */
    public String getDescription() {
        return description;
    }

    /**
     * Setter for the field <code>description</code>.
     *
     * @param description the human-readable description of this license
     */
    public void setDescription(String description) {
        this.description = description;
    }

    
    public boolean isTicketRequired() {
        return ticketRequired;
    }

    
    public void setTicketRequired(boolean ticketRequired) {
        this.ticketRequired = ticketRequired;
    }

    /**
     * 
     * @return true if privilege PRIV_DOWNLOAD_BORN_DIGITAL_FILES is contained; false otherwise
     */
    public boolean isDisplayTicketRequiredToggle() {
        return privilegesCopy.contains(IPrivilegeHolder.PRIV_DOWNLOAD_BORN_DIGITAL_FILES);
    }

    /**
     * Getter for the field <code>subthemeDiscriminatorValues</code>.
     *

     */
    public List<String> getSubthemeDiscriminatorValues() {
        return subthemeDiscriminatorValues;
    }

    /**
     * Setter for the field <code>subthemeDiscriminatorValues</code>.
     *
     * @param subthemeDiscriminatorValues the list of subtheme discriminator values that restrict the scope of this license
     */
    public void setSubthemeDiscriminatorValues(List<String> subthemeDiscriminatorValues) {
        this.subthemeDiscriminatorValues = subthemeDiscriminatorValues;
    }

    /**
     * Getter for the field <code>allowedCategories</code>.
     *

     */
    public List<CMSCategory> getAllowedCategories() {
        return allowedCategories;
    }

    /**
     * Setter for the field <code>allowedCategories</code>.
     *
     * @param allowedCategories the list of CMS categories accessible under this license
     */
    public void setAllowedCategories(List<CMSCategory> allowedCategories) {
        this.allowedCategories = allowedCategories;
    }

    /**
     * Getter for the field <code>allowedCmsTemplates</code>.
     *

     */
    public List<CMSPageTemplate> getAllowedCmsTemplates() {
        return allowedCmsTemplates;
    }

    /**
     * Setter for the field <code>allowedCmsTemplates</code>.
     *
     * @param allowedCmsTemplates the list of CMS page templates accessible under this license
     */
    public void setAllowedCmsTemplates(List<CMSPageTemplate> allowedCmsTemplates) {
        this.allowedCmsTemplates = allowedCmsTemplates;
    }

    /**
     * Getter for the field <code>allowedCrowdsourcingCampaigns</code>.
     *

     */
    public List<Campaign> getAllowedCrowdsourcingCampaigns() {
        return allowedCrowdsourcingCampaigns;
    }

    /**
     * Setter for the field <code>allowedCrowdsourcingCampaigns</code>.
     *
     * @param allowedCrowdsourcingCampaigns the list of crowdsourcing campaigns accessible under this license
     */
    public void setAllowedCrowdsourcingCampaigns(List<Campaign> allowedCrowdsourcingCampaigns) {
        this.allowedCrowdsourcingCampaigns = allowedCrowdsourcingCampaigns;
    }

    
    public Set<String> getPrivilegesCopy() {
        return privilegesCopy;
    }

    
    public void setPrivilegesCopy(Set<String> privilegesCopy) {
        this.privilegesCopy = privilegesCopy;
    }

    public ConsentScope getDisclaimerScope() {
        return disclaimerScope;
    }

    
    @Deprecated(since = "2026.01")
    public Long getClientId() {
        return Optional.ofNullable(client).map(ClientApplication::getId).orElse(null);
    }

    @Deprecated(since = "2026.01")
    public ClientApplication getClient() {
        return this.client;
    }

    
    @Deprecated(since = "2026.01")
    public void setClient(ClientApplication client) {
        this.client = client;
    }

    public void addLicensee() {
        licensees.add(new LicenseRightsHolder(this));
    }

    public void removeLicensee(LicenseRightsHolder licensee) {
        licensees.remove(licensee);
    }

    /**
     * 
     * @return true if at least one of user/userGroup/ipRange/client are non-null; false otherwise
     */
    public boolean isHasLicensees() {
        return !licensees.isEmpty();
    }

    /**
     * 
     * @return true if at least two of user/userGroup/ipRange/client are non-null; false otherwise
     */
    public boolean isHasMultipleLicensees() {
        return licensees.size() > 1;
    }

    /**
     * A.
     * 
     * @return The other non-null member of user/userGroup/ipRange/client
     * @should return correct object
     */
    public ILicensee getSecondaryAccessRequirement() {
        if (licensees.size() > 1) {
            logger.trace("Secondary access requirement found: {}", licensees.get(1).getLicensee().getName());
            return licensees.get(1).getLicensee();
        }

        return null;
    }

    /**
     * Convenience method for disabling the save button.
     * 
     * @return "disabled" if any required values are missing; null otherwise
     * @should return null if all relevant fields filled
     */
    public String getDisabledStatus() {
        if (getLicenseType() == null) {
            return "disabled";
        }

        for (LicenseRightsHolder licensee : licensees) {
            if (licensee.isDisabled()) {
                return "disabled";
            }
        }

        return null;
    }
}
