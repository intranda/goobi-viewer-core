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
package io.goobi.viewer.model.resources.download;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.files.external.ExternalFilesDownloader;
import io.goobi.viewer.model.variables.VariableReplacer;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * Resolves the set of permitted download URLs for a given record identifier by expanding admin-configured URL templates with record-specific
 * metadata.
 */
public class ExternalResourceUrlService {

    @FunctionalInterface
    public interface VariableReplacerFactory {
        /**
         * @return a VariableReplacer for the given PI, or {@code null} if the record is not found
         */
        VariableReplacer create(String pi) throws PresentationException, IndexUnreachableException;
    }

    private final VariableReplacerFactory factory;
    private final List<String> urlTemplates;
    private final Map<String, Map<String, String>> cache = new HashMap<>();
    private BiPredicate<String, String> existenceChecker = ExternalFilesDownloader::resourceExists;
    private Map<String, String> existingUrls = null;

    public ExternalResourceUrlService() {
        this.urlTemplates = DataManager.getInstance().getConfiguration().getExternalResourceUrlTemplates();
        this.factory = pi -> {
            var doc = DataManager.getInstance().getSearchIndex().getDocumentByPI(pi);
            if (doc == null) {
                return null;
            }
            return new VariableReplacer(new StructElement(doc));
        };
    }

    ExternalResourceUrlService(VariableReplacerFactory factory, List<String> urlTemplates) {
        this.factory = factory;
        this.urlTemplates = urlTemplates;
    }

    /**
     * Expands all configured URL templates using the given {@code VariableReplacer}. No Solr lookup or caching is performed; use this when the caller
     * already holds record metadata.
     *
     * @should expand templates using provided variable replacer
     */
    public Map<String, String> getAllowedUrls(VariableReplacer vr) {
        if (urlTemplates.isEmpty() || vr == null) {
            return Collections.emptyMap();
        }
        return urlTemplates.stream()
                .flatMap(templ -> vr.replace(templ).stream().map(url -> Map.entry(url, templ)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
    }

    /**
     * Returns a map of expanded URL → URL template for all configured templates applied to the record identified by {@code pi}. Caches results per PI
     * to avoid repeated Solr queries. Returns an empty map if the record is unknown or no templates are configured.
     *
     * @should return empty map if pi not found
     * @should return empty map if no templates configured
     * @should return expanded url to template mapping for known pi
     * @should call factory only once per pi across multiple calls
     */
    public Map<String, String> getAllowedUrls(String pi) throws PresentationException, IndexUnreachableException {
        if (urlTemplates.isEmpty()) {
            return Collections.emptyMap();
        }
        if (cache.containsKey(pi)) {
            return cache.get(pi);
        }
        VariableReplacer vr = factory.create(pi);
        if (vr == null) {
            return Collections.emptyMap();
        }
        Map<String, String> urls = getAllowedUrls(vr);
        cache.put(pi, urls);
        return urls;
    }

    /**
     * Returns existence-filtered URLs for the record represented by {@code vr}. Only URLs where the remote resource is reachable are included. The
     * result is cached after the first call.
     *
     * @should filter urls by existence check
     * @should cache existence check result across multiple calls
     */
    public Map<String, String> getExistingUrls(VariableReplacer vr) {
        if (existingUrls == null) {
            existingUrls = getAllowedUrls(vr).entrySet()
                    .stream()
                    .filter(e -> existenceChecker.test(e.getKey(), e.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return existingUrls;
    }

    void setExistenceChecker(BiPredicate<String, String> existenceChecker) {
        this.existenceChecker = existenceChecker;
    }
}
