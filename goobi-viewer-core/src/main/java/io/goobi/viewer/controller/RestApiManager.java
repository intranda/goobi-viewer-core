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
package io.goobi.viewer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.ApiInfo;
import io.goobi.viewer.api.rest.v1.ApiUrls;

/**
 * <p>
 * Handles urls to configured rest api endpoints.
 * </p>
 * <p>
 * Two endpoints are managed: one for data (solr, dao) and one for content (image, ocr, other media) An {@link AbstractApiUrlManager} is kept for
 * both. These can either be injected directly in the constructor or resolved from a {@link Configuration}.
 * </p>
 * <p>
 * In the latter case, data resources url is taken from {@link Configuration#getRestApiUrl()}, content resources url is taken from
 * {@link Configuration#getIIIFApiUrl()}.
 * </p>
 * <p>
 * Both urls are updated if the configuration changes. Also, if the configured url contains '/rest' that part is rewritten to '/api/v1' if the
 * rewritten url points to a goobi viewer v1 rest api.
 * </p>
 * 
 * @author florian
 *
 */
public class RestApiManager {

    private static final Logger logger = LoggerFactory.getLogger(RestApiManager.class);

    /**
     * Handles URLs to data resources (index, dao)
     */
    private AbstractApiUrlManager dataApiManager = null;
    /**
     * Handles URLs to content resources (images, ocr and other media)
     */
    private AbstractApiUrlManager contentApiManager = null;

    private String configuredDataApiUrl = null;
    private String configuredContentApiUrl = null;
    private Configuration config = null;

    /**
     * Create an instance directly using the unchanged given ApiUrlManagers
     * 
     * @param dataApiManager
     * @param contentApiManager
     */
    public RestApiManager(AbstractApiUrlManager dataApiManager, AbstractApiUrlManager contentApiManager) {
        this.dataApiManager = dataApiManager;
        this.contentApiManager = contentApiManager;
    }

    /**
     * Create an instance based on configuration. Final urls may change from configured ones
     * 
     * @param config
     */
    public RestApiManager(Configuration config) {
        this.config = config;
        updateDataUrlManager();
        updateContentUrlManager();
    }

    /**
     * @return the dataApiManager if it is either set directly or if the configured rest endpoint points to a goobi viewer v1 rest endpoint. Otherwise
     *         null is returned
     */
    public AbstractApiUrlManager getDataApiManager() {
        this.updateDataUrlManager();
        return dataApiManager;
    }

    /**
     * @return The url to the data api. Either {@link AbstractApiUrlManager#getApiUrl()} of the {@link #getDataApiManager()} if it exists, or else the
     *         configured {@link Configuration#getRestApiUrl()}
     */
    public String getDataApiUrl() {
        AbstractApiUrlManager manager = getDataApiManager();
        if (manager != null) {
            return manager.getApiUrl();
        }

        return this.configuredDataApiUrl;
    }

    /**
     * @return The url to the content api. Either {@link AbstractApiUrlManager#getApiUrl()} of the {@link #getContentApiManager()} if it exists, or
     *         else the configured {@link Configuration#getIIIApiUrl()}
     */
    public String getContentApiUrl() {
        AbstractApiUrlManager manager = getContentApiManager();
        if (manager != null) {
            return manager.getApiPath();
        }

        return this.configuredContentApiUrl;
    }

    /**
     * @return the contentApiManager if it is either set directly or if the configured rest endpoint points to a goobi viewer v1 rest endpoint.
     *         Otherwise null is returned
     */
    public AbstractApiUrlManager getContentApiManager() {
        this.updateContentUrlManager();
        return contentApiManager;
    }

    private void updateContentUrlManager() {
        if (this.config != null) {
            String url = this.config.getIIIFApiUrl();
            if (this.configuredContentApiUrl == null || !this.configuredContentApiUrl.equals(url)) {
                //url not initialized or changed
                url = url.replace("/rest", "/api/v1");
                ApiUrls urls = new ApiUrls(url);
                ApiInfo info = urls.getInfo();
                if (info != null && info.version.equals("v1")) {
                    contentApiManager = urls;
                } else {
                    logger.error("API info not found; URL: {}", url);
                    contentApiManager = null;
                }
                this.configuredContentApiUrl = url;
            }
        }
    }

    private void updateDataUrlManager() {
        if (this.config == null) {
            return;
        }
        String url = this.config.getRestApiUrl();
        if (this.configuredDataApiUrl == null || !this.configuredContentApiUrl.equals(url)) {
            //url not initialized or changed
            url = url.replace("/rest", "/api/v1");
            // logger.debug("REST API URL: {}", url);
            ApiUrls urls = new ApiUrls(url);
            ApiInfo info = urls.getInfo();
            if (info != null && info.version.equals("v1")) {
                dataApiManager = urls;
            } else {
                logger.error("API info not found; URL: {}", url);
                dataApiManager = null;
            }
            this.configuredDataApiUrl = url;
        }
    }

}
