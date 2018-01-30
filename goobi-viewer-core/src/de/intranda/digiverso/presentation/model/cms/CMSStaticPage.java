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
package de.intranda.digiverso.presentation.model.cms;

import java.util.Locale;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;

@Entity
@Table(name = "cms_static_pages")
public class CMSStaticPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "static_page_id")
    private Long id;
    
    @Column(name = "static_page_name", nullable=false)
    private String pageName;
    
    @Column(name = "cms_page_Id")
    private Long cmsPageId;
    

    @Transient
    private Optional<CMSPage> cmsPage = Optional.empty();
    
    public CMSStaticPage() {
        this.id = null;
        this.pageName = "";
    }
    
    /**
     * 
     * @param name
     * 
     * @throws NullPointerException if the given name is null
     */
    public CMSStaticPage(String name) {
        if(name == null) {
            throw new NullPointerException();
        }
        this.id = null;
        this.pageName = name;
    }
    
    /**
     * Construct a CMSStaticPage from a CMSPage referring to a static page.
     * Used for Backwards compability
     * 
     * @param cmsPage
     * @throws IllegalArgumentException if the cmsPage does not refer to a static page
     * @throws NullPointerException if the cmsPage is null
     */
    @SuppressWarnings("deprecation")
    public CMSStaticPage(CMSPage cmsPage) {
        String staticPageName = cmsPage.getStaticPageName();
        if(StringUtils.isBlank(staticPageName)) {
            throw new IllegalArgumentException("Can only create a static page from a CMSPage with a non-empty staticPageName");
        } else {
            this.id = null;
            this.pageName = staticPageName.trim();
            setCmsPage(cmsPage);
        }
    }


    /**
     * @return the cmsPage
     */
    public Optional<CMSPage> getCmsPageOptional() {
        if(!cmsPage.isPresent()) {
            updateCmsPage();
        }
        return cmsPage;
    }
    
    public CMSPage getCmsPage() {
        return getCmsPageOptional().orElse(null);
    }

    /**
     * @param cmsPage the cmsPage to set
     */
    public void setCmsPage(CMSPage cmsPage) {
        this.cmsPage = Optional.ofNullable(cmsPage);
        setCmsPageId(this.cmsPage.map(page -> page.getId()).orElse(null));
    }
    
    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @return the pageName
     */
    public String getPageName() {
        return pageName;
    }

    public boolean isLanguageComplete(Locale locale) {
        if (getCmsPageOptional().isPresent()) {
            return cmsPage.get().isLanguageComplete(locale);
        }
        return false;
    }

    /**
     * @return true only if isUseCmsPage == true and cmsPage != null
     */
    public boolean isHasCmsPage() {
        return getCmsPageId().isPresent();
    }
    
    /**
     * @return the cmsPageId
     */
    public Optional<Long> getCmsPageId() {
        return Optional.ofNullable(cmsPageId);
    }
    
    /**
     * @param cmsPageId the cmsPageId to set
     */
    public void setCmsPageId(Long cmsPageId) {
        this.cmsPageId = cmsPageId;
        if(!getCmsPageOptional().isPresent() || !getCmsPageOptional().get().getId().equals(cmsPageId)) {
            updateCmsPage();
        }
    }

    /**
     * @param cmsPageId2
     */
    private void updateCmsPage() {
        getCmsPageId().ifPresent(id -> {
            try {
                this.cmsPage = Optional.ofNullable(DataManager.getInstance().getDao().getCMSPage(id));
            } catch (DAOException e) {
                this.cmsPage = Optional.empty();
            }
        });
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getPageName().hashCode();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if(obj != null && obj.getClass().equals(this.getClass())) {
            return ((CMSStaticPage)obj).getPageName().equals(this.getPageName());
        } else {
            return false;
        }
    }
    
}
