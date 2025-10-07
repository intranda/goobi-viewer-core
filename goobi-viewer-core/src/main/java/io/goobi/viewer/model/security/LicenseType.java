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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * This class describes license types for record access conditions and also system user roles (not to be confused with the class Role, however), also
 * known as core license types.
 */
@Entity
@Table(name = "license_types")
public class LicenseType extends AbstractPrivilegeHolder implements ILicenseType, Serializable {

    private static final long serialVersionUID = 9206827867178660886L;

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(LicenseType.class);

    // When adding a new static license type name, update isStaticLicenseType()!
    /** Constant <code>LICENSE_TYPE_SET_REPRESENTATIVE_IMAGE="licenseType_setRepresentativeImage"</code> */
    public static final String LICENSE_TYPE_SET_REPRESENTATIVE_IMAGE = "licenseType_setRepresentativeImage";
    /** Constant <code>LICENSE_TYPE_DELETE_OCR_PAGE="licenseType_deleteOcrPage"</code> */
    public static final String LICENSE_TYPE_DELETE_OCR_PAGE = "licenseType_deleteOcrPage";
    private static final String LICENSE_TYPE_SET_REPRESENTATIVE_IMAGE_DESCRIPTION = "licenseType_setRepresentativeImage_desc";
    private static final String LICENSE_TYPE_DELETE_OCR_PAGE_DESCRIPTION = "licenseType_deleteOcrPage_desc";
    /** Constant <code>LICENSE_TYPE_CMS="licenseType_cms"</code> */
    public static final String LICENSE_TYPE_CMS = "licenseType_cms";
    private static final String LICENSE_TYPE_DESC_CMS = "licenseType_cms_desc";
    public static final String LICENSE_TYPE_LEGAL_DISCLAIMER = "licenseType_disclaimer";
    private static final String LICENSE_TYPE_DESC_LEGAL_DISCLAIMER = "licenseType_disclaimer_desc";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "license_type_id")
    private Long id;

    // Field length had to be limited to 180 chars because InnoDB only supports 767 bytes per index,
    // and the unique index will require 255*n bytes (where n depends on the charset)
    @Column(name = "name", nullable = false, unique = true, columnDefinition = "VARCHAR(180)")
    private String name;
    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;
    @Column(name = "open_access")
    private boolean openAccess = false;
    @Column(name = "core")
    private boolean core = false;
    @Column(name = "moving_wall")
    private boolean movingWall = false;
    @Column(name = "pdf_download_quota")
    private boolean pdfDownloadQuota = true;
    @Column(name = "concurrent_views_limit")
    private boolean concurrentViewsLimit = true;
    @Column(name = "redirect")
    private boolean redirect = false;
    @Column(name = "redirect_url")
    private String redirectUrl;
    @Column(name = "access_ticket_required")
    private boolean accessTicketRequired = false;

    /** Privileges that everyone else has (users without this license, users that are not logged in). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "license_type_privileges", joinColumns = @JoinColumn(name = "license_type_id"))
    @Column(name = "privilege_name")
    private Set<String> privileges = new HashSet<>();

    /** Other license types for which this license type can substitute privileges. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "license_types_overriding", joinColumns = @JoinColumn(name = "license_type_id"),
            inverseJoinColumns = @JoinColumn(name = "overriding_license_type_id"))
    private Set<LicenseType> overriddenLicenseTypes = new HashSet<>();

    @Transient
    private Set<String> privilegesCopy = new HashSet<>();

    /**
     * Temporary markers for license types that are part of a moving wall configuration where the condition query no longer matches a particular
     * record.
     */
    @Transient
    private Map<String, Boolean> restrictionsExpired = new HashMap<>();

    @Transient
    private Boolean ugcType = null;

    @Transient
    private Long recordCount = null;

    @Transient
    private boolean displayRecordSearchLink = true;

    /**
     * Empty constructor.
     */
    public LicenseType() {
    }

    /**
     *
     * @param name License type name
     */
    public LicenseType(String name) {
        this.name = name;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
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
        LicenseType other = (LicenseType) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
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
     * Getter for the field <code>name</code>.
     * </p>
     *
     * @return the name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Setter for the field <code>name</code>.
     * </p>
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
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
     * <p>
     * getMatch.
     * </p>
     *
     * @param conditions a {@link java.lang.String} object.
     * @param pattern a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getMatch(String conditions, String pattern) {
        if (StringUtils.isBlank(conditions)) {
            return "";
        }
        Matcher matcher = Pattern.compile(pattern).matcher(conditions);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * <p>
     * isCmsType.
     * </p>
     *
     * @return true if this license type has one of the static CMS type names; false otherwise
     */
    public boolean isCmsType() {
        if (name == null) {
            return false;
        }

        return LICENSE_TYPE_CMS.equals(name);
    }

    /**
     * <p>
     * isCmsType.
     * </p>
     *
     * @return true if this license type has one of the static CMS type names; false otherwise
     */
    public boolean isLegalDisclaimerType() {
        if (name == null) {
            return false;
        }

        return LICENSE_TYPE_LEGAL_DISCLAIMER.equals(name);
    }

    /**
     * Checks whether only Solr documents of the UGC type have the access condition upon which this license type is based.
     *
     * @return the ugcType
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public boolean isUgcType() {
        if (ugcType == null) {
            try {
                ugcType = DataManager.getInstance()
                        .getSearchIndex()
                        .getHitCount(
                                "+"
                                        + SolrConstants.DOCTYPE
                                        + ":"
                                        + DocType.DOCSTRCT
                                        + " +"
                                        + SolrConstants.ACCESSCONDITION
                                        + ":\""
                                        + name
                                        + "\"") == 0
                        && DataManager.getInstance()
                                .getSearchIndex()
                                .getHitCount(
                                        "+"
                                                + SolrConstants.DOCTYPE
                                                + ":"
                                                + DocType.UGC
                                                + " +"
                                                + SolrConstants.ACCESSCONDITION
                                                + ":\""
                                                + name
                                                + "\"") > 0;
            } catch (IndexUnreachableException | PresentationException e) {
                ugcType = false;
            }
        }
        return ugcType;
    }

    /**
     * <p>
     * isOpenAccess.
     * </p>
     *
     * @return the openAccess
     */
    public boolean isOpenAccess() {
        return openAccess;
    }

    /**
     * <p>
     * Setter for the field <code>openAccess</code>.
     * </p>
     *
     * @param openAccess the openAccess to set
     */
    public void setOpenAccess(boolean openAccess) {
        this.openAccess = openAccess;
    }

    /**
     * <p>
     * isCore.
     * </p>
     *
     * @return the core
     */
    public boolean isCore() {
        return core;
    }

    /**
     * <p>
     * Setter for the field <code>core</code>.
     * </p>
     *
     * @param core the core to set
     */
    public void setCore(boolean core) {
        this.core = core;
    }

    /**
     * @return the movingWall
     */
    public boolean isMovingWall() {
        return movingWall;
    }

    /**
     * @param movingWall the movingWall to set
     */
    public void setMovingWall(boolean movingWall) {
        this.movingWall = movingWall;
    }

    /**
     * @return the pdfDownloadQuota
     */
    public boolean isPdfDownloadQuota() {
        return pdfDownloadQuota;
    }

    /**
     * @param pdfDownloadQuota the pdfDownloadQuota to set
     */
    public void setPdfDownloadQuota(boolean pdfDownloadQuota) {
        this.pdfDownloadQuota = pdfDownloadQuota;
    }

    /**
     * @return the concurrentViewsLimit
     */
    public boolean isConcurrentViewsLimit() {
        return concurrentViewsLimit;
    }

    /**
     * @param concurrentViewsLimit the concurrentViewsLimit to set
     */
    public void setConcurrentViewsLimit(boolean concurrentViewsLimit) {
        this.concurrentViewsLimit = concurrentViewsLimit;
    }

    /**
     * @return the redirect
     */
    public boolean isRedirect() {
        return redirect;
    }

    /**
     * @param redirect the redirect to set
     */
    public void setRedirect(boolean redirect) {
        this.redirect = redirect;
    }

    /**
     * @return the redirectUrl
     */
    public String getRedirectUrl() {
        return redirectUrl;
    }

    /**
     * @param redirectUrl the redirectUrl to set
     */
    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    /**
     * @return the accessTicketRequired
     */
    public boolean isAccessTicketRequired() {
        return accessTicketRequired;
    }

    /**
     * @param accessTicketRequired the accessTicketRequired to set
     */
    public void setAccessTicketRequired(boolean accessTicketRequired) {
        this.accessTicketRequired = accessTicketRequired;
        if (accessTicketRequired) {
            if (!hasPrivilegeCopy(IPrivilegeHolder.PRIV_LIST)) {
                addPrivilege(IPrivilegeHolder.PRIV_LIST);
            }
        } else {
            removePrivilege(IPrivilegeHolder.PRIV_LIST);
        }
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
     * Returns the list of available privileges for adding to this license (using the working copy while editing).
     *
     * @return Values in IPrivilegeHolder.PRIVS_RECORD minus the privileges already added
     */
    public List<String> getAvailablePrivileges() {
        return getAvailablePrivileges(privilegesCopy);
    }

    /**
     * Returns the list of available privileges for adding to this license (using the given privileges list).
     *
     * @param privileges Privileges to be removed from the returned list
     * @return Values in IPrivilegeHolder.PRIVS_RECORD minus the privileges already added
     * @should only return priv view ugc if ugc type
     * @should return record privileges if licenseType regular
     */
    public List<String> getAvailablePrivileges(Set<String> privileges) {
        List<String> ret;

        if (isUgcType()) {
            ret = new ArrayList<>(Arrays.asList(IPrivilegeHolder.PRIV_VIEW_UGC));
        } else {
            ret = new ArrayList<>(Arrays.asList(PRIVS_RECORD));
        }
        if (privileges != null) {
            ret.removeAll(privileges);
        }
        return ret;
    }

    /**
     * Returns a sorted list (according to PRIVS_RECORD) based on the given set of privileges.
     *
     * @return Sorted list of privileges contained in <code>privileges</code>
     */
    @Override
    public List<String> getSortedPrivileges(Set<String> privileges) {
        List<String> ret = new ArrayList<>(PRIVS_RECORD.length);
        for (String priv : Arrays.asList(PRIVS_RECORD)) {
            if (privileges.contains(priv)) {
                ret.add(priv);
                // logger.trace("has priv: {}", priv); //NOSONAR Debug
            }
        }

        return ret;
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

    /**
     * Checks whether this license type has the given privilege in the working copy of the privilege list.
     *
     * @param privilege Privilege name to check
     * @return true if copy contains privilege; false otherwise
     */
    public boolean hasPrivilegeCopy(String privilege) {
        return privilegesCopy.contains(privilege);
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
     * <p>
     * Getter for the field <code>overridingLicenseTypes</code>.
     * </p>
     *
     * @return the overridsenLicenseTypes
     */
    public Set<LicenseType> getOverriddenLicenseTypes() {
        return overriddenLicenseTypes;
    }

    /**
     * <p>
     * Setter for the field <code>overriddenLicenseTypes</code>.
     * </p>
     *
     * @param overriddenLicenseTypes the overriddenLicenseTypes to set
     */
    public void setOverriddenLicenseTypes(Set<LicenseType> overriddenLicenseTypes) {
        this.overriddenLicenseTypes = overriddenLicenseTypes;
    }

    /**
     * Returns list of {@link LicenseType}s that contain this object in their overridden LicenseTypes.
     * 
     * @return List<LicenseType>
     * @throws DAOException
     */
    public List<LicenseType> getLicenseTypesOverridingThis() throws DAOException {
        return DataManager.getInstance().getDao().getOverridingLicenseType(this);
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

    /**
     *
     * @param query
     * @return true if the configured restriction query is expired; false otherwise
     */
    public boolean isRestrictionsExpired(String query) {
        if (query == null) {
            return false;
        }
        return restrictionsExpired.get(query) != null && restrictionsExpired.get(query);
    }

    /**
     * @return the restrictionsExpired
     */
    public Map<String, Boolean> getRestrictionsExpired() {
        return restrictionsExpired;
    }

    /**
     * @param restrictionsExpired the restrictionsExpired to set
     */
    public void setRestrictionsExpired(Map<String, Boolean> restrictionsExpired) {
        this.restrictionsExpired = restrictionsExpired;
    }

    /**
     * For unit tests.
     * 
     * @param ugcType the ugcType to set
     */
    void setUgcType(Boolean ugcType) {
        this.ugcType = ugcType;
    }

    /**
     * @return the recordCount
     */
    public Long getRecordCount() {
        return recordCount;
    }

    /**
     * @param recordCount the recordCount to set
     */
    public void setRecordCount(Long recordCount) {
        this.recordCount = recordCount;
    }

    /**
     * @return the displayRecordSearchLink
     */
    public boolean isDisplayRecordSearchLink() {
        return displayRecordSearchLink;
    }

    /**
     * @param displayRecordSearchLink the displayRecordSearchLink to set
     */
    public void setDisplayRecordLink(boolean displayRecordSearchLink) {
        this.displayRecordSearchLink = displayRecordSearchLink;
    }

    /**
     * <p>
     * addCoreLicenseTypesToDB.
     * </p>
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static void addCoreLicenseTypesToDB() throws DAOException {
        // Add the license type "may set representative image", if not yet in the database
        addCoreLicenseType(LICENSE_TYPE_SET_REPRESENTATIVE_IMAGE, LICENSE_TYPE_SET_REPRESENTATIVE_IMAGE_DESCRIPTION,
                IPrivilegeHolder.PRIV_SET_REPRESENTATIVE_IMAGE);
        // Add the license type "may delete ocr page", if not yet in the database
        addCoreLicenseType(LICENSE_TYPE_DELETE_OCR_PAGE, LICENSE_TYPE_DELETE_OCR_PAGE_DESCRIPTION, IPrivilegeHolder.PRIV_DELETE_OCR_PAGE);
        // Add CMS license types, if not yet in the database
        addCoreLicenseType(LICENSE_TYPE_CMS, LICENSE_TYPE_DESC_CMS, IPrivilegeHolder.PRIV_CMS_PAGES);
        // Add legal disclaimer license type, if not yet in the database
        addCoreLicenseType(LICENSE_TYPE_LEGAL_DISCLAIMER, LICENSE_TYPE_DESC_LEGAL_DISCLAIMER, IPrivilegeHolder.PRIV_LEGAL_DISCLAIMER);
    }

    /**
     *
     * @param licenseTypeName
     * @param licenseTypeDesc
     * @param privNames
     * @throws DAOException
     */
    private static void addCoreLicenseType(String licenseTypeName, String licenseTypeDesc, String... privNames) throws DAOException {
        LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType(licenseTypeName);
        if (licenseType != null) {
            // Set core=true
            if (!licenseType.isCore()) {
                logger.info("Adding core=true to license type '{}'...", licenseTypeName);
                licenseType.setCore(true);
                if (!DataManager.getInstance().getDao().updateLicenseType(licenseType)) {
                    logger.error("Could not update static license type '{}'.", licenseTypeName);
                }
            }
            return;
        }
        logger.info("License type '{}' does not exist yet, adding...", licenseTypeName);
        licenseType = new LicenseType();
        licenseType.setName(licenseTypeName);
        licenseType.setDescription(licenseTypeDesc);
        licenseType.setCore(true);
        if (privNames != null && privNames.length > 0) {
            for (String privName : privNames) {
                licenseType.getPrivileges().add(privName);
            }
        }
        if (!DataManager.getInstance().getDao().addLicenseType(licenseType)) {
            logger.error("Could not add static license type '{}'.", licenseTypeName);
        }
    }

    /**
     *
     * @return Solr query for the access condition name
     */
    public String getFilterQueryPart() {
        return new StringBuilder().append(" ").append(SolrConstants.ACCESSCONDITION).append(":\"").append(name).append('"').toString();
    }

    /**
     * 
     * @return Solr query for the moving wall
     */
    public String getMovingWallFilterQueryPart() {
        // Do not append empty sub-query
        return new StringBuilder().append(" (+")
                .append(SolrConstants.ACCESSCONDITION)
                .append(":\"")
                .append(name)
                .append("\" +")
                .append(SearchHelper.getMovingWallQuery())
                /**
                 * The following line is necessary if negateFilterQuery is true. In this case you get a query '(-CONDITION)' which never yields any
                 * results because queries MUST contain a positive expression in order to do so. So the '*:*' acts as a all-encompassing positive
                 * expression which has no logical effect (it's equivalent to 'or true') Source:
                 * https://localcoder.org/weird-solr-lucene-behaviors-with-boolean-operators
                 */
                // .append(" *:*))")
                .append(')')
                .toString();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("LicenceType: ").append(getName()).append(":\t");
        sb.append("moving wall: ").append(isMovingWall());
        sb.append("\topenaccess: ").append(isOpenAccess());
        sb.append("\n\t").append("Privileges: ").append(StringUtils.join(getPrivileges(), ", "));
        return sb.toString();
    }
}
