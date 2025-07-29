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
package io.goobi.viewer.api.rest.v2;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.controller.DataManager;

/**
 * @author florian
 *
 */
public class ApiUrls extends AbstractApiUrlManager {

    public static final String API = "/api/v2";

    public static final String AUTH = "/auth";
    public static final String AUTH_LOGIN = "/login";
    public static final String AUTH_ACCESS_TOKEN = "/token";
    public static final String AUTH_LOGOUT = "/logout";
    public static final String AUTH_PROBE = "/probe";
    public static final String AUTH_PROBE_REQUEST = "/probe/{pi}/{filename}";

    public static final String CACHE = "/cache";
    public static final String CACHE_RECORD = "/{pi}";

    public static final String INDEXER = "/indexer";

    public static final String INDEX = "/index";
    public static final String INDEX_FIELDS = "/fields";
    public static final String INDEX_QUERY = "/query";
    public static final String INDEX_STREAM = "/stream";
    public static final String INDEX_STATISTICS = "/statistics";

    public static final String RECORDS_RSS = "/records/rss";
    public static final String RECORDS_RSS_JSON = "/channel.json";

    public static final String RECORDS_CHANGES = "/records/changes";
    public static final String RECORDS_CHANGES_PAGE = "/{pageNo}";

    public static final String RECORDS_LIST = "/records/list";

    public static final String RECORDS_RECORD = "/records/{pi}";
    public static final String RECORDS_RIS_TEXT = "/ris.txt";
    public static final String RECORDS_RIS_FILE = "/ris";
    public static final String RECORDS_TOC = "/toc";
    public static final String RECORDS_ANNOTATIONS = "/annotations";
    public static final String RECORDS_ANNOTATIONS_PAGE = "/annotations/1";
    public static final String RECORDS_CMDI_LANG = "/cmdi/{lang}";
    public static final String RECORDS_COMMENTS = "/comments";
    public static final String RECORDS_COMMENTS_PAGE = "/comments/1";
    public static final String RECORDS_METADATA_SOURCE = "/metadata/source";
    public static final String RECORDS_MANIFEST = "/manifest";
    public static final String RECORDS_MANIFEST_AUTOCOMPLETE = "/manifest/autocomplete";
    public static final String RECORDS_MANIFEST_SEARCH = "/manifest/search";
    public static final String RECORDS_NER_TAGS = "/ner/tags";
    public static final String RECORDS_PLAINTEXT = "/plaintext";
    public static final String RECORDS_PLAINTEXT_ZIP = "/plaintext.zip";
    public static final String RECORDS_ALTO = "/alto";
    public static final String RECORDS_ALTO_ZIP = "/alto.zip";
    public static final String RECORDS_TEI = "/tei";
    public static final String RECORDS_TEI_LANG = "/tei/{lang}";
    public static final String RECORDS_TEI_ZIP = "/tei.zip";
    public static final String RECORDS_EPUB = "/epub";
    public static final String RECORDS_EPUB_INFO = "/epub/info.json";
    public static final String RECORDS_PDF = "/pdf";
    public static final String RECORDS_PDF_INFO = "/pdf/info.json";
    public static final String RECORDS_IMAGE = "/representative";
    public static final String RECORDS_IMAGE_INFO = "/representative/info.json";
    public static final String RECORDS_IMAGE_IIIF = "/representative/{region}/{size}/{rotation}/{quality}.{format}";

    public static final String RECORDS_SECTIONS = "/records/{pi}/sections/{divId}";
    public static final String RECORDS_SECTIONS_RIS_TEXT = "/ris.txt";
    public static final String RECORDS_SECTIONS_RIS_FILE = "/ris";
    public static final String RECORDS_SECTIONS_RANGE = "/range";
    public static final String RECORDS_SECTIONS_PDF = "/pdf";
    public static final String RECORDS_SECTIONS_PDF_INFO = "/pdf/info.json";

    public static final String RECORDS_PAGES = "/records/{pi}/pages/{pageNo}";
    public static final String RECORDS_PAGES_CANVAS = "/canvas";
    public static final String RECORDS_PAGES_MANIFEST = "/manifest";
    public static final String RECORDS_PAGES_MEDIA = "/media";
    public static final String RECORDS_PAGES_NER_TAGS = "/ner/tags";
    public static final String RECORDS_PAGES_ANNOTATIONS = "/annotations";
    public static final String RECORDS_PAGES_COMMENTS = "/comments";
    public static final String RECORDS_PAGES_COMMENTS_COMMENT = "/comments/{id}";
    public static final String RECORDS_PAGES_TEXT = "/text";

    public static final String RECORDS_FILES = "/records/{pi}/files";
    public static final String RECORDS_FILES_PLAINTEXT = "/plaintext/{filename}";
    public static final String RECORDS_FILES_ALTO = "/alto/{filename}";
    public static final String RECORDS_FILES_CMDI = "/cmdi/{filename}";
    public static final String RECORDS_FILES_TEI = "/tei/{filename}";

    public static final String RECORDS_FILES_SOURCE = "/source/{filename}";
    public static final String RECORDS_FILES_AUDIO = "/audio/{mimetype}/{filename}";
    public static final String RECORDS_FILES_VIDEO = "/video/{mimetype}/{filename}";

    public static final String RECORDS_FILES_IMAGE = "/records/{pi}/files/images/{filename}";
    public static final String RECORDS_FILES_IMAGE_PDF = "/full.pdf";
    public static final String RECORDS_FILES_IMAGE_INFO = "/info.json";
    public static final String RECORDS_FILES_IMAGE_IIIF = "/{region}/{size}/{rotation}/{quality}.{format}";

    public static final String RECORDS_FILES_FOOTER = "/records/{pi}/files/footer/{filename}";
    public static final String RECORDS_FILES_FOOTER_IIIF = "/{region}/{size}/{rotation}/{quality}.{format}";

    public static final String RECORDS_FILES_3D = "/records/{pi}/files/3d/{filename}";
    public static final String RECORDS_FILES_3D_INFO = "/info.json";
    public static final String RECORDS_FILES_3D_AUXILIARY_FILE_1 = "/{subfolder}/{auxfilename}";
    public static final String RECORDS_FILES_3D_AUXILIARY_FILE_1_ALT = "/{subfolder}//{auxfilename}";
    public static final String RECORDS_FILES_3D_AUXILIARY_FILE_2 = "/{subfolder}/{subsubfolder}/{auxfilename}";
    public static final String RECORDS_FILES_3D_AUXILIARY_FILE_2_ALT = "//{subfolder}/{subsubfolder}/{auxfilename}";

    public static final String COLLECTIONS = "/collections/{field}";
    public static final String COLLECTIONS_COLLECTION = "/{collection}";
    public static final String COLLECTIONS_CONTENTASSIST = "/contentassist";

    public static final String USERS = "/users";
    public static final String USERS_USERID = "/users/{userId}";

    public static final String USERS_BOOKMARKS = "/bookmarks";
    public static final String USERS_BOOKMARKS_LIST = "/{listId}";
    public static final String USERS_BOOKMARKS_ITEM = "/{listId}/items/{bookmarkId}";
    public static final String USERS_BOOKMARKS_LIST_IIIF = "/{listId}/collection.json";
    public static final String USERS_BOOKMARKS_LIST_MIRADOR = "/{listId}/mirador.json";
    public static final String USERS_BOOKMARKS_LIST_RSS = "/{listId}/rss.xml";
    public static final String USERS_BOOKMARKS_LIST_RSS_JSON = "/{listId}/rss.json";
    public static final String USERS_BOOKMARKS_LIST_STATISTICS = "/{listId}/statistics";
    public static final String USERS_BOOKMARKS_SHARED = "/shared";
    public static final String USERS_BOOKMARKS_PUBLIC = "/public";
    public static final String USERS_BOOKMARKS_LIST_SHARED = "/shared/{key}";
    public static final String USERS_BOOKMARKS_LIST_SHARED_MIRADOR = "/shared/{key}/mirador.json";
    public static final String USERS_BOOKMARKS_LIST_SHARED_IIIF = "/shared/{key}/collection.json";
    public static final String USERS_BOOKMARKS_LIST_SHARED_RSS = "/shared/{key}/rss.xml";
    public static final String USERS_BOOKMARKS_LIST_SHARED_RSS_JSON = "/shared/{key}/rss.json";

    public static final String USERS_CURRENT = "/current";

    public static final String AUTHORITY = "/authority";
    public static final String AUTHORITY_RESOLVER = "/resolver";

    public static final String OPENSEARCH = "/opensearch";

    public static final String SEARCH = "/search";
    public static final String SEARCH_HIT_CHILDREN = "/hit/{id}/{numChildren}";

    public static final String TASKS = "/tasks";
    public static final String TASKS_TASK = "/{id}";

    public static final String CONTEXT = "/context";

    public static final String CROWDSOURCING_CAMPAIGN = "/crowdsourcing/campaings/{campaignId}";
    public static final String CROWDSOURCING_CAMPAIGN_ANNOTATIONS = "/annotations";
    public static final String CROWDSOURCING_CAMPAIGN_RECORDS = "/records/{pi}";
    public static final String CROWDSOURCING_CAMPAIGN_RECORDS_ANNOTATIONS = "/records/{pi}/annotations";

    public static final String ANNOTATIONS = "/annotations";
    public static final String ANNOTATIONS_PAGE = "/pages/{pageNo}";
    public static final String ANNOTATIONS_ANNOTATION = "/annotation_{id}";
    public static final String ANNOTATIONS_COMMENT = "/comment_{id}";
    public static final String ANNOTATIONS_PLAINTEXT = "/plaintext_{pi}_{pageNo}";
    public static final String ANNOTATIONS_ALTO = "/alto_{pi}_{pageNo}_{elementId}";
    public static final String ANNOTATIONS_METADATA = "/metadata_{pi}_{divId}_{field}";
    public static final String ANNOTATIONS_UGC = "/ugc_{id}";

    public static final String LOCALIZATION = "/localization";
    public static final String LOCALIZATION_VOCABS = "/vocabularies";
    public static final String LOCALIZATION_VOCABS_FILE = "/vocabularies/{filename}";
    public static final String LOCALIZATION_TRANSLATIONS = "/translations";

    public static final String CMS = "/cms";
    public static final String CMS_MEDIA = "/cms/media";
    public static final String CMS_MEDIA_BY_CATEGORY = "/category/{tags}";
    public static final String CMS_MEDIA_ITEM = "/{id}";
    public static final String CMS_MEDIA_ITEM_BY_ID = "/{id: \\d+}";
    public static final String CMS_MEDIA_ITEM_BY_FILE = "/{filename: [^\\/]*\\.\\w{1,4}}";
    public static final String CMS_MEDIA_FILES = "/files";
    public static final String CMS_MEDIA_FILES_FILE = "/files/{filename}";
    public static final String CMS_MEDIA_FILES_FILE_PDF = "/files/{filename: (?i)[^\\/]*\\.(pdf)}";
    public static final String CMS_MEDIA_FILES_FILE_HTML = "/files/{filename: (?i)[^\\/]*\\.(html)}";
    public static final String CMS_MEDIA_FILES_FILE_SVG = "/files/{filename: (?i)[^\\/]*\\.(svg)}";
    public static final String CMS_MEDIA_FILES_FILE_ICO = "/files/{filename: (?i)[^\\/]*\\.(ico)}";
    public static final String CMS_MEDIA_FILES_FILE_AUDIO = "/files/{filename: (?i).*\\.(mp3|mpeg|wav|ogg|wma)}";
    public static final String CMS_MEDIA_FILES_FILE_VIDEO = "/files/{filename: (?i).*\\.(mp4|mpeg4|avi|mov|wmv)}";
    public static final String CMS_MEDIA_FILES_FILE_IMAGE = "/cms/media/files/{filename: (?i)[^\\/]*\\.(jpe?g|tiff?|png|gif|jp2)}";
    public static final String CMS_MEDIA_FILES_FILE_IMAGE_IIIF = "/{region}/{size}/{rotation}/{quality}.{format}";

    public static final String TEMP_MEDIA_FILES = "/temp/files";
    public static final String TEMP_MEDIA_FILES_FOLDER = "/{folder}";
    public static final String TEMP_MEDIA_FILES_FILE = "/{folder}/{filename}";

    public static final String TEMP_MEDIA_FILES_FILE_IMAGE = "/temp/files/{folder}/{filename: (?i)[^\\/]*\\.(jpe?g|tiff?|png|gif|jp2)}";
    public static final String TEMP_MEDIA_FILES_FILE_IIIF = "/{region}/{size}/{rotation}/{quality}.{format}";

    public static final String EXTERNAL_IMAGES = "/images/external/{filename}";
    public static final String EXTERNAL_IMAGES_IIIF = "/{region}/{size}/{rotation}/{quality}.{format}";

    private final String apiUrl;

    public ApiUrls() {
        this(DataManager.getInstance().getConfiguration().getRestApiUrl().replace("/api/v1", API));
    }

    public ApiUrls(final String apiUrl) {
        if (StringUtils.isNotBlank(apiUrl) && apiUrl.endsWith("/")) {
            this.apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
        } else {
            this.apiUrl = apiUrl;
        }
    }

    @Override
    public String getApiUrl() {
        return this.apiUrl;
    }

    @Override
    public String getApplicationUrl() {
        return this.apiUrl.replace("/api/v2", "").replace("/rest", "");
    }

}
