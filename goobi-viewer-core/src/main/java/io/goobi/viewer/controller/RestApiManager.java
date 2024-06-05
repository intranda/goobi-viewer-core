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
package io.goobi.viewer.controller;

import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.ApiInfo;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.Version;
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

    private static final Logger logger = LogManager.getLogger(RestApiManager.class);

    private Configuration config = null;

    /**
     * Create an instance directly using the unchanged given ApiUrlManagers.
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
        return getDataApiManager(Version.v1);
    }

    public Optional<AbstractApiUrlManager> getDataApiManager(Version version) {
        String apiUrl = this.config.getRestApiUrl();
        if (isLegacyUrl(apiUrl)) {
            return Optional.empty();
        } else if (Version.v2.equals(version)) {
            return Optional.of(new io.goobi.viewer.api.rest.v2.ApiUrls(apiUrl.replace("/api/v1", "/api/v2")));
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
     *         else the configured {@link Configuration#getIIIFApiUrl()}
     */
    public String getContentApiUrl() {
        return config.getIIIFApiUrl();
    }

    /**
     * @return the contentApiManager if it is either set directly or if the configured rest endpoint points to a goobi viewer v1 rest endpoint.
     *         Otherwise null is returned
     */
    public Optional<AbstractApiUrlManager> getContentApiManager() {
        return getContentApiManager(Version.v1);
    }

    public Optional<AbstractApiUrlManager> getContentApiManager(Version version) {
        String apiUrl = this.config.getIIIFApiUrl();
        if (isLegacyUrl(apiUrl)) {
            return Optional.empty();
        } else if (Version.v2.equals(version)) {
            return Optional.of(new io.goobi.viewer.api.rest.v2.ApiUrls(apiUrl.replace("/api/v1", "/api/v2")));
        } else {
            return Optional.of(new ApiUrls(apiUrl));
        }
    }

    public AbstractApiUrlManager getCMSMediaImageApiManager() {
        return getCMSMediaImageApiManager(Version.v1);
    }

    public AbstractApiUrlManager getCMSMediaImageApiManager(Version version) {
        if (DataManager.getInstance().getConfiguration().isUseIIIFApiUrlForCmsMediaUrls()) {
            return getContentApiManager(version).orElse(null);
        }
        
        return getDataApiManager(version).orElse(null);
    }

    /**
     * @param restApiUrl
     * @return true if restApiUrl is legacy URL; false otherwise
     */
    public static boolean isLegacyUrl(String restApiUrl) {
        return !restApiUrl.matches(".*/api/v[12]/?");
    }

    /**
     * @return the url to the data api to use for IIIF resources
     */
    public String getIIIFDataApiUrl() {
        return getDataApiManager(getVersionToUseForIIIF()).map(AbstractApiUrlManager::getApiUrl)
                .orElse(getDataApiManager().map(AbstractApiUrlManager::getApiUrl)
                        .orElse(DataManager.getInstance().getConfiguration().getRestApiUrl()));
    }

    /**
     * @return the url to the content api to use for IIIF resources
     */
    public String getIIIFContentApiUrl() {
        return getIIIFContentApiUrl(getVersionToUseForIIIF());
    }

    /**
     * @param version
     * @return the url to the content api to use for IIIF resources
     */
    public String getIIIFContentApiUrl(Version version) {
        return getContentApiManager(version).map(AbstractApiUrlManager::getApiUrl)
                .orElse(getContentApiManager().map(AbstractApiUrlManager::getApiUrl)
                        .orElse(DataManager.getInstance().getConfiguration().getIIIFApiUrl()));
    }

    public AbstractApiUrlManager getIIIFDataApiManager() {
        return getDataApiManager(getVersionToUseForIIIF())
                .orElse(getDataApiManager().orElse(null));
    }

    public AbstractApiUrlManager getIIIFContentApiManager() {
        return getContentApiManager(getVersionToUseForIIIF())
                .orElse(getContentApiManager().orElse(null));
    }

    /**
     * @return Appropriate API version to use for IIIF
     */
    public static Version getVersionToUseForIIIF() {
        String iiifVersion = DataManager.getInstance().getConfiguration().getIIIFVersionToUse();
        switch (iiifVersion) {
            case "3.0":
                return Version.v2;
            default:
                return Version.v1;
        }
    }

}
