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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import com.ocpsoft.pretty.PrettyContext;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.CMSStaticPage;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.servlets.utils.ServletUtils;

/**
 * This class offers static methods to create {@link ViewerPath ViewerPaths} from a http request.
 *
 * @author Florian Alpers
 */
public final class ViewerPathBuilder {

    private ViewerPathBuilder() {
        //
    }

    /**
     * Returns the request path of the given {@code httpRequest} as a {@link io.goobi.viewer.model.urlresolution.ViewerPath}, including information on
     * associated CMSPage and targeted PageType
     *
     * If the url has a pretty-url context and only consists of the server url, "/index" is appended to the url to redirect to the index
     * pretty-mapping Any occurrences of "index.(x)html" are removed from the url to get the actual pretty url
     *
     * @param httpRequest The request from which the path is generated
     * @return a {@link java.util.Optional} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static Optional<ViewerPath> createPath(HttpServletRequest httpRequest) throws DAOException {
        String serverUrl = ServletUtils.getServletPathWithHostAsUrlFromRequest(httpRequest); // http://localhost:8080/viewer
        String serviceUrl = httpRequest.getServletPath(); // /resources/.../index.xhtml
        String serverName = httpRequest.getContextPath(); // /viewer
        String queryString = httpRequest.getQueryString();
        PrettyContext context = PrettyContext.getCurrentInstance(httpRequest);
        if (!serviceUrl.contains("/cms/") && !serviceUrl.contains("/campaigns/") && context != null && context.getRequestURL() != null) {
            serviceUrl = ServletUtils.getServletPathWithHostAsUrlFromRequest(httpRequest)
                    + ("/".equals(context.getRequestURL().toURL()) ? "/index" : context.getRequestURL().toURL());
        }
        serviceUrl = serviceUrl.replaceAll("\\/index\\.x?html", "/");
        return createPath(serverUrl, serverName, serviceUrl, queryString);

    }

    /**
     * <p>
     * createPath.
     * </p>
     *
     * @param request a {@link jakarta.servlet.http.HttpServletRequest} object.
     * @param baseUrl a {@link java.lang.String} object.
     * @return a {@link java.util.Optional} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @should remove server url or name correctly
     */
    public static Optional<ViewerPath> createPath(HttpServletRequest request, String baseUrl) throws DAOException {
        String serverUrl = ServletUtils.getServletPathWithHostAsUrlFromRequest(request); // http://localhost:8080/viewer
        String serverName = request.getContextPath(); // /viewer
        String regexServerUrl = "^" + serverUrl;
        String regexServerName = "^" + serverName;
        String serviceUrl = baseUrl;
        if (baseUrl.matches(Pattern.quote(regexServerUrl))) {
            serviceUrl = serviceUrl.replaceAll(regexServerUrl, "");
        }
        if (baseUrl.matches(Pattern.quote(regexServerName))) {
            serviceUrl = serviceUrl.replaceAll(regexServerName, "");
        }
        return createPath(serverUrl, serverName, serviceUrl, request.getQueryString());
    }

    /**
     * Create a combined path from the given url.
     *
     * If the url leads to a known PageType, associates the PageType with the combined path. If the path leads to a cmsPage, either through direct url
     * {@code /cmds/...}, the cmsPages alternative url or a static page mapping, the cmsPage is associated with this path
     *
     * @param applicationUrl The absolute url of the web-application including the application name ('viewer')
     * @param applicationName The name of the web-application. This is always the last part of the {@code hostUrl}. May be empty
     * @param serviceUrl The complete requested url, optionally including the hostUrl
     * @param queryString
     * @return A {@link io.goobi.viewer.model.urlresolution.ViewerPath} containing the complete path information
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static Optional<ViewerPath> createPath(String applicationUrl, String applicationName, final String serviceUrl, String queryString)
            throws DAOException {
        String useServiceUrl = serviceUrl.replace(applicationUrl, "").replaceAll("^\\/", "");
        try {
            useServiceUrl = URLEncoder.encode(useServiceUrl, "utf-8").replace("%2F", "/");
        } catch (UnsupportedEncodingException e) {
            //
        }

        URI servicePath = URI.create(useServiceUrl);

        servicePath = cleanPath(servicePath);
        String[] pathParts = useServiceUrl.split("/");

        ViewerPath currentPath = new ViewerPath();
        currentPath.setApplicationUrl(applicationUrl);
        currentPath.setApplicationName(applicationName);
        currentPath.setQueryString(queryString);

        if (useServiceUrl.matches("cms/\\d+/.*")) {
            Long cmsPageId = Long.parseLong(pathParts[1]);
            CMSPage page = DataManager.getInstance().getDao().getCMSPage(cmsPageId);
            if (page != null) {
                currentPath.setCmsPage(page);
            }
            currentPath.setPagePath(URI.create(pathParts[0] + "/" + pathParts[1]));
            currentPath.setParameterPath(currentPath.getPagePath().relativize(servicePath));
        } else if (useServiceUrl.matches("campaigns/\\d+/.*")) {
            Long campaignId = Long.parseLong(pathParts[1]);
            Campaign campaign = DataManager.getInstance().getDao().getCampaign(campaignId);
            if (campaign != null) {
                currentPath.setCampaign(campaign);
            }
            currentPath.setPagePath(URI.create(pathParts[0] + "/" + pathParts[1]));
            currentPath.setParameterPath(currentPath.getPagePath().relativize(servicePath));
        } else {
            Optional<PageType> pageType = getPageType(servicePath);
            if (pageType.isPresent()) {
                currentPath.setPagePath(URI.create(pageType.get().getName()));
                currentPath.setParameterPath(currentPath.getPagePath().relativize(servicePath));
                currentPath.setPageType(pageType.get());
                if (pageType.get().isHandledWithCms()) {
                    Optional<CMSStaticPage> staticPage = DataManager.getInstance().getDao().getStaticPageForTypeType(pageType.get());
                    Optional<CMSPage> oCmsPage = staticPage.map(sp -> sp.getCmsPage());
                    if (oCmsPage.isPresent()) {
                        currentPath.setCmsPage(oCmsPage.get());
                    }
                }
            } else {
                // CMS page permalink
                Optional<CMSPage> cmsPage = getCmsPage(servicePath);
                if (cmsPage.isPresent()) {
                    currentPath.setPagePath(URI.create(cmsPage.get().getPersistentUrl()));
                    currentPath.setParameterPath(currentPath.getPagePath().relativize(servicePath));
                    currentPath.setCmsPage(cmsPage.get());
                }
            }
        }
        if (StringUtils.isNotBlank(currentPath.getPagePath().toString())) {
            return Optional.of(currentPath);
        }
        return Optional.empty();
    }

    /**
     * Gets the best matching CMSPage which alternative url ('persistent url') matches the beginning of the given path
     *
     * @param servicePath a {@link java.net.URI} object.
     * @return a {@link java.util.Optional} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static Optional<CMSPage> getCmsPage(URI servicePath) throws DAOException {

        List<CMSPage> cmsPages = DataManager.getInstance().getDao().getAllCMSPages();
        //check pages with longer persistent url first because they have a narrower match and should be preferred
        Collections.sort(cmsPages,
                (p1, p2) -> Integer.compare(StringTools.getLength(p2.getPersistentUrl()), StringTools.getLength(p1.getPersistentUrl())));
        for (CMSPage cmsPage : cmsPages) {
            String pagePath = cmsPage.getPersistentUrl();
            if (StringUtils.isNotBlank(pagePath)) {
                pagePath = pagePath.replaceAll("(^\\/)|(\\/$)", "").trim();
                if (startsWith(servicePath, pagePath)) {
                    return Optional.of(cmsPage);
                }
            }
        }
        return Optional.empty();

    }

    /**
     * <p>
     * getCampaign.
     * </p>
     *
     * @param servicePath a {@link java.net.URI} object.
     * @return a {@link java.util.Optional} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static Optional<Campaign> getCampaign(URI servicePath) throws DAOException {
        List<Campaign> campaigns = DataManager.getInstance().getDao().getAllCampaigns();
        Collections.sort(campaigns, (c1, c2) -> Integer.compare(StringTools.getLength(c2.getPermalink()), StringTools.getLength(c1.getPermalink())));
        for (Campaign campaign : campaigns) {
            String pagePath = campaign.getPermalink();
            if (StringUtils.isNotBlank(pagePath)) {
                pagePath = pagePath.replaceAll("(^\\/)|(\\/$)", "").trim();
                if (startsWith(servicePath, pagePath)) {
                    return Optional.of(campaign);
                }
            }
        }
        return Optional.empty();

    }

    /**
     * Gets the {@link io.goobi.viewer.model.viewer.PageType} that the given path refers to, if any
     *
     * @param servicePath a {@link java.net.URI} object.
     * @return a {@link java.util.Optional} object.
     */
    public static Optional<PageType> getPageType(final URI servicePath) {
        List<PageType> matchingTypes =
                EnumSet.complementOf(EnumSet.of(PageType.other)).stream().filter(type -> type.matches(servicePath)).collect(Collectors.toList());
        matchingTypes.sort((type1, type2) -> Integer.compare(type2.getName().length(), type1.getName().length()));

        return matchingTypes.stream().findFirst();

    }

    /**
     * Returns true if the first parts of the uri (separated by '/') are equal to all parts of the given string (separated by '/'). If the string has
     * more parts than the uri, false is returned
     *
     * @param uri a {@link java.net.URI} object.
     * @param string a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean startsWith(URI uri, final String string) {
        if (uri != null && string != null) {
            String s = string;
            if (uri.toString().endsWith("/") && !s.endsWith("/")) {
                s = s + "/";
            }
            String[] uriParts = uri.toString().split("/");
            String[] stringParts = s.split("/");
            if (uriParts.length < stringParts.length) {
                //no match if the uri contains less path parts than the string to match
                return false;
            }
            boolean match = true;
            for (int i = 0; i < stringParts.length; i++) {
                String uriPart = uriParts[i];
                uriPart = cleanPathPart(uriPart);
                if (!stringParts[i].equals(uriPart)) {
                    match = false;
                }
            }
            return match;
        }
        return false;
    }

    /**
     * @param uriPart
     * @return {@link String}
     */
    private static String cleanPathPart(final String uriPart) {
        String ret = uriPart;
        if (ret.startsWith("!")) {
            ret = ret.substring(1);
        } else if (ret.startsWith("%21")) {
            //escaped '!'
            ret = ret.substring(3);
        }
        return ret;
    }

    /**
     * 
     * @param uri
     * @return {@link URI}
     */
    private static URI cleanPath(URI uri) {
        String string = uri.toString();
        string = cleanPathPart(string);
        return URI.create(string);
    }

    /**
     * <p>
     * resolve.
     * </p>
     *
     * @param master a {@link java.net.URI} object.
     * @param slave a {@link java.net.URI} object.
     * @param fragment
     * @param query
     * @return a {@link java.net.URI} object.
     */
    public static URI resolve(URI master, URI slave, String fragment, String query) {
        return resolve(master, slave.toString(), fragment, query);
    }

    /**
     * 
     * @param master
     * @param slave
     * @return a {@link java.net.URI} object
     */
    public static URI resolve(URI master, String slave) {
        return resolve(master, slave, "", "");
    }

    /**
     * <p>
     * resolve.
     * </p>
     *
     * @param master a {@link java.net.URI} object.
     * @param slave a {@link java.lang.String} object.
     * @param fragment
     * @param query
     * @return a {@link java.net.URI} object.
     */
    public static URI resolve(URI master, final String slave, String fragment, String query) {
        String base = master.toString();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (StringUtils.isBlank(master.toString())) {
            return URI.create(StringTools.appendTrailingSlash(slave) + getFragmentString(fragment) + getQueryString(query));
        }
        return URI.create(base + "/" + slave + getFragmentString(fragment) + getQueryString(query));
    }

    /**
     * @param query
     * @return query with a '?' prefix; empty string if query blank
     */
    private static String getQueryString(String query) {
        if (StringUtils.isBlank(query)) {
            return "";
        }
        return "?" + query;
    }

    /**
     * @param fragment
     * @return fragment with a '#' prefix; empty string if fragment blank
     */
    private static String getFragmentString(String fragment) {
        if (StringUtils.isBlank(fragment)) {
            return "";
        }
        return "#" + fragment;
    }

}
