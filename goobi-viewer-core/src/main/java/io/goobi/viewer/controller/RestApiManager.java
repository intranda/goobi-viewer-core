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

import java.util.Optional;

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

    private Configuration config = null;

    /**
     * Create an instance directly using the unchanged given ApiUrlManagers
     * 
     * @param dataApiManager
     * @param contentApiManager
     */
    public RestApiManager() {
        this(DataManager.getInstance().getConfiguration());
    }

    /**
     * Create an instance based on configuration. Final urls may change from configured ones
     * 
     * @param config
     */
    public RestApiManager(Configuration config) {
        this.config = config;
    }

    /**
     * @return the dataApiManager if it is either set directly or if the configured rest endpoint points to a goobi viewer v1 rest endpoint. Otherwise
     *         null is returned
     */
    public Optional<AbstractApiUrlManager> getDataApiManager() {
        String apiUrl = this.config.getRestApiUrl();
        if(isLegacyUrl(apiUrl)) {
            return Optional.empty();
        } else {
            return Optional.of(new ApiUrls(apiUrl));
        }
    }

    /**
     * @return The url to the data api. Either {@link AbstractApiUrlManager#getApiUrl()} of the {@link #getDataApiManager()} if it exists, or else the
     *         configured {@link Configuration#getRestApiUrl()}
     */
    public String getDataApiUrl() {
        return config.getRestApiUrl();
    }

    /**
     * @return The url to the content api. Either {@link AbstractApiUrlManager#getApiUrl()} of the {@link #getContentApiManager()} if it exists, or
     *         else the configured {@link Configuration#getIIIApiUrl()}
     */
    public String getContentApiUrl() {
        return config.getIIIFApiUrl();
    }

    /**
     * @return the contentApiManager if it is either set directly or if the configured rest endpoint points to a goobi viewer v1 rest endpoint.
     *         Otherwise null is returned
     */
    public Optional<AbstractApiUrlManager> getContentApiManager() {
        String apiUrl = this.config.getIIIFApiUrl();
        if(isLegacyUrl(apiUrl)) {
            return Optional.empty();
        } else {
            return Optional.of(new ApiUrls(apiUrl));
        }
    }

    /**
     * @param restApiUrl
     * @return
     */
    public static boolean isLegacyUrl(String restApiUrl) {
        return !restApiUrl.matches(".*/api/v1/?");
    }

}
