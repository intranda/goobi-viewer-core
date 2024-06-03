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
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
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
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * <p>
 * License class.
 * </p>
 */
@Entity
@Table(name = "licenses")
public class License extends AbstractPrivilegeHolder implements Serializable {

    private static final long serialVersionUID = 1363557138283960150L;

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(License.class);

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
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

    @JoinColumn(name = "license_type_id")
    private LicenseType licenseType;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "user_group_id")
    private UserGroup userGroup;

    @ManyToOne
    @JoinColumn(name = "ip_range_id")
    private IpRange ipRange;

    @ManyToOne
    @JoinColumn(name = "client_id")
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
    private String type;

    @Transient
    private Set<String> privilegesCopy = new HashSet<>();

    @Transient
    private transient List<Selectable<String>> selectableSubthemes = null;

    @Transient
    private transient List<Selectable<CMSCategory>> selectableCategories = null;

    @Transient
    private transient List<Selectable<CMSPageTemplate>> selectableTemplates = null;

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
     * @param privilege
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
     * @param privilege
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivCms()
     */
    /** {@inheritDoc} */
    @Override
    public boolean isPrivCmsPages() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_PAGES);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivCmsPages(boolean)
     */
    /** {@inheritDoc} */
    @Override
    public void setPrivCmsPages(boolean priv) {
        if (priv) {
            privilegesCopy.add(IPrivilegeHolder.PRIV_CMS_PAGES);
        } else {
            privilegesCopy.remove(IPrivilegeHolder.PRIV_CMS_PAGES);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivCmsAllSubthemes()
     */
    /** {@inheritDoc} */
    @Override
    public boolean isPrivCmsAllSubthemes() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_ALL_SUBTHEMES);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivCmsAllSubthemes(boolean)
     */
    /** {@inheritDoc} */
    @Override
    public void setPrivCmsAllSubthemes(boolean priv) {
        if (priv) {
            privilegesCopy.add(IPrivilegeHolder.PRIV_CMS_ALL_SUBTHEMES);
        } else {
            privilegesCopy.remove(IPrivilegeHolder.PRIV_CMS_ALL_SUBTHEMES);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivCmsAllCategories()
     */
    /** {@inheritDoc} */
    @Override
    public boolean isPrivCmsAllCategories() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_ALL_CATEGORIES);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivCmsAllCategories(boolean)
     */
    /** {@inheritDoc} */
    @Override
    public void setPrivCmsAllCategories(boolean priv) {
        if (priv) {
            privilegesCopy.add(IPrivilegeHolder.PRIV_CMS_ALL_CATEGORIES);
        } else {
            privilegesCopy.remove(IPrivilegeHolder.PRIV_CMS_ALL_CATEGORIES);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivCmsAllTemplates()
     */
    /** {@inheritDoc} */
    @Override
    public boolean isPrivCmsAllTemplates() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_ALL_TEMPLATES);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivCmsAllTemplates(boolean)
     */
    /** {@inheritDoc} */
    @Override
    public void setPrivCmsAllTemplates(boolean priv) {
        if (priv) {
            privilegesCopy.add(IPrivilegeHolder.PRIV_CMS_ALL_TEMPLATES);
        } else {
            privilegesCopy.remove(IPrivilegeHolder.PRIV_CMS_ALL_TEMPLATES);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivCmsMenu()
     */
    /** {@inheritDoc} */
    @Override
    public boolean isPrivCmsMenu() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_MENU);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivCmsMenu(boolean)
     */
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

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivCmsStaticPages()
     */
    /** {@inheritDoc} */
    @Override
    public boolean isPrivCmsStaticPages() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_STATIC_PAGES);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivCmsStaticPages(boolean)
     */
    /** {@inheritDoc} */
    @Override
    public void setPrivCmsStaticPages(boolean priv) {
        if (priv) {
            privilegesCopy.add(IPrivilegeHolder.PRIV_CMS_STATIC_PAGES);
        } else {
            privilegesCopy.remove(IPrivilegeHolder.PRIV_CMS_STATIC_PAGES);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivCmsCollections()
     */
    /** {@inheritDoc} */
    @Override
    public boolean isPrivCmsCollections() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_COLLECTIONS);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivCmsCollections(boolean)
     */
    /** {@inheritDoc} */
    @Override
    public void setPrivCmsCollections(boolean priv) {
        if (priv) {
            privilegesCopy.add(IPrivilegeHolder.PRIV_CMS_COLLECTIONS);
        } else {
            privilegesCopy.remove(IPrivilegeHolder.PRIV_CMS_COLLECTIONS);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivCmsCategories()
     */
    /** {@inheritDoc} */
    @Override
    public boolean isPrivCmsCategories() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_CATEGORIES);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivCmsCategories(boolean)
     */
    /** {@inheritDoc} */
    @Override
    public void setPrivCmsCategories(boolean priv) {
        if (priv) {
            privilegesCopy.add(IPrivilegeHolder.PRIV_CMS_CATEGORIES);
        } else {
            privilegesCopy.remove(IPrivilegeHolder.PRIV_CMS_CATEGORIES);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivCrowdsourcingAllCampaigns()
     */
    /** {@inheritDoc} */
    @Override
    public boolean isPrivCrowdsourcingAllCampaigns() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CROWDSOURCING_ALL_CAMPAIGNS);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivCrowdsourcingAllCampaigns(boolean)
     */
    /** {@inheritDoc} */
    @Override
    public void setPrivCrowdsourcingAllCampaigns(boolean priv) {
        if (priv) {
            privilegesCopy.add(IPrivilegeHolder.PRIV_CROWDSOURCING_ALL_CAMPAIGNS);
        } else {
            privilegesCopy.remove(IPrivilegeHolder.PRIV_CROWDSOURCING_ALL_CAMPAIGNS);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivCrowdsourcingAnnotateCampaign()
     */
    /** {@inheritDoc} */
    @Override
    public boolean isPrivCrowdsourcingAnnotateCampaign() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CROWDSOURCING_ANNOTATE_CAMPAIGN);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivCrowdsourcingAnnotateCampaign(boolean)
     */
    /** {@inheritDoc} */
    @Override
    public void setPrivCrowdsourcingAnnotateCampaign(boolean priv) {
        if (priv) {
            privilegesCopy.add(IPrivilegeHolder.PRIV_CROWDSOURCING_ANNOTATE_CAMPAIGN);
        } else {
            privilegesCopy.remove(IPrivilegeHolder.PRIV_CROWDSOURCING_ANNOTATE_CAMPAIGN);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivCrowdsourcingReviewCampaign()
     */
    /** {@inheritDoc} */
    @Override
    public boolean isPrivCrowdsourcingReviewCampaign() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CROWDSOURCING_REVIEW_CAMPAIGN);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivCrowdsourcingReviewCampaign(boolean)
     */
    /** {@inheritDoc} */
    @Override
    public void setPrivCrowdsourcingReviewCampaign(boolean priv) {
        if (priv) {
            privilegesCopy.add(IPrivilegeHolder.PRIV_CROWDSOURCING_REVIEW_CAMPAIGN);
        } else {
            privilegesCopy.remove(IPrivilegeHolder.PRIV_CROWDSOURCING_REVIEW_CAMPAIGN);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivViewUgc()
     */
    @Override
    public boolean isPrivViewUgc() {
        return hasPrivilege(IPrivilegeHolder.PRIV_VIEW_UGC);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivViewUgc(boolean)
     */
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
     * @param privileges
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
     * @param excludePrivileges
     * @param sourcePrivileges
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
                            .collect(Collectors.toList());
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
                    allCategories.stream().map(cat -> new Selectable<>(cat, this.allowedCategories.contains(cat))).collect(Collectors.toList());
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
                            .collect(Collectors.toList());
        }
        return selectableTemplates;
    }

    /**
     * <p>
     * Getter for the field <code>id</code>.
     * </p>
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * <p>
     * Setter for the field <code>id</code>.
     * </p>
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * <p>
     * Getter for the field <code>licenseType</code>.
     * </p>
     *
     * @return the licenseType
     */
    public LicenseType getLicenseType() {
        return licenseType;
    }

    /**
     * <p>
     * Setter for the field <code>licenseType</code>.
     * </p>
     *
     * @param licenseType the licenseType to set
     */
    public void setLicenseType(LicenseType licenseType) {
        this.licenseType = licenseType;
    }

    /**
     * <p>
     * Getter for the field <code>user</code>.
     * </p>
     *
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * <p>
     * Setter for the field <code>user</code>.
     * </p>
     *
     * @param user the user to set
     * @should set userGroup and ipRange to null if user not null
     * @should not set userGroup and ipRange to null if user null
     */
    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.userGroup = null;
            this.ipRange = null;
        }
    }

    /**
     * <p>
     * Getter for the field <code>userGroup</code>.
     * </p>
     *
     * @return the userGroup
     */
    public UserGroup getUserGroup() {
        return userGroup;
    }

    /**
     * <p>
     * Setter for the field <code>userGroup</code>.
     * </p>
     *
     * @param userGroup the userGroup to set
     * @should set user and ipRange to null if userGroup not null
     * @should not set user and ipRange to null if userGroup null
     */
    public void setUserGroup(UserGroup userGroup) {
        this.userGroup = userGroup;
        if (userGroup != null) {
            this.user = null;
            this.ipRange = null;
        }
    }

    /**
     * <p>
     * Getter for the field <code>ipRange</code>.
     * </p>
     *
     * @return the ipRange
     */
    public IpRange getIpRange() {
        return ipRange;
    }

    /**
     * <p>
     * Setter for the field <code>ipRange</code>.
     * </p>
     *
     * @param ipRange the ipRange to set
     * @should set user and userGroup to null if ipRange not null
     * @should not set user and userGroup to null if ipRange null
     */
    public void setIpRange(IpRange ipRange) {
        this.ipRange = ipRange;
        if (ipRange != null) {
            this.user = null;
            this.userGroup = null;
        }
    }

    /**
     * <p>
     * Getter for the field <code>start</code>.
     * </p>
     *
     * @return the start
     */
    public LocalDateTime getStart() {
        return start;
    }

    /**
     * <p>
     * Setter for the field <code>start</code>.
     * </p>
     *
     * @param start the start to set
     */
    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    /**
     * <p>
     * Getter for the field <code>end</code>.
     * </p>
     *
     * @return the end
     */
    public LocalDateTime getEnd() {
        return end;
    }

    /**
     * <p>
     * Setter for the field <code>end</code>.
     * </p>
     *
     * @param end the end to set
     */
    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

    /**
     * <p>
     * Getter for the field <code>privileges</code>.
     * </p>
     *
     * @return the privileges
     */
    public Set<String> getPrivileges() {
        return privileges;
    }

    /**
     * <p>
     * Setter for the field <code>privileges</code>.
     * </p>
     *
     * @param privileges the privileges to set
     */
    public void setPrivileges(Set<String> privileges) {
        this.privileges = privileges;
    }

    /**
     * <p>
     * Getter for the field <code>conditions</code>.
     * </p>
     *
     * @return the conditions
     */
    public String getConditions() {
        return conditions;
    }

    /**
     * <p>
     * Setter for the field <code>conditions</code>.
     * </p>
     *
     * @param conditions the conditions to set
     */
    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    /**
     * <p>
     * Getter for the field <code>description</code>.
     * </p>
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * <p>
     * Setter for the field <code>description</code>.
     * </p>
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the ticketRequired
     */
    public boolean isTicketRequired() {
        return ticketRequired;
    }

    /**
     * @param ticketRequired the ticketRequired to set
     */
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
     * <p>
     * Getter for the field <code>subthemeDiscriminatorValues</code>.
     * </p>
     *
     * @return the subthemeDiscriminatorValues
     */
    public List<String> getSubthemeDiscriminatorValues() {
        return subthemeDiscriminatorValues;
    }

    /**
     * <p>
     * Setter for the field <code>subthemeDiscriminatorValues</code>.
     * </p>
     *
     * @param subthemeDiscriminatorValues the subthemeDiscriminatorValues to set
     */
    public void setSubthemeDiscriminatorValues(List<String> subthemeDiscriminatorValues) {
        this.subthemeDiscriminatorValues = subthemeDiscriminatorValues;
    }

    /**
     * <p>
     * Getter for the field <code>allowedCategories</code>.
     * </p>
     *
     * @return the allowedCategories
     */
    public List<CMSCategory> getAllowedCategories() {
        return allowedCategories;
    }

    /**
     * <p>
     * Setter for the field <code>allowedCategories</code>.
     * </p>
     *
     * @param allowedCategories the allowedCategories to set
     */
    public void setAllowedCategories(List<CMSCategory> allowedCategories) {
        this.allowedCategories = allowedCategories;
    }

    /**
     * <p>
     * Getter for the field <code>allowedCmsTemplates</code>.
     * </p>
     *
     * @return the allowedCmsTemplates
     */
    public List<CMSPageTemplate> getAllowedCmsTemplates() {
        return allowedCmsTemplates;
    }

    /**
     * <p>
     * Setter for the field <code>allowedCmsTemplates</code>.
     * </p>
     *
     * @param allowedCmsTemplates the allowedCmsTemplates to set
     */
    public void setAllowedCmsTemplates(List<CMSPageTemplate> allowedCmsTemplates) {
        this.allowedCmsTemplates = allowedCmsTemplates;
    }

    /**
     * <p>
     * Getter for the field <code>allowedCrowdsourcingCampaigns</code>.
     * </p>
     *
     * @return the allowedCrowdsourcingCampaigns
     */
    public List<Campaign> getAllowedCrowdsourcingCampaigns() {
        return allowedCrowdsourcingCampaigns;
    }

    /**
     * <p>
     * Setter for the field <code>allowedCrowdsourcingCampaigns</code>.
     * </p>
     *
     * @param allowedCrowdsourcingCampaigns the allowedCrowdsourcingCampaigns to set
     */
    public void setAllowedCrowdsourcingCampaigns(List<Campaign> allowedCrowdsourcingCampaigns) {
        this.allowedCrowdsourcingCampaigns = allowedCrowdsourcingCampaigns;
    }

    /**
     * @return the type
     */
    public String getType() {
        if (type == null) {
            if (user != null) {
                type = "user";
            } else if (userGroup != null) {
                type = "group";
            } else if (ipRange != null) {
                type = "iprange";
            }
        }
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the privilegesCopy
     */
    public Set<String> getPrivilegesCopy() {
        return privilegesCopy;
    }

    /**
     * @param privilegesCopy the privilegesCopy to set
     */
    public void setPrivilegesCopy(Set<String> privilegesCopy) {
        this.privilegesCopy = privilegesCopy;
    }

    public ConsentScope getDisclaimerScope() {
        return disclaimerScope;
    }

    /**
     * @return the client
     */
    public Long getClientId() {
        return Optional.ofNullable(client).map(ClientApplication::getId).orElse(null);
    }

    public ClientApplication getClient() {
        return this.client;
    }

    /**
     * @param client the client to set
     */
    public void setClient(ClientApplication client) {
        this.client = client;
    }

    /**
     * @param clientId the clientId to set
     * @throws DAOException
     */
    public void setClientId(Long clientId) throws DAOException {
        if (clientId != null) {
            this.client = DataManager.getInstance().getDao().getClientApplication(clientId);
        }
    }
}
