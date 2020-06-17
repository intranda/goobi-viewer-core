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
package io.goobi.viewer.api.rest.v1;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.iterators.ArrayIterator;
import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.api.rest.IApiUrlManager;
import io.goobi.viewer.controller.DataManager;

/**
 * @author florian
 *
 */
public class ApiUrlManager implements IApiUrlManager {

    public static final String RECORDS_QUERY =                              "/records/query";
    public static final String RECORDS_STATISTICS =                         "/records/statistics";
    public static final String RECORDS_RSS =                                "/records/rss.xml";
    public static final String RECORDS_RSS_JSON =                           "/records/rss.json";
    public static final String RECORDS_RSS_SUBTHEME =                       "/records/themes/{subtheme}/rss.xml";
    public static final String RECORDS_RSS_JSON_SUBTHEME =                  "/records/themes/{subtheme}/rss.json";
    public static final String RECORDS_CHANGES =                            "/records/changes";
    public static final String RECORDS_CHANGES_PAGE =                       "/records/changes/pages/{pageNo}";
    public static final String RECORDS_RIS_TEXT =                           "/records/{pi}/ris.txt";
    public static final String RECORDS_RIS_FILE =                           "/records/{pi}/ris.ris";
    public static final String RECORDS_TOC =                                "/records/{pi}/toc";
    public static final String RECORDS_ANNOTATIONS =                        "/records/{pi}/annotations";
    public static final String RECORDS_COMMENTS =                           "/records/{pi}/comments";
    public static final String RECORDS_COMMENTS_COMMENT =                   "/records/{pi}/comments/{id}";
    public static final String RECORDS_METADATA_SOURCE =                    "/records/{pi}/metadata/source";
    public static final String RECORDS_MANIFEST =                           "/records/{pi}/manifest";
    public static final String RECORDS_LAYER =                              "/records/{pi}/layers/{name}";
    public static final String RECORDS_NER_TAGS =                           "/records/{pi}/ner/tags";
    public static final String RECORDS_PLAINTEXT =                          "/records/{pi}/plaintext";
    public static final String RECORDS_ALTO =                               "/records/{pi}/alto";
    public static final String RECORDS_TEI =                                "/records/{pi}/tei";
    public static final String RECORDS_PDF =                                "/records/{pi}/pdf";
    public static final String RECORDS_SECTIONS_RANGE =                     "/records/{pi}/sections/{divId}/range";
    public static final String RECORDS_SECTIONS_LAYER =                     "/records/{pi}/sections/{divId}/layers/base";
    public static final String RECORDS_SECTIONS_NER_TAGS =                  "/records/{pi}/sections/{divId}/ner/tags";
    public static final String RECORDS_SECTIONS_ANNOTATIONS =               "/records/{pi}/sections/{divId}/annotations";
    public static final String RECORDS_SECTIONS_PLAINTEXT =                 "/records/{pi}/sections/{divId}/plaintext";
    public static final String RECORDS_SECTIONS_ALTO =                      "/records/{pi}/sections/{divId}/alto";
    public static final String RECORDS_SECTIONS_PDF =                       "/records/{pi}/sections/{divId}/pdf";
    public static final String RECORDS_PAGES_SEQUENCE =                     "/records/{pi}/pages/sequence/{name}";
    public static final String RECORDS_PAGES_CANVAS =                       "/records/{pi}/pages/{pageNo}/canvas";
    public static final String RECORDS_PAGES_NER_TAGS =                     "/records/{pi}/pages/{pageNo}/ner/tags";
    public static final String RECORDS_PAGES_ANNOTATIONS =                  "/records/{pi}/pages/{pageNo}/annotations";
    public static final String RECORDS_PAGES_COMMENTS =                     "/records/{pi}/pages/{pageNo}/comments";
    public static final String RECORDS_PAGES_COMMENTS_COMMENT =             "/records/{pi}/pages/{pageNo}/comments/{id}";
    public static final String RECORDS_FILES_PLAINTEXT =                    "/records/{pi}/files/plaintext/{filename}";
    public static final String RECORDS_FILES_ALTO =                         "/records/{pi}/files/alto/{filename}";
    public static final String RECORDS_FILES_PDF =                          "/records/{pi}/files/pdf/{filename}";
    public static final String RECORDS_FILES_IMAGE =                        "/records/{pi}/files/images/{filename}";
    public static final String COLLECTIONS =                                "/collections/{field}";
    public static final String COLLECTIONS_COLLECTION =                     "/collections/{field}/{collection}";
    public static final String COLLECTIONS_CONTENTASSIST =                  "/collections/{field}/contentassist";
    public static final String DOWNLOADS_EPUB =                             "/downloads/records/{pi}/epub";
    public static final String DOWNLOADS_PDF =                              "/downloads/records/{pi}/pdf";
    public static final String DOWNLOADS_SECTIONS_EPUB =                    "/downloads/records/{pi}/sections/{divId}/epub";
    public static final String DOWNLOADS_SECTIONS_PDF =                     "/downloads/records/{pi}/sections/{divId}/pdf";
    public static final String USERS =                                      "/users/{userId}";
    public static final String USERS_BOOKMARKS =                            "/users/{userId}/bookmarks";
    public static final String USERS_BOOKMARKS_LIST =                       "/users/{userId}/bookmarks/{listId}";
    public static final String USERS_BOOKMARKS_LIST_RSS =                   "/users/{userId}/bookmarks/{listId}/rss.xml";
    public static final String USERS_BOOKMARKS_LIST_RSS_JSON =              "/users/{userId}/bookmarks/{listId}/rss.json";
    public static final String USERS_BOOKMARKS_LIST_STATISTICS =            "/users/{userId}/bookmarks/{listId}/statistics";
    public static final String USERS_BOOKMARKS_SHARED =                     "/users/{userId}/bookmarks/shared";
    public static final String USERS_BOOKMARKS_PUBLIC =                     "/users/{userId}/bookmarks/public";
    public static final String AUTHORITY =                                  "/authorities/{authority}/{id}";
    public static final String CROWDSOURCING_CAMPAIGN =                     "/crowdsourcing/campaings/{campaignId}";
    public static final String CROWDSOURCING_CAMPAIGN_ANNOTATIONS =         "/crowdsourcing/campaings/{campaignId}/annotations";
    public static final String CROWDSOURCING_CAMPAIGN_RECORDS =             "/crowdsourcing/campaings/{campaignId}/records/{pi}";
    public static final String CROWDSOURCING_CAMPAIGN_RECORDS_ANNOTATIONS = "/crowdsourcing/campaings/{campaignId}/records/{pi}/annotations";
    public static final String ANNOTATIONS =                                "/annotations";
    public static final String ANNOTATIONS_PAGE =                           "/annotations/pages/{pageNo}";
    public static final String ANNOTATIONS_ANNOTATION =                     "/annotations/{id}";
    public static final String LOCALIZATION_VOCABS =                        "/localization/vocabularies";
    public static final String LOCALIZATION_VOCABS_FILE =                   "/localization/vocabularies/{filename}";
    public static final String LOCALIZATION_TRANSLATIONS =                  "/localization/translations";



    
    
    private final String apiUrl;
    
    public ApiUrlManager() {
        this.apiUrl = DataManager.getInstance().getConfiguration().getRestApiUrl();
    }
    
    public ApiUrlManager(String apiUrl) {
        if(StringUtils.isNotBlank(apiUrl) && apiUrl.endsWith("/")) {
            apiUrl = apiUrl.substring(0, apiUrl.length()-1);
        }
        this.apiUrl = apiUrl;
    }
    
    @Override
    public String getUrl(String path, String...pathParams) {
        if(!path.startsWith("/")) {
            path = "/" + path;
        }
        String url = this.apiUrl + path;
        if(pathParams != null && pathParams.length > 0) {
            url = replacePathParams(url, pathParams);
        }
        return url;
    }

    @Override
    public String getUrl(String path, Map<String, String> queryParams, String...pathParams) {
        path = this.getUrl(path, pathParams);
        String querySeparator = "?";
        for (String queryParam : queryParams.keySet()) {
            String value = queryParams.get(queryParam);
            path = path + querySeparator + queryParam + "=" + value;
            querySeparator = "&";
        }
        return path;
    }

    @Override
    public String getApiUrl() {
        return this.apiUrl;
    }

    @Override
    public String getApplicationUrl() {
        return this.apiUrl.replace("/api/v1", "").replace("/rest", "");
    }

    @SuppressWarnings("unchecked")
    private String replacePathParams(String url, String[] pathParams) {
        Matcher matcher = Pattern.compile("\\{\\w+\\}").matcher(url);
        Iterator<String> i = new ArrayIterator(pathParams);
        while(matcher.find()) {
            String group = matcher.group();
            if(i.hasNext()) {
                String replacement = i.next();
                url = url.replace(group, replacement);
            } else {
                //no further params. Cannot keep replacing
                break;
            }
        }
        return url;
    }


}
