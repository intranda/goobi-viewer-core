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
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.json.JSONObject;

import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrConstants;

public final class WebArchiveReader {

    private static final Logger logger = LogManager.getLogger(WebArchiveReader.class);

    private WebArchiveReader() {
    }

    /**
     * Returns the seed URL from the first WACZ file indexed for the given PI, or an empty string if no seed page is found.
     *
     * <p>
     * If local WACZ files are indexed, their {@code pages/pages.jsonl} is read for the first {@code seed:true} URL. Only when no local archive
     * documents exist does this fall back to the {@code url} parameter of an external {@code MD_WEBARCHIVE_IDENTIFIER}.
     *
     * @param pi persistent identifier of the record
     * @return the resolved seed URL, or {@code ""} if none is found or the lookup fails
     * @should not use the external fallback when local archive docs exist
     * @should return the url param value from the fallback identifier when no local docs exist
     * @should return empty string when the fallback identifier has no url param
     * @should skip a malformed fallback identifier and use the next one
     * @should return empty string when neither the local nor the fallback query find anything
     */
    public static String getSeedUrl(String pi) {
        // Catch only the checked exceptions the query/file-read pipeline declares; a missing FILENAME field is guarded
        // explicitly below rather than swallowed as a blanket RuntimeException.
        try {
            var docs = DataManager.getInstance()
                    .getSearchIndex()
                    .getDocs("+PI_TOPSTRUCT:%s +DOCTYPE:PAGE +MIMETYPE:application/warc".formatted(pi),
                            Collections.emptyList());
            if (docs != null && !docs.isEmpty()) {
                for (SolrDocument doc : docs) {
                    // Guard against docs without a FILENAME value so the lookup degrades to "" instead of throwing an NPE
                    Object filenameValue = doc.getFieldValue(SolrConstants.FILENAME);
                    if (filenameValue == null) {
                        continue;
                    }
                    String filename = filenameValue.toString();
                    Path waczPath = DataFileTools.getDataFilePath(pi,
                            DataManager.getInstance().getConfiguration().getMediaFolder(), null, filename);
                    if (Files.isRegularFile(waczPath)) {
                        String url = readSeedUrlFromWacz(waczPath);
                        if (StringUtils.isNotBlank(url)) {
                            return url;
                        }
                    }
                }
                return "";
            }

            return findExternalSeedUrl(pi);

        } catch (IOException | IndexUnreachableException | PresentationException e) {
            logger.warn("Could not read seed URL from web archive for PI {}: {}", pi, e.getMessage());
            return "";
        }
    }

    /**
     * Finds the seed URL among {@code MD_WEBARCHIVE_IDENTIFIER} values on the document matching {@code PI:<pi>}: the
     * first non-blank {@code url} query parameter value among them, or {@code ""} if none is found.
     *
     * @param pi persistent identifier of the record
     * @return the resolved seed URL, or {@code ""} if no matching document or usable identifier is found
     */
    private static String findExternalSeedUrl(String pi) throws IndexUnreachableException, PresentationException {
        var docs = DataManager.getInstance()
                .getSearchIndex()
                .getDocs("+PI:%s +MD_WEBARCHIVE_IDENTIFIER:*".formatted(pi), Collections.emptyList());
        if (docs == null || docs.isEmpty()) {
            return "";
        }

        for (SolrDocument doc : docs) {
            Collection<Object> rawIdentifiers = doc.getFieldValues(SolrConstants.MD_WEBARCHIVE_IDENTIFIER);
            if (rawIdentifiers == null) {
                continue;
            }
            for (Object rawIdentifier : rawIdentifiers) {
                String value = rawIdentifier.toString();
                if (StringUtils.isBlank(value)) {
                    continue;
                }
                try {
                    // The seed URL lives in the URL's fragment (replayweb.page convention: "?source=...#url=..."),
                    // not its query string.
                    String seedUrl = extractParamValue(new URI(value).getRawFragment(), "url");
                    if (StringUtils.isNotBlank(seedUrl)) {
                        return seedUrl;
                    }
                } catch (URISyntaxException e) {
                    logger.warn("Could not parse web archive identifier URL '{}': {}", value, e.getMessage());
                }
            }
        }
        return "";
    }

    /**
     * Looks up the value of a named parameter in a raw {@code &}-delimited parameter string, such as a URI's query or
     * fragment component.
     *
     * @param rawParams raw parameter string, e.g. {@code uri.getRawQuery()} or {@code uri.getRawFragment()}
     * @param paramName name of the parameter to look up
     * @return the parameter's decoded value, or {@code null} if {@code rawParams} is {@code null} or has no such parameter
     * @should return the value of the named param
     * @should return null when the named param is absent
     * @should return null when rawParams is null
     * @should find the requested param among several
     */
    public static String extractParamValue(String rawParams, String paramName) {
        if (rawParams == null) {
            return null;
        }
        for (NameValuePair param : URLEncodedUtils.parse(rawParams, StandardCharsets.UTF_8)) {
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
