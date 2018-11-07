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
package de.intranda.digiverso.presentation.model.urlresolution;

import java.net.URI;
import java.nio.file.Path;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.model.cms.CMSPage;
import de.intranda.digiverso.presentation.model.viewer.PageType;

/**
 * Stores the url path of a http request organized by its logical parts so application url, application name, view type and parameter urls can be
 * retrieved independendly. If applicable, the {@link PageType} of the requested view and an associated {@link CMSPage} are also referenced
 * <p>
 * This information helps calling the correct url in different contexts and is also used to redirect to CMSPages and store a brief view history to
 * allow returning to a previous view The entire url always consists of the properties {@link #applicationUrl} + {@link #pagePath} +
 * {@link parameterPath}
 * </p>
 * <p>
 * The easiest way to create ViewerPath based on a http request is by calling {@link ViewerPathBuilder#createPath(HttpServletRequest)} or
 * {@link ViewerPathBuilder#createPath(String, String, String)}
 * </p>
 * 
 * @author Florian Alpers
 *
 */
public class ViewerPath {
    /**
     * The absolute url of the web-application, e.g. {@code "http://localhost:8080/viewer"}. The {@link #applicationName} is always the last part of
     * this url
     */
    private String applicationUrl;
    /**
     * The name of the web application called by this url, e.g. {@code "/viewer"}. If the application is the root application of the server this
     * property is {@code "/"}
     */
    private String applicationName;
    /**
     * The part of the url referring to the called view (e.g. {@code "/image", "/search" or "/cms/editOcr"}). This usually matches a {@link PageType}
     * name or an alternative cms page url
     */
    private URI pagePath;
    /**
     * The entire url path after the {@link #pagePath}. This part contains search parameters and the like
     */
    private URI parameterPath;
    /**
     * The {@link PageType} referred to by the paths {@link #pagePath}. Is null if no matching PageType was found
     */
    private PageType pageType = null;
    /**
     * The {@link CMSPage} referred to by the paths {@link #pagePath}. Is null if no CMSPage is referenced
     */
    private CMSPage cmsPage = null;

    /**
     * Creates an empty {@link ViewerPath}. Usually this does not need to be called directly. Instead a ViewerPath should be created by calling
     * {@link ViewerPathBuilder#createPath(HttpServletRequest)} or {@link ViewerPathBuilder#createPath(String, String, String)}
     * 
     */
    public ViewerPath() {
        applicationUrl = "";
        applicationName = "";
        pagePath = URI.create("");
        parameterPath = URI.create("");
    }

    /**
     * Creates a {@link ViewerPath} based on the given request properties. This should not be called directly. Instead a ViewerPath should be created
     * by calling {@link ViewerPathBuilder#createPath(HttpServletRequest)} or {@link ViewerPathBuilder#createPath(String, String, String)}
     * 
     * @param applicationPath
     * @param applicationName
     * @param pagePath
     * @param parameterPath
     */
    ViewerPath(String applicationUrl, String applicationName, URI pagePath, URI parameterPath) {
        super();
        this.applicationUrl = applicationUrl;
        this.applicationName = applicationName;
        this.pagePath = pagePath;
        this.parameterPath = parameterPath;
    }

    /**
     * Creates an exact copy of the passed {@code blueprint}. This only creates a shallow copy, which is irrelevant to almost all properties which are
     * immutables, {@link #cmsPage} being the only exception (but that is ok since all paths should indeed point to the same CMSPage)
     * 
     * @param blueprint The ViewerPath to be copied
     */
    public ViewerPath(ViewerPath blueprint) {
        this.applicationUrl = blueprint.applicationUrl;
        this.applicationName = blueprint.applicationName;
        this.pagePath = blueprint.pagePath;
        this.parameterPath = blueprint.parameterPath;
        this.cmsPage = blueprint.cmsPage;
        this.pageType = blueprint.pageType;
    }

    /**
     * @return the {@link #applicationUrl}
     */
    public String getApplicationUrl() {
        return applicationUrl;
    }

    /**
     * @param applicationUrl The {@link #applicationUrl} to set
     */
    public void setApplicationUrl(String applicationUrl) {
        this.applicationUrl = applicationUrl;
    }

    /**
     * @return the {@link #pagePath}
     */
    public URI getPagePath() {
        return pagePath;
    }

    /**
     * @param pagePath the {@link #pagePath} to set
     */
    public void setPagePath(URI pagePath) {
        this.pagePath = pagePath;
    }

    /**
     * @return the {@link #parameterPath}
     */
    public URI getParameterPath() {
        return parameterPath;
    }

    /**
     * @param parameterPath the {@link #parameterPath} to set
     */
    public void setParameterPath(URI parameterPath) {
        this.parameterPath = parameterPath;
    }

    /**
     * 
     * @return The alternative url or static page url of a CMSPage if present, otherwise {@link #pagePath}
     */
    public URI getPrettifiedPagePath() {
        if (getCmsPage() != null && StringUtils.isNotBlank(getCmsPage().getPersistentUrl())) {
            return URI.create(getCmsPage().getPersistentUrl().replaceAll("^\\/|\\/$", ""));
        } else if (getCmsPage() != null && StringUtils.isNotBlank(getCmsPage().getStaticPageName())) {
            return URI.create(getCmsPage().getStaticPageName().replaceAll("^\\/|\\/$", ""));
        } else if (getCmsPage() != null) {
            try {
                return DataManager.getInstance().getDao().getStaticPageForCMSPage(getCmsPage()).stream().findFirst()
                .map(staticPage -> staticPage.getPageName().replaceAll("^\\/|\\/$", ""))
                .map(pageName -> URI.create(pageName))
                .orElse(getPagePath());
            } catch (DAOException e) {
            }
        }
        return getPagePath();
    }

    /**
     * @return the entire {@link #getPrettifiedPagePath() prettified} url <b>except</b> the application url
     */
    public URI getCombinedPrettyfiedPath() {
        return ViewerPathBuilder.resolve(getPrettifiedPagePath(), getParameterPath());
    }

    /**
     * @return the entire {@link #getPrettifiedPagePath() prettified} url as a path <b>except</b> the application url
     */
    public String getCombinedPrettyfiedUrl() {
        String url = ("/" + getCombinedPrettyfiedPath().toString() + "/").replaceAll("\\/+", "/").replaceAll("\\\\+", "/");
        return url;
    }

    /**
     * @return the entire request url as a path <b>except</b> the application url
     */
    public URI getCombinedPath() {
        return ViewerPathBuilder.resolve(pagePath, parameterPath);
    }

    /**
     * @return the entire request url <b>except</b> the application url
     */
    public String getCombinedUrl() {

        String url = ("/" + getCombinedPath().toString() + "/").replace("\\", "/").replaceAll("\\/+", "/").replaceAll("\\\\+", "/");
        return url;
    }

    /**
     * @return The {@link #getCombinedUrl() combined url}
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
     * @param pageType the {@link PageType} to set
     */
    public void setPageType(PageType pageType) {
        this.pageType = pageType;
    }

    /**
     * @return the {@link #pageType}
     */
    public PageType getPageType() {
        return pageType;
    }

    /**
     * @see PageType#matches(Path)
     * @param pageType
     * @return The matching {@link PageType} or null if no PageType matches
     */
    public boolean matches(PageType pageType) {
        if (getPageType() != null) {
            return getPageType().matches(getPagePath());
        } else {
            return false;
        }
    }

    /**
     * @return the {@link #cmsPage} if one is associated with this path. Otherwise null
     */
    public CMSPage getCmsPage() {
        return cmsPage;
    }

    /**
     * @param cmsPage the {@link #cmsPage} to set
     */
    public void setCmsPage(CMSPage cmsPage) {
        this.cmsPage = cmsPage;
    }

    /**
     * @return the {@link #applicationName}
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * @param applicationName the {@link #applicationName} to set
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public boolean isCmsPage() {
        return getCmsPage() != null;
    }

}
