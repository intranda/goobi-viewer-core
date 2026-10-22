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
package io.goobi.viewer.model.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import io.goobi.viewer.solr.SolrConstants.DocType;

/**
 * Privileges required for accessing a content file, optionally depending on the {@link DocType} of the Solr document that lists the file. This
 * allows a single access check query to decide whether a file is e.g. a page image ({@link DocType#PAGE}) or a born-digital download
 * ({@link DocType#DOWNLOAD_RESOURCE}) without knowing the document type beforehand. All privileges that apply to a document type must be granted.
 * Instances are immutable.
 */
public final class RequiredPrivileges {

    private final String[] defaultPrivileges;
    private final Map<DocType, String[]> docTypePrivileges;

    private RequiredPrivileges(String[] defaultPrivileges, Map<DocType, String[]> docTypePrivileges) {
        this.defaultPrivileges = defaultPrivileges;
        this.docTypePrivileges = docTypePrivileges;
    }

    /**
     * @param privileges privileges required for any document type that has no specific mapping
     * @return new {@link RequiredPrivileges}
     */
    public static RequiredPrivileges of(String... privileges) {
        return new RequiredPrivileges(privileges.clone(), Collections.emptyMap());
    }

    /**
     * @param docType document type for which other privileges are required
     * @param privileges privileges required for files listed in documents of the given type
     * @return new {@link RequiredPrivileges} containing the additional mapping; this instance remains unchanged
     * @should not modify original instance
     */
    public RequiredPrivileges withDocType(DocType docType, String... privileges) {
        Map<DocType, String[]> map = new EnumMap<>(DocType.class);
        map.putAll(docTypePrivileges);
        map.put(docType, privileges.clone());
        return new RequiredPrivileges(defaultPrivileges, Collections.unmodifiableMap(map));
    }

    /**
     * @param docType document type of the Solr document listing the file; may be null
     * @return privileges mapped to docType, or the default privileges if there is no mapping
     * @should return default privileges for unmapped doc type
     * @should return mapped privileges for mapped doc type
     */
    public String[] getPrivileges(DocType docType) {
        String[] ret = docType != null ? docTypePrivileges.get(docType) : null;
        return (ret != null ? ret : defaultPrivileges).clone();
    }

    /**
     * @param privilege privilege name to look for
     * @return true if privilege is required for any document type; false otherwise
     * @should find privilege in defaults and mappings
     */
    public boolean contains(String privilege) {
        return Arrays.asList(defaultPrivileges).contains(privilege)
                || docTypePrivileges.values().stream().anyMatch(p -> Arrays.asList(p).contains(privilege));
    }

    /**
     * Key identifying this privilege combination in cached permission session attributes. For a single privilege without document type
     * mappings, this is the privilege name itself so that cache entries are shared with the single-privilege access check methods.
     *
     * @return Cache key
     * @should equal privilege name for single privilege without mapping
     * @should differ for different mappings
     */
    public String getCacheKey() {
        StringBuilder sb = new StringBuilder(String.join("+", defaultPrivileges));
        for (Entry<DocType, String[]> entry : docTypePrivileges.entrySet()) {
            sb.append('|').append(entry.getKey().name()).append(':').append(String.join("+", entry.getValue()));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getCacheKey();
    }
}
