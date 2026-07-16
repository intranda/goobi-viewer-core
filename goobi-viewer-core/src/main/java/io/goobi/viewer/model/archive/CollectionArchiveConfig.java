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
package io.goobi.viewer.model.archive;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The resolved set of {@link ArchiveContentType content types} that a per-collection BagIt archive should contain, as configured in
 * {@code config_viewer.xml} under {@code <collectionArchives>}.
 *
 * <p>
 * A {@code <default field="…">} block defines the baseline for a Solr collection field; a {@code <collection name="…" field="…">} block
 * defines a <b>sparse override</b> — only the content-type toggles it lists change the effective set, everything else is inherited from the
 * default. The merge of override onto default is performed while reading the configuration (see
 * {@link io.goobi.viewer.controller.Configuration#getCollectionArchiveConfig(String, String)}); this class simply holds the resolved,
 * effective set of enabled content types.
 *
 * <p>
 * Instances are immutable.
 */
public class CollectionArchiveConfig {

    private final String field;
    private final String collectionName;
    private final Set<ArchiveContentType> enabledTypes;

    /**
     * @param field the Solr collection field this configuration applies to (e.g. {@code DC})
     * @param collectionName the collection name for a per-collection override, or {@code null} for the {@code <default>} block
     * @param enabledTypes the content types explicitly enabled in this block (never null)
     */
    public CollectionArchiveConfig(String field, String collectionName, Set<ArchiveContentType> enabledTypes) {
        this.field = field;
        this.collectionName = collectionName;
        this.enabledTypes = (enabledTypes == null || enabledTypes.isEmpty()) ? EnumSet.noneOf(ArchiveContentType.class)
                : EnumSet.copyOf(enabledTypes);
    }

    /**
     * @return the Solr collection field (e.g. {@code DC})
     */
    public String getField() {
        return field;
    }

    /**
     * @return the collection name for a per-collection override, or {@code null} for the default block
     */
    public String getCollectionName() {
        return collectionName;
    }

    /**
     * @return the effective set of enabled content types (unmodifiable, never null)
     */
    public Set<ArchiveContentType> getEnabledTypes() {
        return Collections.unmodifiableSet(enabledTypes);
    }

    /**
     * @param type the content type to check
     * @return true if the given content type is enabled in this configuration
     */
    public boolean isEnabled(ArchiveContentType type) {
        return enabledTypes.contains(type);
    }

    /**
     * @return true if at least one content type is enabled, i.e. an archive would actually contain payload
     */
    public boolean hasEnabledTypes() {
        return !enabledTypes.isEmpty();
    }

    @Override
    public String toString() {
        return "CollectionArchiveConfig[field=" + field + ", collectionName=" + collectionName + ", enabledTypes=" + enabledTypes + "]";
    }
}
