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

import org.apache.commons.lang.StringUtils;

import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.controller.DataManager;

/**
 * @author florian
 *
 */
public class ApiUrls extends AbstractApiUrlManager {

    public static final String RECORDS_INDEX =                              "/records";
    public static final String RECORDS_QUERY =                              "/query";
    public static final String RECORDS_STATISTICS =                         "/statistics";
    
    public static final String RECORDS_RSS =                                "/records/rss";
    public static final String RECORDS_RSS_JSON =                           "/channel.json";
    
    public static final String RECORDS_CHANGES =                            "/records/changes";
    public static final String RECORDS_CHANGES_PAGE =                       "/{pageNo}";
    
    public static final String RECORDS_RECORD =                             "/records/{pi}";
    public static final String RECORDS_RIS_TEXT =                           "/ris.txt";
    public static final String RECORDS_RIS_FILE =                           "/ris";
    public static final String RECORDS_TOC =                                "/toc";
    public static final String RECORDS_ANNOTATIONS =                        "/annotations";
    public static final String RECORDS_COMMENTS =                           "/comments";
    public static final String RECORDS_COMMENTS_COMMENT =                   "/comments/{id}";
    public static final String RECORDS_METADATA_SOURCE =                    "/metadata/source";
    public static final String RECORDS_MANIFEST =                           "/manifest";    
    public static final String RECORDS_MANIFEST_AUTOSUGGEST =               "/manifest/autosuggest";
    public static final String RECORDS_MANIFEST_SEARCH =                    "/manifest/search";
    public static final String RECORDS_LAYER =                              "/layers/{name}";
    public static final String RECORDS_NER_TAGS =                           "/ner/tags";
    public static final String RECORDS_PLAINTEXT =                          "/plaintext";
    public static final String RECORDS_PLAINTEXT_ZIP =                      "/plaintext.zip";
    public static final String RECORDS_ALTO =                               "/alto";
    public static final String RECORDS_ALTO_ZIP =                           "/alto.zip";
    public static final String RECORDS_TEI =                                "/tei";
    public static final String RECORDS_TEI_ZIP =                            "/tei.zip";
    public static final String RECORDS_PDF =                                "/pdf";
    public static final String RECORDS_PDF_INFO =                           "/pdf/info.json";
    
    public static final String RECORDS_SECTIONS =                            "/records/{pi}/sections/{divId}";
    public static final String RECORDS_SECTIONS_RIS_TEXT =                   "/ris.txt";
    public static final String RECORDS_SECTIONS_RIS_FILE =                   "/ris";
    public static final String RECORDS_SECTIONS_RANGE =                     "/range";
    public static final String RECORDS_SECTIONS_PDF =                       "/pdf";
    public static final String RECORDS_SECTIONS_PDF_INFO =                  "/pdf/info.json";
    
    public static final String RECORDS_PAGES =                              "/records/{pi}/pages";
    public static final String RECORDS_PAGES_SEQUENCE =                     "/sequence/base";
    public static final String RECORDS_PAGES_CANVAS =                       "/{pageNo}/canvas";
    public static final String RECORDS_PAGES_NER_TAGS =                     "/{pageNo}/ner/tags";
    public static final String RECORDS_PAGES_ANNOTATIONS =                  "/{pageNo}/annotations";
    public static final String RECORDS_PAGES_COMMENTS =                     "/{pageNo}/comments";
    public static final String RECORDS_PAGES_COMMENTS_COMMENT =             "/{pageNo}/comments/{id}";
    
    public static final String RECORDS_FILES =                              "/records/{pi}/files";
    public static final String RECORDS_FILES_PLAINTEXT =                    "/plaintext/{filename}";
    public static final String RECORDS_FILES_ALTO =                         "/alto/{filename}";
    public static final String RECORDS_FILES_TEI =                          "/tei/{filename}";
    public static final String RECORDS_FILES_PDF =                          "/pdf/{filename}";

    
    public static final String RECORDS_FILES_IMAGE =                        "/records/{pi}/files/images/{filename}";
    public static final String RECORDS_FILES_IMAGE_PDF =                    "/full.pdf";
    public static final String RECORDS_FILES_IMAGE_INFO =                   "/info.json";
    public static final String RECORDS_FILES_IMAGE_IIIF =                   "/{region}/{size}/{rotation}/{quality}.{format}";

    
    public static final String COLLECTIONS =                                "/collections/{field}";
    public static final String COLLECTIONS_COLLECTION =                     "/{collection}";
    public static final String COLLECTIONS_CONTENTASSIST =                  "/contentassist";
    
    public static final String DOWNLOADS =                                  "/downloads/records/{pi}";
    public static final String DOWNLOADS_EPUB =                             "/epub";
    public static final String DOWNLOADS_PDF =                              "/pdf";
    public static final String DOWNLOADS_SECTIONS_EPUB =                    "/sections/{divId}/epub";
    public static final String DOWNLOADS_SECTIONS_PDF =                     "/sections/{divId}/pdf";
    
    public static final String USERS =                                      "/users/{userId}";
    public static final String USERS_BOOKMARKS =                            "/bookmarks";
    public static final String USERS_BOOKMARKS_LIST =                       "/bookmarks/{listId}";
    public static final String USERS_BOOKMARKS_LIST_RSS =                   "/bookmarks/{listId}/rss.xml";
    public static final String USERS_BOOKMARKS_LIST_RSS_JSON =              "/{userId}/bookmarks/{listId}/rss.json";
    public static final String USERS_BOOKMARKS_LIST_STATISTICS =            "/{userId}/bookmarks/{listId}/statistics";
    public static final String USERS_BOOKMARKS_SHARED =                     "/{userId}/bookmarks/shared";
    public static final String USERS_BOOKMARKS_PUBLIC =                     "/{userId}/bookmarks/public";
    
    public static final String AUTHORITY =                                  "/authorities/{authority}/{id}";
    
    public static final String CROWDSOURCING_CAMPAIGN =                     "/crowdsourcing/campaings/{campaignId}";
    public static final String CROWDSOURCING_CAMPAIGN_ANNOTATIONS =         "/annotations";
    public static final String CROWDSOURCING_CAMPAIGN_RECORDS =             "/records/{pi}";
    public static final String CROWDSOURCING_CAMPAIGN_RECORDS_ANNOTATIONS = "/records/{pi}/annotations";
    
    public static final String ANNOTATIONS =                                "/annotations";
    public static final String ANNOTATIONS_PAGE =                           "/pages/{pageNo}";
    public static final String ANNOTATIONS_ANNOTATION =                     "/{id}";
    
    public static final String LOCALIZATION =                               "/localization";
    public static final String LOCALIZATION_VOCABS =                        "/vocabularies";
    public static final String LOCALIZATION_VOCABS_FILE =                   "/vocabularies/{filename}";
    public static final String LOCALIZATION_TRANSLATIONS =                  "/translations";



    
    
    private final String apiUrl;
    
    public ApiUrls() {
        this.apiUrl = DataManager.getInstance().getConfiguration().getRestApiUrl();
    }
    
    public ApiUrls(String apiUrl) {
        if(StringUtils.isNotBlank(apiUrl) && apiUrl.endsWith("/")) {
            apiUrl = apiUrl.substring(0, apiUrl.length()-1);
        }
        this.apiUrl = apiUrl;
    }

    @Override
    public String getApiUrl() {
        return this.apiUrl;
    }

    @Override
    public String getApplicationUrl() {
        return this.apiUrl.replace("/api/v1", "").replace("/rest", "");
    }

}
