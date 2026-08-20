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
package io.goobi.viewer.model.export.bagit;

/**
 * The kinds of content that can be included in a per-collection BagIt archive. Each content type maps to a boolean toggle in
 * {@code config_viewer.xml} (under {@code <collectionArchives><default>} or a per-collection override) and to a subfolder under the bag's
 * {@code data/} payload directory.
 *
 * <p>
 * Multiple export flavours (CSV/RIS/BibTeX) share the same {@code data/export/} subfolder but are distinguished by their file names, so the
 * subfolder value is not necessarily unique across constants.
 */
public enum ArchiveContentType {

    /** Record source files (METS/LIDO/DenkXweb XML) as stored during ingest. */
    METADATA_SOURCE("metadataSourceFormats", "metadata"),
    /** ALTO OCR files. */
    FULLTEXT_ALTO("fulltextAlto", "alto"),
    /** Plain-text fulltext files. */
    FULLTEXT_TEXT("fulltextText", "fulltext"),
    /** TEI fulltext files. */
    FULLTEXT_TEI("fulltextTei", "tei"),
    /** Media files (images/audio/video). */
    IMAGES("images", "media"),
    /** Collection metadata exported as CSV. */
    EXPORT_CSV("metadataExportCsv", "export"),
    /** Collection metadata exported as RIS. */
    EXPORT_RIS("metadataExportRis", "export"),
    /** Collection metadata exported as BibTeX. */
    EXPORT_BIBTEX("metadataExportBibtex", "export");

    private final String configKey;
    private final String dataSubfolder;

    ArchiveContentType(String configKey, String dataSubfolder) {
        this.configKey = configKey;
        this.dataSubfolder = dataSubfolder;
    }

    /**
     * @return the {@code config_viewer.xml} element name that toggles this content type (e.g. {@code fulltextAlto})
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * @return the subfolder name below the bag's {@code data/} directory where this content type is placed (e.g. {@code alto})
     */
    public String getDataSubfolder() {
        return dataSubfolder;
    }

    /**
     * Looks up the content type whose {@link #getConfigKey() config key} matches the given element name.
     *
     * @param configKey the {@code config_viewer.xml} element name
     * @return the matching content type, or {@code null} if none matches
     */
    public static ArchiveContentType fromConfigKey(String configKey) {
        for (ArchiveContentType type : values()) {
            if (type.configKey.equals(configKey)) {
                return type;
            }
        }
        return null;
    }
}
