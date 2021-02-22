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
package io.goobi.viewer.api.rest;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import org.apache.commons.collections4.iterators.ArrayIterator;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.StringTools;

/**
 * @author florian
 *
 */
public abstract class AbstractApiUrlManager {

    private static final Logger logger = LoggerFactory.getLogger(AbstractApiUrlManager.class);

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

    public String parseParameter(String template, String url, String parameter) {
        if (StringUtils.isNoneBlank(url, parameter)) {
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
                int urlAfterStart = after.length() > 0 ? (after.length() > 1 ? url.indexOf(after) : url.length() - 1) : url.length();
                String paramValue = url.substring(urlBeforeEnd, urlAfterStart);
                return paramValue;
            }
            return "";
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
     * @param records2
     * @param recordsRssJson
     * @return
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
        
        /**
         * Check whether the given path matches a path given by this ApiPath, regardless of path parameters and ignoring query parameters
         * @param path
         * @return
         */
        public boolean matches(String path) {
            String regex = this.build() + "/?";
            regex = regex.replaceAll("{.*?}", "([^/]+)");
            regex = regex.replaceAll("\\?.*", "");
            return path.replaceAll("\\?.*", "").matches(regex);
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
         * @param urlString
         * @param pathParams
         * @return
         */
        static String replacePathParams(String urlString, Object[] pathParams) {
            Matcher matcher = Pattern.compile("\\{\\w+\\}").matcher(urlString);
            Iterator<Object> i = new ArrayIterator<>(pathParams);
            while (matcher.find()) {
                String group = matcher.group();
                if (i.hasNext()) {
                    String replacement = i.next().toString();
                    // Escape URLs and colons
                    urlString = urlString.replace(group, replacement);
                } else {
                    //no further params. Cannot keep replacing
                    break;
                }
            }
            
            try {
                URI uri = URI.create(urlString);
                String path = uri.getPath();
                path = URLEncoder.encode(path, "utf-8");
//            Path path = FileTools.getPathFromUrlString(urlString);
                if (urlString.endsWith("/") && Paths.get(path).getFileName().toString().contains(".")) {
                    urlString = urlString.substring(0, urlString.length() - 1);
                }
            } catch (UnsupportedEncodingException e) {
            }
            

            return urlString;
        }
    }

    /**
     * Calls the identical method inside the inline class ApiPathParams. For testing purposes.
     * 
     * @param urlString
     * @param pathParams
     * @return
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
            if(value != null) {                
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
            String path = super.build();
            String querySeparator = "?";
            for (String queryParam : queries.keySet()) {
                String value = queries.get(queryParam).toString();
                try {
                    value = URLEncoder.encode(value, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                }
                path = path + querySeparator + queryParam + "=" + value;
                querySeparator = "&";
            }
            return path;
        }
    }

    public static class ApiInfo {
        public String name = "";
        public String version = "";
        public String specification = "";
    }

    /**
     * Calls the {@link #getApiUrl()} and returns a {@link ApiInfo} object if a valid response is returned. Otherwise an object with empty properties
     * is returned Timeout after a maximum of 3 seconds
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
        } catch (Throwable e) {
            logger.error(e.getMessage());
            return new ApiInfo();
        }
    }

}
