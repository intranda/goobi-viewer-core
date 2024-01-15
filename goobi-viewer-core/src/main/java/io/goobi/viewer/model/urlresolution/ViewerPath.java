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
package io.goobi.viewer.model.urlresolution;

import java.io.Serializable;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.viewer.PageType;

/**
 * Stores the url path of a http request organized by its logical parts so application url, application name, view type and parameter urls can be
 * retrieved independendly. If applicable, the {@link io.goobi.viewer.model.viewer.PageType} of the requested view and an associated
 * {@link io.goobi.viewer.model.cms.pages.CMSPage} are also referenced
 * <p>
 * This information helps calling the correct url in different contexts and is also used to redirect to CMSPages and store a brief view history to
 * allow returning to a previous view The entire url always consists of the properties {@link #applicationUrl} + {@link #pagePath} +
 * {@link parameterPath}
 * </p>
 * <p>
 * The easiest way to create ViewerPath based on a http request is by calling
 * {@link io.goobi.viewer.model.urlresolution.ViewerPathBuilder#createPath(HttpServletRequest)} or
 * {@link io.goobi.viewer.model.urlresolution.ViewerPathBuilder#createPath(String, String, String)}
 * </p>
 *
 * @author Florian Alpers
 */
public class ViewerPath implements Serializable {

    private static final long serialVersionUID = 3200000636800001722L;

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
     * The entire query string of the URL, starting with the character after the '?'. An empty String no query exists
     */
    private String queryString = "";
    /**
     * The {@link PageType} referred to by the paths {@link #pagePath}. Is null if no matching PageType was found
     */
    private PageType pageType = null;
    /**
     * The {@link CMSPage} referred to by the paths {@link #pagePath}. Is null if no CMSPage is referenced
     */
    private CMSPage cmsPage = null;

    private Campaign campaign = null;

    /**
     * Creates an empty {@link io.goobi.viewer.model.urlresolution.ViewerPath}. Usually this does not need to be called directly. Instead a ViewerPath
     * should be created by calling {@link io.goobi.viewer.model.urlresolution.ViewerPathBuilder#createPath(HttpServletRequest)} or
     * {@link io.goobi.viewer.model.urlresolution.ViewerPathBuilder#createPath(String, String, String)}
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
     * @param applicationUrl
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
        this.queryString = blueprint.queryString;
    }

    /**
     * <p>
     * Getter for the field <code>applicationUrl</code>.
     * </p>
     *
     * @return the {@link #applicationUrl}
     */
    public String getApplicationUrl() {
        return applicationUrl;
    }

    /**
     * <p>
     * Setter for the field <code>applicationUrl</code>.
     * </p>
     *
     * @param applicationUrl The {@link #applicationUrl} to set
     */
    public void setApplicationUrl(String applicationUrl) {
        this.applicationUrl = applicationUrl;
    }

    /**
     * <p>
     * Getter for the field <code>pagePath</code>.
     * </p>
     *
     * @return the {@link #pagePath}
     */
    public URI getPagePath() {
        return pagePath;
    }

    /**
     * <p>
     * Setter for the field <code>pagePath</code>.
     * </p>
     *
     * @param pagePath the {@link #pagePath} to set
     */
    public void setPagePath(URI pagePath) {
        this.pagePath = pagePath;
    }

    /**
     * <p>
     * Getter for the field <code>parameterPath</code>.
     * </p>
     *
     * @return the {@link #parameterPath}
     */
    public URI getParameterPath() {
        return parameterPath;
    }

    /**
     * <p>
     * Setter for the field <code>parameterPath</code>.
     * </p>
     *
     * @param parameterPath the {@link #parameterPath} to set
     */
    public void setParameterPath(URI parameterPath) {
        this.parameterPath = parameterPath;
    }

    /**
     * @return the queryString
     */
    public String getQueryString() {
        return queryString;
    }

    /**
     * @param queryString the queryString to set
     */
    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    /**
     * <p>
     * getPrettifiedPagePath.
     * </p>
     *
     * @return The alternative url or static page url of a CMSPage if present, otherwise {@link #pagePath}
     */
    public URI getPrettifiedPagePath() {

        Optional<URI> path = Optional.empty();

        if (getCmsPage() != null) {
            try {
                path = DataManager.getInstance()
                        .getDao()
                        .getStaticPageForCMSPage(getCmsPage())
                        .stream()
                        .findFirst()
                        .map(staticPage -> staticPage.getPageName().replaceAll("(^\\/)|(\\/$)", ""))
                        .map(URI::create);
            } catch (DAOException e) {
                //
            }
            if (!path.isPresent() && StringUtils.isNotBlank(getCmsPage().getPersistentUrl())) {
                path = Optional.of(URI.create(getCmsPage().getPersistentUrl().replaceAll("(^\\/)|(\\/$)", "")));
            }
        }
        return path.orElse(getPagePath());

    }

    /**
     * <p>
     * getCombinedPrettyfiedPath.
     * </p>
     *
     * @return the entire {@link #getPrettifiedPagePath() prettified} url <b>except</b> the application url
     */
    public URI getCombinedPrettyfiedPath() {
        return ViewerPathBuilder.resolve(getPrettifiedPagePath(), getParameterPath(), "", getQueryString());
    }

    /**
     * <p>
     * getCombinedPrettyfiedUrl.
     * </p>
     *
     * @return the entire {@link #getPrettifiedPagePath() prettified} url as a path <b>except</b> the application url
     */
    public String getCombinedPrettyfiedUrl() {
        return ("/" + getCombinedPrettyfiedPath().toString()).replaceAll("\\/+", "/").replaceAll("\\\\+", "/");
    }

    /**
     * <p>
     * getCombinedPath.
     * </p>
     *
     * @return the entire request url as a path <b>except</b> the application url
     */
    public URI getCombinedPath() {
        return ViewerPathBuilder.resolve(pagePath, parameterPath, "", queryString);
    }

    /**
     * <p>
     * getCombinedUrl.
     * </p>
     *
     * @return the entire request url <b>except</b> the application url
     */
    public String getCombinedUrl() {
        return ("/" + getCombinedPath().toString()).replace("\\", "/").replaceAll("\\/+", "/").replaceAll("\\\\+", "/");
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getCombinedUrl();
    }

    /**
     * <p>
     * isPage.
     * </p>
     *
     * @return true if this path has been associated with a pageType other than 'other'
     */
    public boolean isPage() {
        return getPageType() != null && !getPageType().equals(PageType.other);
        //        return !getCombinedPath().getFileName().toString().contains(".");
    }

    /**
     * <p>
     * Setter for the field <code>pageType</code>.
     * </p>
     *
     * @param pageType the {@link io.goobi.viewer.model.viewer.PageType} to set
     */
    public void setPageType(PageType pageType) {
        this.pageType = pageType;
    }

    /**
     * <p>
     * Getter for the field <code>pageType</code>.
     * </p>
     *
     * @return the {@link #pageType}
     */
    public PageType getPageType() {
        return pageType;
    }

    /**
     * <p>
     * matches.
     * </p>
     *
     * @see PageType#matches(Path)
     * @param pageType a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @return The matching {@link io.goobi.viewer.model.viewer.PageType} or null if no PageType matches
     */
    public boolean matches(PageType pageType) {
        if (pageType != null) {
            return pageType.matches(getPagePath());
        }
        return false;
    }

    /**
     * <p>
     * Getter for the field <code>cmsPage</code>.
     * </p>
     *
     * @return the {@link #cmsPage} if one is associated with this path. Otherwise null
     */
    public CMSPage getCmsPage() {
        return cmsPage;
    }

    /**
     * <p>
     * Setter for the field <code>cmsPage</code>.
     * </p>
     *
     * @param cmsPage the {@link #cmsPage} to set
     */
    public void setCmsPage(CMSPage cmsPage) {
        this.cmsPage = cmsPage;
    }

    /**
     * <p>
     * Getter for the field <code>campaign</code>.
     * </p>
     *
     * @return the campaign
     */
    public Campaign getCampaign() {
        return campaign;
    }

    /**
     * <p>
     * Setter for the field <code>campaign</code>.
     * </p>
     *
     * @param campaign the campaign to set
     */
    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    /**
     * <p>
     * Getter for the field <code>applicationName</code>.
     * </p>
     *
     * @return the {@link #applicationName}
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * <p>
     * Setter for the field <code>applicationName</code>.
     * </p>
     *
     * @param applicationName the {@link #applicationName} to set
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * <p>
     * isCmsPage.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isCmsPage() {
        return getCmsPage() != null;
    }

}
