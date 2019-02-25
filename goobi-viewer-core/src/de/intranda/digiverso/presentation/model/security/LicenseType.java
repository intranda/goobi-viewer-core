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
package de.intranda.digiverso.presentation.model.security;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;

@Entity
@Table(name = "license_types")
public class LicenseType implements IPrivilegeHolder {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(LicenseType.class);

    // When adding a new static license type name, update isStaticLicenseType()!
    public static final String LICENSE_TYPE_SET_REPRESENTATIVE_IMAGE = "licenseType_setRepresentativeImage";
    public static final String LICENSE_TYPE_DELETE_OCR_PAGE = "licenseType_deleteOcrPage";
    private static final String LICENSE_TYPE_SET_REPRESENTATIVE_IMAGE_DESCRIPTION = "licenseType_setRepresentativeImage_desc";
    private static final String LICENSE_TYPE_DELETE_OCR_PAGE_DESCRIPTION = "licenseType_deleteOcrPage_desc";

    private static final String LICENSE_TYPE_CMS = "licenseType_cms";
    private static final String LICENSE_TYPE_DESC_CMS = "licenseType_cms_desc";
    //    private static final String LICENSE_TYPE_CMS_MENU = "licenseType_cms_navigation";
    //    private static final String LICENSE_TYPE_DESC_CMS_MENU = "licenseType_cms_menu_desc";
    //    private static final String LICENSE_TYPE_CMS_STATIC_PAGES = "licenseType_cms_static_pages";
    //    private static final String LICENSE_TYPE_DESC_CMS_STATIC_PAGES = "licenseType_cms_static_pages_desc";
    //    private static final String LICENSE_TYPE_CMS_COLLECTIONS = "licenseType_cms_collections";
    //    private static final String LICENSE_TYPE_DESC_CMS_COLLECTIONS = "licenseType_cms_collections_desc";

    //    private static final String CONDITIONS_QUERY = "QUERY:\\{(.*?)\\}";
    private static final String CONDITIONS_FILENAME = "FILENAME:\\{(.*)\\}";

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
    @Column(name = "conditions")
    private String conditions;
    @Column(name = "open_access")
    private boolean openAccess = false;

    /** Privileges that everyone else has (users without this license, users that are not logged in). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "license_type_privileges", joinColumns = @JoinColumn(name = "license_type_id"))
    @Column(name = "privilege_name")
    private Set<String> privileges = new HashSet<>();

    /** Other license types for which a user may have privileges that this license type does not grant. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "license_types_overriding", joinColumns = @JoinColumn(name = "license_type_id"),
            inverseJoinColumns = @JoinColumn(name = "overriding_license_type_id"))
    private Set<LicenseType> overridingLicenseTypes = new HashSet<>();

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
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
     * Checks conditions under which the edit button for this object shall be disabled.
     *
     * @return
     */
    public boolean isEditLocked() {
        return isStaticLicenseType();
    }

    /**
     * Checks conditions under which the delete button for this object shall be disabled.
     *
     * @return
     */
    public boolean isDeleteLocked() {
        return isStaticLicenseType();
    }

    /**
     * Checks whether this is a static license type by the name.
     *
     * @return true if license type name is one of the static name strings; false otherwise
     */
    public boolean isStaticLicenseType() {
        if (name == null) {
            return false;
        }

        switch (name) {
            case LICENSE_TYPE_CMS:
            case LICENSE_TYPE_DELETE_OCR_PAGE:
            case LICENSE_TYPE_SET_REPRESENTATIVE_IMAGE:
                return true;
            default:
                return false;
        }
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
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 
     * @return
     * @should replace NOW/YEAR with the current year if not using a date field
     */
    public String getProcessedConditions() {
        String conditions = this.conditions;

        conditions = getQueryConditions(conditions);

        if (conditions.contains("NOW/YEAR") && !conditions.contains("DATE_")) {
            // Hack for getting the current year as a number for non-date Solr fields
            conditions = conditions.replace("NOW/YEAR", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
        }

        return conditions.trim();
    }

    public String getFilenameConditions() {
        return getFilenameConditions(this.conditions);
    }

    /**
     * Get the conditions referring to a SOLR query. This is either the substring in {} after QUERY: or the entire string if neither QUERY:{...} nor
     * FILENAME:{...} is part of the given string
     * 
     * @param conditions
     * @return
     */
    private String getQueryConditions(String conditions) {
        String filenameConditions = getMatch(conditions, CONDITIONS_FILENAME);
        String queryConditions = conditions == null ? "" : conditions.replaceAll(CONDITIONS_FILENAME, "");
        if (StringUtils.isBlank(queryConditions) && StringUtils.isBlank(filenameConditions)) {
            return conditions == null ? "" : conditions;
        }
        return queryConditions;
    }

    /**
     * @param conditions
     * @return
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
     * Get the conditions referring to filename matching. This is either the substring in {} after FILENAME: or null if neither QUERY:{...} nor
     * FILENAME:{...} is part of the given string
     * 
     * @param conditions
     * @return
     */
    private String getFilenameConditions(String conditions) {
        String filenameConditions = getMatch(conditions, CONDITIONS_FILENAME);
        return filenameConditions;
    }

    /**
     * 
     * @return true if this license type has one of the static CMS type names; false otherwise
     */
    public boolean isCmsType() {
        if (name == null) {
            return false;
        }

        switch (name) {
            case LICENSE_TYPE_CMS:
                //            case LICENSE_TYPE_CMS_MENU:
                //            case LICENSE_TYPE_CMS_STATIC_PAGES:
                //            case LICENSE_TYPE_CMS_COLLECTIONS:
                return true;
            default:
                return false;
        }
    }

    /**
     * @return the conditions
     */
    public String getConditions() {
        return conditions;
    }

    /**
     * @param conditions the conditions to set
     */
    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    /**
     * @return the openAccess
     */
    public boolean isOpenAccess() {
        return openAccess;
    }

    /**
     * @param openAccess the openAccess to set
     */
    public void setOpenAccess(boolean openAccess) {
        this.openAccess = openAccess;
    }

    /**
     * @return the privileges
     */
    public Set<String> getPrivileges() {
        return privileges;
    }

    /**
     * @param privileges the privileges to set
     */
    public void setPrivileges(Set<String> privileges) {
        this.privileges = privileges;
    }

    @Override
    public boolean hasPrivilege(String privilege) {
        return privileges.contains(privilege);
    }

    @Override
    public boolean isPrivList() {
        return hasPrivilege(IPrivilegeHolder.PRIV_LIST);
    }

    @Override
    public void setPrivList(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_LIST);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_LIST);
        }
    }

    @Override
    public boolean isPrivViewImages() {
        return hasPrivilege(IPrivilegeHolder.PRIV_VIEW_IMAGES);
    }

    @Override
    public void setPrivViewImages(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_VIEW_IMAGES);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_VIEW_IMAGES);
        }
    }

    @Override
    public boolean isPrivViewThumbnails() {
        return hasPrivilege(IPrivilegeHolder.PRIV_VIEW_THUMBNAILS);
    }

    @Override
    public void setPrivViewThumbnails(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_VIEW_THUMBNAILS);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_VIEW_THUMBNAILS);
        }
    }

    @Override
    public boolean isPrivViewFulltext() {
        return hasPrivilege(IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
    }

    @Override
    public void setPrivViewFulltext(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
        }
    }

    @Override
    public boolean isPrivViewVideo() {
        return hasPrivilege(IPrivilegeHolder.PRIV_VIEW_VIDEO);
    }

    @Override
    public void setPrivViewVideo(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_VIEW_VIDEO);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_VIEW_VIDEO);
        }
    }

    @Override
    public boolean isPrivViewAudio() {
        return hasPrivilege(IPrivilegeHolder.PRIV_VIEW_AUDIO);
    }

    @Override
    public void setPrivViewAudio(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_VIEW_AUDIO);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_VIEW_AUDIO);
        }
    }

    @Override
    public boolean isPrivDownloadPdf() {
        return hasPrivilege(IPrivilegeHolder.PRIV_DOWNLOAD_PDF);
    }

    @Override
    public void setPrivDownloadPdf(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_DOWNLOAD_PDF);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_DOWNLOAD_PDF);
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.user.IPrivilegeHolder#isPrivDownloadOriginalContent()
     */
    @Override
    public boolean isPrivDownloadOriginalContent() {
        return hasPrivilege(IPrivilegeHolder.PRIV_DOWNLOAD_ORIGINAL_CONTENT);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.user.IPrivilegeHolder#setPrivDownloadOriginalContent(boolean)
     */
    @Override
    public void setPrivDownloadOriginalContent(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_DOWNLOAD_ORIGINAL_CONTENT);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_DOWNLOAD_ORIGINAL_CONTENT);
        }

    }

    @Override
    public boolean isPrivSetRepresentativeImage() {
        return hasPrivilege(IPrivilegeHolder.PRIV_SET_REPRESENTATIVE_IMAGE);
    }

    @Override
    public void setPrivSetRepresentativeImage(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_SET_REPRESENTATIVE_IMAGE);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_SET_REPRESENTATIVE_IMAGE);
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.IPrivilegeHolder#isPrivCmsPages()
     */
    @Override
    public boolean isPrivCmsPages() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_PAGES);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.IPrivilegeHolder#setPrivCmsPages(boolean)
     */
    @Override
    public void setPrivCmsPages(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_CMS_PAGES);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_PAGES);
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.IPrivilegeHolder#isPrivCmsMenu()
     */
    @Override
    public boolean isPrivCmsMenu() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_MENU);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.IPrivilegeHolder#setPrivCmsMenu(boolean)
     */
    @Override
    public void setPrivCmsMenu(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_CMS_MENU);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_MENU);
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.IPrivilegeHolder#isPrivCmsStaticPages()
     */
    @Override
    public boolean isPrivCmsStaticPages() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_STATIC_PAGES);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.IPrivilegeHolder#setPrivCmsStaticPages(boolean)
     */
    @Override
    public void setPrivCmsStaticPages(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_CMS_STATIC_PAGES);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_STATIC_PAGES);
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.IPrivilegeHolder#isPrivCmsCollections()
     */
    @Override
    public boolean isPrivCmsCollections() {
        return hasPrivilege(IPrivilegeHolder.PRIV_CMS_COLLECTIONS);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.security.IPrivilegeHolder#setPrivCmsCollections(boolean)
     */
    @Override
    public void setPrivCmsCollections(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_CMS_COLLECTIONS);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_COLLECTIONS);
        }
    }

    /**
     * @return the overridingLicenseTypes
     */
    public Set<LicenseType> getOverridingLicenseTypes() {
        return overridingLicenseTypes;
    }

    /**
     * @param overridingLicenseTypes the overridingLicenseTypes to set
     */
    public void setOverridingLicenseTypes(Set<LicenseType> overridingLicenseTypes) {
        this.overridingLicenseTypes = overridingLicenseTypes;
    }

    public static void addStaticLicenseTypesToDB() throws DAOException {
        // Add the license type "may set representative image", if not yet in the database
        addStaticLicenseType(LICENSE_TYPE_SET_REPRESENTATIVE_IMAGE, LICENSE_TYPE_SET_REPRESENTATIVE_IMAGE_DESCRIPTION,
                IPrivilegeHolder.PRIV_SET_REPRESENTATIVE_IMAGE);
        // Add the license type "may delete ocr page", if not yet in the database
        addStaticLicenseType(LICENSE_TYPE_DELETE_OCR_PAGE, LICENSE_TYPE_DELETE_OCR_PAGE_DESCRIPTION, IPrivilegeHolder.PRIV_DELETE_OCR_PAGE);
        // Add CMS license types, if not yet in the database
        addStaticLicenseType(LICENSE_TYPE_CMS, LICENSE_TYPE_DESC_CMS, IPrivilegeHolder.PRIV_CMS_PAGES);
        //        addStaticLicenseType(LICENSE_TYPE_CMS_MENU, LICENSE_TYPE_DESC_CMS_MENU, IPrivilegeHolder.PRIV_CMS_MENU);
        //        addStaticLicenseType(LICENSE_TYPE_CMS_STATIC_PAGES, LICENSE_TYPE_DESC_CMS_STATIC_PAGES, IPrivilegeHolder.PRIV_CMS_STATIC_PAGES);
        //        addStaticLicenseType(LICENSE_TYPE_CMS_COLLECTIONS, LICENSE_TYPE_DESC_CMS_COLLECTIONS, IPrivilegeHolder.PRIV_CMS_COLLECTIONS);
    }

    /**
     * 
     * @param licenseTypeName
     * @param licenseTypeDesc
     * @param privName
     * @throws DAOException
     */
    private static void addStaticLicenseType(String licenseTypeName, String licenseTypeDesc, String privName) throws DAOException {
        if (DataManager.getInstance().getDao().getLicenseType(licenseTypeName) == null) {
            logger.info("License type '{}' does not exist yet, adding...", licenseTypeName);
            LicenseType licenseType = new LicenseType();
            licenseType.setName(licenseTypeName);
            licenseType.setDescription(licenseTypeDesc);
            licenseType.getPrivileges().add(privName);
            if (!DataManager.getInstance().getDao().addLicenseType(licenseType)) {
                logger.error("Could not add static license type '{}'.", licenseTypeName);
            }
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getName()).append(":\t");
        sb.append("openaccess: ").append(isOpenAccess());
        sb.append("\tconditions: ").append(conditions);
        sb.append("\n\t").append("Privileges: ").append(StringUtils.join(getPrivileges(), ", "));
        return sb.toString();
    }
}
