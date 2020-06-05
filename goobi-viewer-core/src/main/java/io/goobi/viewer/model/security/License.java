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
package io.goobi.viewer.model.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSPageTemplate;
import io.goobi.viewer.model.cms.Selectable;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;

/**
 * <p>
 * License class.
 * </p>
 */
@Entity
@Table(name = "licenses")
public class License implements IPrivilegeHolder, Serializable {

    private static final long serialVersionUID = 1363557138283960150L;

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(License.class);

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

    @JoinColumn(name = "license_type_id", nullable = false)
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

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_start")
    private Date start;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_end")
    private Date end;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "license_privileges", joinColumns = @JoinColumn(name = "license_id"))
    @Column(name = "privilege_name")
    private Set<String> privileges = new HashSet<>();

    @Column(name = "conditions")
    private String conditions;

    @Column(name = "description")
    private String description;

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
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "license_cms_templates", joinColumns = @JoinColumn(name = "license_id"))
    @Column(name = "template_id")
    private List<String> allowedCmsTemplates = new ArrayList<>();

    /** List of allowed crowdsourcing campaigns. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "license_crowdsourcing_campaigns", joinColumns = @JoinColumn(name = "license_id"),
            inverseJoinColumns = @JoinColumn(name = "campaign_id"))
    private List<Campaign> allowedCrowdsourcingCampaigns = new ArrayList<>();

    @Transient
    private String type;

    @Transient
    private Set<String> privilegesCopy = new HashSet<>();

    @Transient
    private List<Selectable<String>> selectableSubthemes = null;

    @Transient
    private List<Selectable<CMSCategory>> selectableCategories = null;

    @Transient
    private List<Selectable<String>> selectableTemplates = null;

    /**
     * Checks the validity of this license. A valid license is either not time limited (start and/or end) or the current date lies between the
     * license's start and and dates.
     *
     * @return true if valid; false otherwise;
     * @should return correct value
     */
    public boolean isValid() {
        Date now = new Date();
        return (start == null || start.before(now)) && (end == null || end.after(now));
    }

    /**
     * Adds the given privilege to the working set.
     * 
     * @param privilege
     * @return true if successful; false otherwise
     */
    public boolean addPrivilege(String privilege) {
        return privilegesCopy.add(privilege);
    }

    /**
     * Removes the given privilege from the working set.
     * 
     * @param privilege
     * @return true if successful; false otherwise
     */
    public boolean removePrivilege(String privilege) {
        return privilegesCopy.remove(privilege);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasPrivilege(String privilege) {
        return privileges.contains(privilege);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPrivList() {
        return hasPrivilege(IPrivilegeHolder.PRIV_LIST);
    }

    /** {@inheritDoc} */
    @Override
    public void setPrivList(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_LIST);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_LIST);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPrivViewImages() {
        return hasPrivilege(IPrivilegeHolder.PRIV_VIEW_IMAGES);
    }

    /** {@inheritDoc} */
    @Override
    public void setPrivViewImages(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_VIEW_IMAGES);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_VIEW_IMAGES);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPrivViewThumbnails() {
        return hasPrivilege(IPrivilegeHolder.PRIV_VIEW_THUMBNAILS);
    }

    /** {@inheritDoc} */
    @Override
    public void setPrivViewThumbnails(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_VIEW_THUMBNAILS);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_VIEW_THUMBNAILS);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPrivViewFulltext() {
        return hasPrivilege(IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
    }

    /** {@inheritDoc} */
    @Override
    public void setPrivViewFulltext(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPrivViewVideo() {
        return hasPrivilege(IPrivilegeHolder.PRIV_VIEW_VIDEO);
    }

    /** {@inheritDoc} */
    @Override
    public void setPrivViewVideo(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_VIEW_VIDEO);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_VIEW_VIDEO);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPrivViewAudio() {
        return hasPrivilege(IPrivilegeHolder.PRIV_VIEW_AUDIO);
    }

    /** {@inheritDoc} */
    @Override
    public void setPrivViewAudio(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_VIEW_AUDIO);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_VIEW_AUDIO);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPrivDownloadPdf() {
        return hasPrivilege(IPrivilegeHolder.PRIV_DOWNLOAD_PDF);
    }

    /** {@inheritDoc} */
    @Override
    public void setPrivDownloadPdf(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_DOWNLOAD_PDF);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_DOWNLOAD_PDF);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivDownloadPagePdf()
     */
    /** {@inheritDoc} */
    @Override
    public boolean isPrivDownloadPagePdf() {
        return hasPrivilege(IPrivilegeHolder.PRIV_DOWNLOAD_PAGE_PDF);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivDownloadPagePdf(boolean)
     */
    /** {@inheritDoc} */
    @Override
    public void setPrivDownloadPagePdf(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_DOWNLOAD_PAGE_PDF);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_DOWNLOAD_PAGE_PDF);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.user.IPrivilegeHolder#isPrivDownloadOriginalContent()
     */
    /** {@inheritDoc} */
    @Override
    public boolean isPrivDownloadOriginalContent() {
        return hasPrivilege(IPrivilegeHolder.PRIV_DOWNLOAD_ORIGINAL_CONTENT);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.user.IPrivilegeHolder#setPrivDownloadOriginalContent(boolean)
     */
    /** {@inheritDoc} */
    @Override
    public void setPrivDownloadOriginalContent(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_DOWNLOAD_ORIGINAL_CONTENT);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_DOWNLOAD_ORIGINAL_CONTENT);
        }

    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivDownloadMetadata()
     */
    @Override
    public boolean isPrivDownloadMetadata() {
        return hasPrivilege(IPrivilegeHolder.PRIV_DOWNLOAD_METADATA);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivDownloadMetadata(boolean)
     */
    @Override
    public void setPrivDownloadMetadata(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_DOWNLOAD_METADATA);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_DOWNLOAD_METADATA);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivGenerateIiifManifest()
     */
    @Override
    public boolean isPrivGenerateIiifManifest() {
        return hasPrivilege(IPrivilegeHolder.PRIV_GENERATE_IIIF_MANIFEST);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivIiifManifest(boolean)
     */
    @Override
    public void setPrivGenerateIiifManifest(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_GENERATE_IIIF_MANIFEST);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_GENERATE_IIIF_MANIFEST);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#isPrivDeleteOcrPage()
     */
    /** {@inheritDoc} */
    @Override
    public boolean isPrivDeleteOcrPage() {
        return hasPrivilege(IPrivilegeHolder.PRIV_DELETE_OCR_PAGE);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.security.IPrivilegeHolder#setPrivDeleteOcrPage(boolean)
     */
    /** {@inheritDoc} */
    @Override
    public void setPrivDeleteOcrPage(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_DELETE_OCR_PAGE);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_DELETE_OCR_PAGE);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPrivSetRepresentativeImage() {
        return hasPrivilege(IPrivilegeHolder.PRIV_SET_REPRESENTATIVE_IMAGE);
    }

    /** {@inheritDoc} */
    @Override
    public void setPrivSetRepresentativeImage(boolean priv) {
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_SET_REPRESENTATIVE_IMAGE);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_SET_REPRESENTATIVE_IMAGE);
        }
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
            privileges.add(IPrivilegeHolder.PRIV_CMS_PAGES);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_PAGES);
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
            privileges.add(IPrivilegeHolder.PRIV_CMS_ALL_SUBTHEMES);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_ALL_SUBTHEMES);
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
            privileges.add(IPrivilegeHolder.PRIV_CMS_ALL_CATEGORIES);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_ALL_CATEGORIES);
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
            privileges.add(IPrivilegeHolder.PRIV_CMS_ALL_TEMPLATES);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_ALL_TEMPLATES);
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
        if (priv) {
            privileges.add(IPrivilegeHolder.PRIV_CMS_MENU);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_MENU);
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
            privileges.add(IPrivilegeHolder.PRIV_CMS_STATIC_PAGES);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_STATIC_PAGES);
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
            privileges.add(IPrivilegeHolder.PRIV_CMS_COLLECTIONS);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_COLLECTIONS);
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
            privileges.add(IPrivilegeHolder.PRIV_CMS_CATEGORIES);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CMS_CATEGORIES);
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
            privileges.add(IPrivilegeHolder.PRIV_CROWDSOURCING_ALL_CAMPAIGNS);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CROWDSOURCING_ALL_CAMPAIGNS);
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
            privileges.add(IPrivilegeHolder.PRIV_CROWDSOURCING_ANNOTATE_CAMPAIGN);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CROWDSOURCING_ANNOTATE_CAMPAIGN);
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
            privileges.add(IPrivilegeHolder.PRIV_CROWDSOURCING_REVIEW_CAMPAIGN);
        } else {
            privileges.remove(IPrivilegeHolder.PRIV_CROWDSOURCING_REVIEW_CAMPAIGN);
        }
    }

    /**
     * Returns the list of available privileges for adding to this license (using the working copy while editing).
     * 
     * @return Values in IPrivilegeHolder.PRIVS_RECORD minus the privileges already added
     */
    public Set<String> getAvailablePrivileges() {
        return getAvailablePrivileges(privilegesCopy);
    }

    /**
     * Returns the list of available privileges for adding to this license (using the given privileges list).
     * 
     * @return Values in IPrivilegeHolder.PRIVS_RECORD minus the privileges already added
     */
    public Set<String> getAvailablePrivileges(Set<String> privileges) {
        Set<String> ret = new HashSet<>(Arrays.asList(IPrivilegeHolder.PRIVS_RECORD));
        privileges.toString();
        // Remove existing privileges
        ret.removeAll(privileges);
        // Remove privileges inherited from the license type
        if (licenseType != null) {
            ret.removeAll(licenseType.getPrivileges());
        }
        return ret;
    }

    public List<String> getAvailableSubthemes() {
        // TODO
        return Collections.emptyList();
    }

    /**
     * 
     * @return
     * @throws DAOException
     */
    public List<Selectable<String>> getSelectableSubthemes() throws DAOException {
        if (selectableSubthemes == null) {
            List<String> allSubthemes = new ArrayList<>(); // TODO
            selectableSubthemes =
                    allSubthemes.stream()
                            .map(sub -> new Selectable<>(sub, this.subthemeDiscriminatorValues.contains(sub)))
                            .collect(Collectors.toList());
        }
        return selectableSubthemes;
    }

    /**
     * 
     * @return
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
     * @return
     * @throws DAOException
     */
    public List<Selectable<String>> getSelectableTemplates() throws DAOException {
        if (selectableTemplates == null) {
            List<CMSPageTemplate> allTemplates = BeanUtils.getCmsBean().getTemplates();
            selectableTemplates =
                    allTemplates.stream()
                            .map(template -> new Selectable<>(template.getName(), this.allowedCmsTemplates.contains(template.getName())))
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
    public Date getStart() {
        return start;
    }

    /**
     * <p>
     * Setter for the field <code>start</code>.
     * </p>
     *
     * @param start the start to set
     */
    public void setStart(Date start) {
        this.start = start;
    }

    /**
     * <p>
     * Getter for the field <code>end</code>.
     * </p>
     *
     * @return the end
     */
    public Date getEnd() {
        return end;
    }

    /**
     * <p>
     * Setter for the field <code>end</code>.
     * </p>
     *
     * @param end the end to set
     */
    public void setEnd(Date end) {
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
    public List<String> getAllowedCmsTemplates() {
        return allowedCmsTemplates;
    }

    /**
     * <p>
     * Setter for the field <code>allowedCmsTemplates</code>.
     * </p>
     *
     * @param allowedCmsTemplates the allowedCmsTemplates to set
     */
    public void setAllowedCmsTemplates(List<String> allowedCmsTemplates) {
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
}
