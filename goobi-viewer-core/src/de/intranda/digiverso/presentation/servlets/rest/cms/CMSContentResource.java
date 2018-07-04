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
package de.intranda.digiverso.presentation.servlets.rest.cms;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.cms.CMSContentItem;
import de.intranda.digiverso.presentation.model.cms.CMSPage;
import de.intranda.digiverso.presentation.model.cms.CMSSidebarElement;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;

/**
 * Provides methods to access cms-content to be embedded into pages with <ui:include>
 * 
 * getPageUrl(), getContentUrl() and getSidebarElementUrl() provide urls to cms-pages, content and sidebar-element respectively. The other methods
 * resolve these urls and return the appropriate html content. All urls are absolute urls including scheme information (http), as ui:include cannot
 * resolve them otherwise (only file urls can be resolved with relative paths)
 */
@Path("/cms")
@ViewerRestServiceBinding
public class CMSContentResource {

    private static final Logger logger = LoggerFactory.getLogger(CMSContentResource.class);

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
    private static final long REQUEST_TIMEOUT = 3000;//3s

    private enum TargetType {
        PAGE,
        CONTENT,
        SIDEBAR
    }

    @GET
    @Path("/content/{pageId}/{language}/{contentId}")
    @Produces({ MediaType.TEXT_PLAIN })
    public String getContentHtml(@PathParam("pageId") Long pageId, @PathParam("language") String language, @PathParam("contentId") String contentId)
            throws IOException, DAOException, ServletException {
        String output = createResponseInThread(TargetType.CONTENT, pageId, language, contentId, REQUEST_TIMEOUT);
        return wrap(output);
    }

    @GET
    @Path("/page/{pageId}")
    @Produces({ MediaType.TEXT_HTML })
    public String getPageUrl(@PathParam("pageId") Long pageId) throws IOException, DAOException, ServletException {
        String output = createResponseInThread(TargetType.PAGE, pageId, null, null, REQUEST_TIMEOUT);
        return wrap(output);
    }

    @GET
    @Path("/sidebar/{elementId}")
    @Produces({ MediaType.TEXT_PLAIN })
    public String getSidebarElementHtml(@PathParam("elementId") Long elementId) throws IOException, DAOException, ServletException {
        String output = createResponseInThread(TargetType.SIDEBAR, elementId, null, null, REQUEST_TIMEOUT);
        return wrap(output);
    }

    /**
     * @param output
     * @return
     */
    protected String wrap(String string) {
        String output = "";
        if (StringUtils.isNotBlank(string)) {
            output = replaceHtmlCharacters(string);
            output = "<span>" + output + "</span>";
        } else {
            output = "<span></span>";
        }
        logger.trace("Sending cms content string '{}'", output);
        return output;
    }

    private static String createResponseInThread(final TargetType target, final Long pageId, final String language, final String contentId,
            final long timeout/*ms*/) throws IOException, DAOException, ServletException {

        logger.trace("Creating response for Target = '{}', pageId = '{}', language = '{}', fieldId = '{}'", target, pageId, language, contentId);

        Callable<String> job = new Callable<String>() {

            @Override
            public String call() throws Exception {
                return createResponse(target, pageId, language, contentId);
            }
        };

        Future<String> result = executor.submit(job);

        try {
            return result.get(timeout, TimeUnit.MILLISECONDS);
        } catch (CancellationException | InterruptedException e) {
            //servlet request cancelled or servlet thread interrupted. No answer needed
            return null;
        } catch (ExecutionException e) {
            //error creating response
            Throwable cause = e.getCause();
            if (cause != null && cause instanceof IOException) {
                throw (IOException) cause;
            } else if (cause != null && cause instanceof DAOException) {
                throw (DAOException) cause;
            } else {
                throw new ServletException(cause);
            }
        } catch (TimeoutException e) {
            throw new DAOException("Timeout while accessing database: " + e.getMessage());
        }
    }

    private static String createResponse(TargetType target, Long pageId, String language, String contentId) throws IOException, DAOException {
        String output = null;
        switch (target) {
            case CONTENT:
                output = getValue(pageId, contentId, language);
                break;
            case SIDEBAR:
                output = getSidebarElement(pageId);
                break;
            default: // nothing
        }
        return output;
    }

    /**
     * @param request
     * @return
     * @throws IOException
     * @throws DAOException
     */
    private static String getSidebarElement(Long elementId) throws IOException, DAOException {
        try {
            CMSSidebarElement element = DataManager.getInstance().getDao().getCMSSidebarElement(elementId);
            String html = element.getHtml();
            if (StringUtils.isNotBlank(html)) {
                return html;
            }
        } catch (NumberFormatException e) {
            logger.error("Value of 'element' parameter is not a long: {}", e.getMessage());
        }

        return null;
    }

    /**
     * @param include
     * @return
     */
    private static String replaceHtmlCharacters(String input) {
        String output = StringEscapeUtils.unescapeHtml(input);
        output = output.replace("&", "&amp;");
        return output;
    }

    /**
     * @param pageId
     * @param fieldId
     * @return
     * @throws DAOException
     */
    private static String getValue(Long pageId, String fieldId, String language) throws DAOException {
        CMSPage page = DataManager.getInstance().getDao().getCMSPage(pageId);
        if (page != null) {
            CMSContentItem item = page.getContentItem(fieldId, language);
            if (item == null || item.getHtmlFragment() == null) {
                item = page.getDefaultLanguage().getContentItem(fieldId);
            }
            if (item != null) {
                return item.getHtmlFragment();
            }
        }
        return null;
    }

    /**
     * @param cmsPage
     * @return
     */
    public static String getPageUrl(CMSPage cmsPage) {
        if (cmsPage != null) {
            StringBuilder urlBuilder = new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
            urlBuilder.append("/rest/cms/");
            urlBuilder.append(TargetType.PAGE.name().toLowerCase());
            urlBuilder.append("/");
            urlBuilder.append(cmsPage.getId()).append("/").append("/");
            urlBuilder.append("?timestamp=").append(System.currentTimeMillis());
            logger.debug("CMS rest api url = {}", urlBuilder.toString());
            return urlBuilder.toString();
        }
        return "";
    }

    /**
     * @param page
     * @param item
     */
    public static String getContentUrl(CMSContentItem item) {
        if (item != null) {
            StringBuilder urlBuilder = new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
            //	              StringBuilder urlBuilder = new StringBuilder(BeanUtils.getRequest().getContextPath());

            urlBuilder.append("/rest/cms/");
            urlBuilder.append(TargetType.CONTENT.name().toLowerCase());
            urlBuilder.append("/");
            urlBuilder.append(item.getOwnerPageLanguageVersion().getOwnerPage().getId());
            urlBuilder.append("/");
            urlBuilder.append(item.getOwnerPageLanguageVersion().getLanguage());
            urlBuilder.append("/");
            urlBuilder.append(item.getItemId()).append("/");
            urlBuilder.append("?timestamp=").append(System.currentTimeMillis());
            logger.debug("CMS rest api url = {}", urlBuilder.toString());
            return urlBuilder.toString();
        }
        return "";
    }

    public static String getSidebarElementUrl(CMSSidebarElement item) {
        if (item != null && item.hasHtml()) {
            StringBuilder urlBuilder = new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
            urlBuilder.append("/rest/cms/");
            urlBuilder.append(TargetType.SIDEBAR.name().toLowerCase());
            urlBuilder.append("/");
            urlBuilder.append(item.getId()).append("/");
            urlBuilder.append("?timestamp=").append(System.currentTimeMillis());
            return urlBuilder.toString();
        }
        return "";
    }

}
