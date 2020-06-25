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
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.iterators.ArrayIterator;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;


/**
 * @author florian
 *
 */
public abstract class AbstractApiUrlManager {
    
    /**
     * @return The base url to the api without trailing slashes
     */
    public abstract String getApiUrl();
    
    /**
     * @return  The base url of the viewer application
     */
    public abstract String getApplicationUrl();
    
    public static String subPath(String url, String within) {
        if(url.startsWith(within)) {
            return url.substring(within.length());
        } else {
            return url;
        }
    }
    
    public String parseParameter(String template, String url, String parameter) {
        if(StringUtils.isNoneBlank(url, parameter)) {
            if(!parameter.matches("\\{.*\\}")) {
                parameter = "{" + parameter + "}";
            }
            int paramStart = template.indexOf(parameter);
            if(paramStart < 0) {
                return "";  //not found
            }
            int paramEnd = paramStart + parameter.length();
            String before = template.substring(0, paramStart);
            String after = template.substring(paramEnd);
            if(before.contains("}")) {
                int lastBracketIndex = before.lastIndexOf("}")+1;
                before = before.substring(lastBracketIndex);
            }
            if(after.contains("{")) {
                int firstBracketIndex = after.indexOf("{");
                after = after.substring(0, firstBracketIndex);
            }
            if(url.contains(before) && url.contains(after)) {
                int urlBeforeEnd = url.indexOf(before) + before.length();
                int urlAfterStart = after.length() > 0 ? url.indexOf(after) : url.length();
                String paramValue = url.substring(urlBeforeEnd, urlAfterStart);
                return paramValue;
            } else {
                return "";
            }
        } else {
            return "";
        }
    }

    /**
     * @param records2
     * @param recordsRssJson
     * @return
     */
    public ApiPath path(String...paths) {
        String[] array = (String[]) ArrayUtils.addAll(new String[] {getApiUrl()}, paths);
        return new ApiPath(array);
    }

    
    public static class ApiPath {
        
        private final String[] paths;
        
        public ApiPath(String[] paths) {
            this.paths = paths;
        }
        
        public ApiPathParams params(Object...params) {
            return new ApiPathParams(this, params);
        }
        
        public ApiPathQueries query(String key, Object value) {
            return new ApiPathQueries(this).query(key, value);
        }
        
        public String build() {
            String path = String.join("", this.paths);
            return path;
        }
    }
    
    public static class ApiPathParams extends ApiPath{
        
        private final Object[] params;
        
        public ApiPathParams(ApiPath path, Object[] params) {
            super(path.paths);
            this.params = params;
        }
        
        public String build() {
            return replacePathParams(super.build(), this.params);
        }
        
        public String toString() {
            return build();
        }
        
        private String replacePathParams(String url, Object[] pathParams) {
            Matcher matcher = Pattern.compile("\\{\\w+\\}").matcher(url);
            Iterator i = new ArrayIterator(pathParams);
            while(matcher.find()) {
                String group = matcher.group();
                if(i.hasNext()) {
                    String replacement = i.next().toString();
                    url = url.replace(group, replacement);
                } else {
                    //no further params. Cannot keep replacing
                    break;
                }
            }
            return url;
        }
    }
    
    public static class ApiPathQueries extends ApiPathParams{
        
        private final Map<String, Object> queries;
        
        public ApiPathQueries(ApiPath path) {
            
            super(path, initParams(path));
            this.queries = new LinkedHashMap<>();
        }
        
        private static Object[] initParams(ApiPath path) {
            Object[] params = new Object[0];
            if(path instanceof ApiPathParams) {
                params = ((ApiPathParams) path).params;
            }
            return params;
        }
        
        public ApiPathQueries query(String key, Object value) {
            this.queries.put(key, value);
            return this;
        }
        
        public String toString() {
            return build();
        }
        
        public String build() {
            String path = super.build();
            String querySeparator = "?";
            for (String queryParam : queries.keySet()) {
                String value = queries.get(queryParam).toString();
                path = path + querySeparator + queryParam + "=" + value;
                querySeparator = "&";
            }
            return path;
        }
    }
}
