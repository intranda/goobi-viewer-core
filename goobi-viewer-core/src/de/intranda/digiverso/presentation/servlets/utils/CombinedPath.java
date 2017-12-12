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
package de.intranda.digiverso.presentation.servlets.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;

import de.intranda.digiverso.presentation.model.cms.CMSPage;
import de.intranda.digiverso.presentation.model.viewer.PageType;

/**
 * @author Florian Alpers
 *
 */
public class CombinedPath {

    private String hostUrl;
    private String hostName;
    private Path pagePath;
    private Path parameterPath;
    
    private PageType pageType = null;
    private CMSPage cmsPage = null;
    
    /**
     * 
     */
    public CombinedPath() {
        hostUrl = "";
        
        pagePath = Paths.get("");
        parameterPath = Paths.get("");
    }
    
    
    
    /**
     * @param hostPath
     * @param pagePath
     * @param parameterPath
     */
    public CombinedPath(String hostUrl, String hostName, Path pagePath, Path parameterPath) {
        super();
        this.hostUrl = hostUrl;
        this.hostName = hostName;
        this.pagePath = pagePath;
        this.parameterPath = parameterPath;
    }



    /**
     * @param previousPath
     */
    public CombinedPath(CombinedPath blueprint) {
        this.hostUrl = blueprint.hostUrl;
        this.hostName = blueprint.hostName;
        this.pagePath = blueprint.pagePath;
        this.parameterPath = blueprint.parameterPath;
        this.cmsPage = blueprint.cmsPage;
        this.pageType = blueprint.pageType;
    }



    /**
     * @return the hostUrl
     */
    public String getHostUrl() {
        return hostUrl;
    }
    /**
     * @param hostUrl the hostUrl to set
     */
    public void setHostUrl(String hostUrl) {
        this.hostUrl = hostUrl;
    }
    /**
     * @return the pagePath
     */
    public Path getPagePath() {
        return pagePath;
    }
    /**
     * @param pagePath the pagePath to set
     */
    public void setPagePath(Path pagePath) {
        this.pagePath = pagePath;
    }
    /**
     * @return the parameterPath
     */
    public Path getParameterPath() {
        return parameterPath;
    }
    /**
     * @param parameterPath the parameterPath to set
     */
    public void setParameterPath(Path parameterPath) {
        this.parameterPath = parameterPath;
    }
    
    public Path getCombinedPath() {
        return pagePath.resolve(parameterPath);
    }
    
    public Path getPrettifiedPagePath() {
        if(getCmsPage() != null && StringUtils.isNotBlank(getCmsPage().getPersistentUrl())) {
            return Paths.get(getCmsPage().getPersistentUrl().replaceAll("^\\/|\\/$", ""));
        } else if(getCmsPage() != null && StringUtils.isNotBlank(getCmsPage().getStaticPageName())) {
            return Paths.get(getCmsPage().getStaticPageName().replaceAll("^\\/|\\/$", ""));
        } else {            
            return getPagePath();
        }
    }
    
    public Path getCombinedPrettyfiedPath() {
        return getPrettifiedPagePath().resolve(getParameterPath());
    }
    
    public String getCombinedPrettyfiedUrl() {
        String url = ("/" + getCombinedPrettyfiedPath().toString() + "/").replaceAll("\\/+", "/");
        return url;
    }
    
    public String getCombinedUrl() {
        
        String url = ("/" + getCombinedPath().toString() + "/").replaceAll("\\/+", "/");
        return url;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getCombinedUrl();
    }



    /**
     * @return true if this path has been associated with a pageType other than 'other'
     */
    public boolean isPage() {
        return getPageType() != null && !getPageType().equals(PageType.other);
//        return !getCombinedPath().getFileName().toString().contains(".");
    }
    
    /**
     * @param pageType the pageType to set
     */
    public void setPageType(PageType pageType) {
        this.pageType = pageType;
    }
    
    /**
     * @return the pageType; a return value of NULL means that the type has not been set yet, not that no pageType is associated
     * with this path (in this case PageType.other should be returned)
     */
    public PageType getPageType() {
        return pageType;
    }
    
    public boolean matches(PageType pageType) {
        if(getPageType() != null) {
            return getPageType().matches(getPagePath().toString());
        } else {
            return false;
        }
    }
    
    /**
     * @return the cmsPage if one is associated with this path. Otherwise NULL
     */
    public CMSPage getCmsPage() {
        return cmsPage;
    }
    
    /**
     * @param cmsPage the cmsPage to set
     */
    public void setCmsPage(CMSPage cmsPage) {
        this.cmsPage = cmsPage;
    }

    /**
     * @return the hostName
     */
    public String getHostName() {
        return hostName;
    }
    
    /**
     * @param hostName the hostName to set
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

}

