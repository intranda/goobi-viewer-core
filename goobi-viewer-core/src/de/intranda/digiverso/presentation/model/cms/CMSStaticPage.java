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

public class CMSStaticPage {

    private final String pageName;
    @Deprecated
    private final boolean useCmsPage = true;
    private CMSPage cmsPage = null;

    /**
     * @param pageName
     */
    public CMSStaticPage(String pageName) {
        this.pageName = pageName;
    }

    @Deprecated
    public boolean isUseCmsPage() {
        return useCmsPage;
    }

    @Deprecated
    public void setUseCmsPage(boolean useCmsPage) {
        //		this.useCmsPage = useCmsPage;
        //		System.out.println("Use cms page in " + pageName + ": " + useCmsPage);
//        if (this.cmsPage != null) {
//            if(useCmsPage) {
//                this.cmsPage.addStaticPageName(pageName);                
//            } else {
//                this.cmsPage.removeStaticPageName(pageName);
//            }
//            this.cmsPage.setStaticPageName(useCmsPage ? pageName : null);
//        }
    }

    /**
     * @return the cmsPage
     */
    public CMSPage getCmsPage() {
        return cmsPage;
    }

    /**
     * @param cmsPage the cmsPage to set
     */
    public void setCmsPage(CMSPage cmsPage) {
        
//        if (this.cmsPage != null) {
//            this.cmsPage.setStaticPageName(null);
//        }

//        if (cmsPage == null && this.cmsPage != null) {
//            this.cmsPage.removeStaticPageName(pageName);
//            setUseCmsPage(false);
//        }

        this.cmsPage = cmsPage;
//        if (this.cmsPage != null) {
//            this.cmsPage.addStaticPageName(pageName);
//        }
    }

    /**
     * @return the pageName
     */
    public String getPageName() {
        return pageName;
    }

    public boolean isLanguageComplete(Locale locale) {
        if (isUseCmsPage() && cmsPage != null) {
            return cmsPage.isLanguageComplete(locale);
        }
        return false;
    }

    /**
     * @return true only if isUseCmsPage == true and cmsPage != null
     */
    public boolean isHasCmsPage() {
        return isUseCmsPage() && cmsPage != null;
    }

}
