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
package io.goobi.viewer.model.cms;

import java.io.Serializable;
import java.util.Locale;
import java.util.Optional;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.viewer.PageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * <p>
 * CMSStaticPage class.
 * </p>
 */
@Entity
@Table(name = "cms_static_pages")
public class CMSStaticPage implements Serializable {

    private static final long serialVersionUID = -8591081547005923490L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "static_page_id")
    private Long id;

    @Column(name = "static_page_name", nullable = false)
    private String pageName;

    @Column(name = "cms_page_Id")
    private Long cmsPageId;

    @Transient
    private Optional<CMSPage> cmsPage = Optional.empty();

    /**
     * <p>
     * Constructor for CMSStaticPage.
     * </p>
     */
    public CMSStaticPage() {
        this.id = null;
        this.pageName = "";
    }

    /**
     * <p>
     * Constructor for CMSStaticPage.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @throws java.lang.NullPointerException if the given name is null
     */
    public CMSStaticPage(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        this.id = null;
        this.pageName = name;
    }

    /**
     * <p>
     * getCmsPageOptional.
     * </p>
     *
     * @return the cmsPage
     */
    public Optional<CMSPage> getCmsPageOptional() {
        if (!cmsPage.isPresent()) {
            updateCmsPage();
        }
        return cmsPage;
    }

    /**
     * <p>
     * Getter for the field <code>cmsPage</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.cms.pages.CMSPage} object.
     */
    public CMSPage getCmsPage() {
        return getCmsPageOptional().orElse(null);
    }

    /**
     * <p>
     * Setter for the field <code>cmsPage</code>.
     * </p>
     *
     * @param cmsPage the cmsPage to set
     */
    public void setCmsPage(CMSPage cmsPage) {
        this.cmsPage = Optional.ofNullable(cmsPage);
        setCmsPageId(this.cmsPage.map(CMSPage::getId).orElse(null));
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
     * Getter for the field <code>pageName</code>.
     * </p>
     *
     * @return the pageName
     */
    public String getPageName() {
        return pageName;
    }

    /**
     * <p>
     * isLanguageComplete.
     * </p>
     *
     * @param locale a {@link java.util.Locale} object.
     * @return a boolean.
     */
    public boolean isLanguageComplete(Locale locale) {
        if (getCmsPageOptional().isPresent()) {
            return cmsPage.get().isComplete(locale);
        }
        return false;
    }

    /**
     * <p>
     * isHasCmsPage.
     * </p>
     *
     * @return true only if isUseCmsPage == true and cmsPage != null
     */
    public boolean isHasCmsPage() {
        return getCmsPageId().isPresent();
    }

    /**
     * <p>
     * Getter for the field <code>cmsPageId</code>.
     * </p>
     *
     * @return the cmsPageId
     */
    public Optional<Long> getCmsPageId() {
        return Optional.ofNullable(cmsPageId);
    }

    /**
     * <p>
     * Setter for the field <code>cmsPageId</code>.
     * </p>
     *
     * @param cmsPageId the cmsPageId to set
     */
    public void setCmsPageId(Long cmsPageId) {
        this.cmsPageId = cmsPageId;
        Optional<CMSPage> localCmsPage = getCmsPageOptional();
        if (!localCmsPage.isPresent() || localCmsPage.map(CMSPage::getId).map(id1 -> !id1.equals(cmsPageId)).orElse(true)) {
            updateCmsPage();
        }
    }

    private void updateCmsPage() {
        getCmsPageId().ifPresent(id1 -> {
            try {
                this.cmsPage = Optional.ofNullable(DataManager.getInstance().getDao().getCMSPage(id1));
            } catch (DAOException e) {
                this.cmsPage = Optional.empty();
            }
        });
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return getPageName().hashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(this.getClass())) {
            return ((CMSStaticPage) obj).getPageName().equals(this.getPageName());
        }

        return false;
    }

    public PageType getPageType() {
        return PageType.getByName(this.pageName);
    }

}
