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
package io.goobi.viewer.model.media.webarchives;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.solr.SolrConstants;

public class WebArchiveReader {

    private static final Logger logger = LogManager.getLogger(WebArchiveReader.class);

    private WebArchiveReader() {
    }

    /**
     * Returns the seed URL from the first WACZ file indexed for the given PI, or an empty string if no seed page is found.
     */
    public static String getSeedUrl(String pi) {
        try {
            var docs = DataManager.getInstance()
                    .getSearchIndex()
                    .getDocs("+PI_TOPSTRUCT:%s +DOCTYPE:PAGE +MIMETYPE:application/warc".formatted(pi),
                            Collections.emptyList());
            if (docs.isEmpty()) {
                return "";
            }
            String filename = docs.get(0).getFieldValue(SolrConstants.FILENAME).toString();
            Path waczPath = DataFileTools.getDataFilePath(pi,
                    DataManager.getInstance().getConfiguration().getMediaFolder(), null, filename);
            if (!Files.isRegularFile(waczPath)) {
                return "";
            }
            return readSeedUrlFromWacz(waczPath);
        } catch (Exception e) {
            logger.warn("Could not read seed URL from web archive for PI {}: {}", pi, e.getMessage());
            return "";
        }
    }

    /**
     * Looks up the value of a query parameter on an already-parsed URI.
     *
     * @param uri URI to inspect
     * @param paramName name of the query parameter to look up
     * @return the parameter's decoded value, or {@code null} if it is not present
     * @should return the value of the named query parameter
     * @should return null when the named query parameter is absent
     * @should return null when the uri has no query
     * @should find the requested param among several
     */
    public static String extractQueryParamValue(URI uri, String paramName) {
        for (NameValuePair param : URLEncodedUtils.parse(uri, StandardCharsets.UTF_8)) {
            if (paramName.equals(param.getName())) {
                return param.getValue();
            }
        }
        return null;
    }

    /**
     * Reads the first seed URL from the {@code pages/pages.jsonl} entry inside a WACZ file.
     */
    public static String readSeedUrlFromWacz(Path waczPath) throws IOException {
        try (ZipFile zip = new ZipFile(waczPath.toFile())) {
            ZipEntry entry = zip.getEntry("pages/pages.jsonl");
            if (entry == null) {
                return "";
            }
            try (InputStream is = zip.getInputStream(entry);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines()
                        .filter(line -> line.contains("\"seed\":true") || line.contains("\"seed\": true"))
                        .findFirst()
                        .map(line -> new JSONObject(line).optString("url", ""))
                        .orElse("");
            }
        }
    }
}
