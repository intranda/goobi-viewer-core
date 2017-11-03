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

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

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
    public static final String LICENSE_TYPE_FORCE_OVERVIEW_PAGE = "licenseType_forceOverviewPage";
    public static final String LICENSE_TYPE_DELETE_OCR_PAGE = "licenseType_deleteOcrPage";
    private static final String LICENSE_TYPE_SET_REPRESENTATIVE_IMAGE_DESCRIPTION = "licenseType_setRepresentativeImage_desc";
    private static final String LICENSE_TYPE_FORCE_OVERVIEW_PAGE_DESCRIPTION = "licenseType_forceOverviewPage_desc";
    private static final String LICENSE_TYPE_DELETE_OCR_PAGE_DESCRIPTION = "licenseType_deleteOcrPage_desc";

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
     * @return
     */
    public boolean isStaticLicenseType() {
        return LICENSE_TYPE_SET_REPRESENTATIVE_IMAGE.equals(name) || LICENSE_TYPE_FORCE_OVERVIEW_PAGE.equals(name) || LICENSE_TYPE_DELETE_OCR_PAGE
                .equals(name);
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
        if (conditions.contains("NOW/YEAR") && !conditions.contains("DATE_")) {
            // Hack for getting the current year as a number for non-date Solr fields
            conditions = conditions.replace("NOW/YEAR", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
        }

        return conditions;
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

    public static void addStaticLicenseTypesToDB() throws DAOException {
        // Add the license type "may set representative image", if not yet in the database
        if (DataManager.getInstance().getDao().getLicenseType(LicenseType.LICENSE_TYPE_SET_REPRESENTATIVE_IMAGE) == null) {
            logger.info("License type '{}' does not exist yet, adding...", LicenseType.LICENSE_TYPE_SET_REPRESENTATIVE_IMAGE);
            LicenseType licenseType = new LicenseType();
            licenseType.setName(LicenseType.LICENSE_TYPE_SET_REPRESENTATIVE_IMAGE);
            licenseType.setDescription(LICENSE_TYPE_SET_REPRESENTATIVE_IMAGE);
            licenseType.getPrivileges().add(IPrivilegeHolder.PRIV_SET_REPRESENTATIVE_IMAGE);
            if (!DataManager.getInstance().getDao().addLicenseType(licenseType)) {
                logger.error("Could not add static license type '{}'.", LicenseType.LICENSE_TYPE_SET_REPRESENTATIVE_IMAGE);
            }
        }
        // Add the license type "may force overview page", if not yet in the database
        if (DataManager.getInstance().getDao().getLicenseType(LicenseType.LICENSE_TYPE_FORCE_OVERVIEW_PAGE) == null) {
            logger.info("License type '{}' does not exist yet, adding...", LicenseType.LICENSE_TYPE_FORCE_OVERVIEW_PAGE);
            LicenseType licenseType = new LicenseType();
            licenseType.setName(LicenseType.LICENSE_TYPE_FORCE_OVERVIEW_PAGE);
            licenseType.setDescription(LICENSE_TYPE_FORCE_OVERVIEW_PAGE_DESCRIPTION);
            licenseType.getPrivileges().add(IPrivilegeHolder.PRIV_EDIT_OVERVIEW_PAGE);
            if (!DataManager.getInstance().getDao().addLicenseType(licenseType)) {
                logger.error("Could not add static license type '{}'.", LicenseType.LICENSE_TYPE_FORCE_OVERVIEW_PAGE);
            }
        }
        // Add the license type "may delete ocr page", if not yet in the database
        if (DataManager.getInstance().getDao().getLicenseType(LicenseType.LICENSE_TYPE_DELETE_OCR_PAGE) == null) {
            logger.info("License type '{}' does not exist yet, adding...", LicenseType.LICENSE_TYPE_DELETE_OCR_PAGE);
            LicenseType licenseType = new LicenseType();
            licenseType.setName(LicenseType.LICENSE_TYPE_DELETE_OCR_PAGE);
            licenseType.setDescription(LICENSE_TYPE_DELETE_OCR_PAGE_DESCRIPTION);
            licenseType.getPrivileges().add(IPrivilegeHolder.PRIV_DELETE_OCR_PAGE);
            if (!DataManager.getInstance().getDao().addLicenseType(licenseType)) {
                logger.error("Could not add static license type '{}'.", IPrivilegeHolder.PRIV_DELETE_OCR_PAGE);
            }
        }
    }
}
