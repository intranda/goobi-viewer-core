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
package io.goobi.viewer.api.rest;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;

import org.apache.commons.collections4.iterators.ArrayIterator;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientProperties;

/**
 * @author florian
 *
 */
public abstract class AbstractApiUrlManager {

    private static final Logger logger = LogManager.getLogger(AbstractApiUrlManager.class);

    /**
     * @return The base url to the api without trailing slashes
     */
    public abstract String getApiUrl();

    /**
     * @return The base url of the viewer application
     */
    public abstract String getApplicationUrl();

    public static String subPath(String url, String within) {
        if (url.startsWith(within)) {
            return url.substring(within.length());
        }
        return url;
    }

    public String parseParameter(String template, String url, final String param) {
        if (StringUtils.isAnyBlank(url, param)) {
            return "";
        }

        String parameter = param;
        if (!parameter.matches("\\{.*\\}")) {
            parameter = "{" + parameter + "}";
        }
        int paramStart = template.indexOf(parameter);
        if (paramStart < 0) {
            return ""; //not found
        }
        int paramEnd = paramStart + parameter.length();
        String before = template.substring(0, paramStart);
        String after = template.substring(paramEnd);
        if (before.contains("}")) {
            int lastBracketIndex = before.lastIndexOf("}") + 1;
            before = before.substring(lastBracketIndex);
        }
        if (after.contains("{")) {
            int firstBracketIndex = after.indexOf("{");
            after = after.substring(0, firstBracketIndex);
        }
        if (url.contains(before) && url.contains(after)) {
            int urlBeforeEnd = url.indexOf(before) + before.length();
            int urlAfterStartIfNotEmpty = after.length() > 1 ? url.indexOf(after) : url.length() - 1;
            int urlAfterStart = after.length() > 0 ? urlAfterStartIfNotEmpty : url.length();
            return url.substring(urlBeforeEnd, urlAfterStart);
        }

        return "";
    }

    /**
     * @return the path part of the {@link #getApiUrl()}
     */
    public String getApiPath() {
        URI uri = URI.create(getApiUrl());
        return uri.getPath();
    }

    /**
     * @param paths
     * @return {@link ApiPath}
     */
    public ApiPath path(String... paths) {
        String[] array = ArrayUtils.addAll(new String[] { getApiUrl() }, paths);
        return new ApiPath(array);
    }

    public static class ApiPath {

        private final String[] paths;

        public ApiPath(String[] paths) {
            this.paths = paths;
        }

        public ApiPathParams params(Object... params) {
            return new ApiPathParams(this, params);
        }

        public ApiPathQueries query(String key, Object value) {
            return new ApiPathQueries(this).query(key, value);
        }

        public String build() {
            String path = String.join("", this.paths);
            if (!this.paths[this.paths.length - 1].contains(".")) {
                path += "/";
            }
            return path;
        }

        public URI buildURI() {
            return URI.create(this.build());
        }

    }

    public static class ApiPathParams extends ApiPath {

        private final Object[] params;

        public ApiPathParams(ApiPath path, Object[] params) {
            super(path.paths);
            this.params = params;
        }

        @Override
        public String build() {
            return replacePathParams(super.build(), this.params);
        }

        @Override
        public String toString() {
            return build();
        }

        /**
         *
         * @param url
         * @param pathParams
         * @return urlSting with replacements
         */
        static String replacePathParams(final String url, Object[] pathParams) {
            Matcher matcher = Pattern.compile("\\{\\w+\\}").matcher(url);
            Iterator<Object> i = new ArrayIterator<>(pathParams);
            String urlString = url;
            while (matcher.find()) {
                String group = matcher.group();
                if (i.hasNext()) {
                    Object o = i.next();
                    if (o != null) {
                        String replacement = o.toString();
                        // Escape URLs and colons
                        urlString = urlString.replace(group, replacement);
                    }
                } else {
                    //no further params. Cannot keep replacing
                    break;
                }
            }

            //remove trailing slash if the url contains a dot in the last path segment
            if (urlString.matches(".*\\.\\w+\\/")) {
                urlString = urlString.substring(0, urlString.length() - 1);
            }

            return urlString;
        }
    }

    /**
     * Calls the identical method inside the inline class ApiPathParams. For testing purposes.
     *
     * @param urlString
     * @param pathParams
     * @return urlString with replacements
     * @should remove trailing slash if file name contains period
     */
    static String replaceApiPathParams(String urlString, Object[] pathParams) {
        return ApiPathParams.replacePathParams(urlString, pathParams);
    }

    public static class ApiPathQueries extends ApiPathParams {

        private final Map<String, Object> queries;

        public ApiPathQueries(ApiPath path) {

            super(path, initParams(path));
            this.queries = new LinkedHashMap<>();
        }

        private static Object[] initParams(ApiPath path) {
            Object[] params = new Object[0];
            if (path instanceof ApiPathParams) {
                params = ((ApiPathParams) path).params;
            }
            return params;
        }

        @Override
        public ApiPathQueries query(String key, Object value) {
            if (value != null) {
                this.queries.put(key, value);
            }
            return this;
        }

        @Override
        public String toString() {
            return build();
        }

        @Override
        public String build() {
            StringBuilder sbPath = new StringBuilder(super.build());
            String querySeparator = "?";
            for (Entry<String, Object> entry : queries.entrySet()) {
                String value = entry.getValue().toString();
                try {
                    value = URLEncoder.encode(value, StandardCharsets.UTF_8.name());
                } catch (UnsupportedEncodingException e) {
                    logger.error(e.getMessage());
                }
                sbPath.append(querySeparator).append(entry.getKey()).append('=').append(value);
                querySeparator = "&";
            }

            return sbPath.toString();
        }
    }

    public static class ApiInfo {
        private final String name;
        private final String version;
        private final String specification;

        public ApiInfo() {
            this("", "", "");
        }

        /**
         * 
         * @param name
         * @param version
         * @param specification
         */
        public ApiInfo(String name, String version, String specification) {
            this.name = name;
            this.version = version;
            this.specification = specification;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @return the specification
         */
        public String getSpecification() {
            return specification;
        }

        /**
         * @return the version
         */
        public String getVersion() {
            return version;
        }
    }

    /**
     * Calls the {@link #getApiUrl()} and returns a {@link ApiInfo} object if a valid response is returned. Otherwise an object with empty properties
     * is returned Timeout after a maximum of 3 seconds.
     * 
     * @return {@link ApiInfo}
     */
    public ApiInfo getInfo() {
        try {
            Client client = ClientBuilder.newClient();
            client.property(ClientProperties.CONNECT_TIMEOUT, 5000);
            client.property(ClientProperties.READ_TIMEOUT, 5000);
            return client
                    .target(getApiUrl() + "/")
                    .request(MediaType.APPLICATION_JSON)
                    .get(ApiInfo.class);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return new ApiInfo();
        }
    }

    public enum Version {
        v1,
        v2;
    }

}
