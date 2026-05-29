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
package io.goobi.viewer.model.export;

/**
 * Describes one configurable XSLT-based search export format as defined in
 * {@code config_viewer.xml} under {@code <search><export><format>}.
 *
 * <p>Each instance holds the format's unique name, enable flag, XSLT file name,
 * HTTP content type and file extension. New export formats can be added at runtime
 * by simply adding another {@code <format>} element and dropping the corresponding
 * XSLT stylesheet into the config or classpath directory.
 *
 * <p>Example configuration:
 * <pre>{@code
 * <format name="bibtex" enabled="true" xslt="solr2bibtex.xsl"
 *         contentType="text/plain" fileExtension="bib" />
 * }</pre>
 */
public class ExportFormat {

    private final String name;
    private final boolean enabled;
    private final String xslt;
    private final String contentType;
    private final String fileExtension;

    /**
     * Creates a new export format descriptor.
     *
     * @param name unique format identifier used in the REST path (e.g. "bibtex")
     * @param enabled whether this format is currently active
     * @param xslt file name of the XSLT stylesheet (e.g. "solr2bibtex.xsl")
     * @param contentType HTTP content type for the response (e.g. "text/plain")
     * @param fileExtension file extension for the download file (e.g. "bib")
     */
    public ExportFormat(String name, boolean enabled, String xslt, String contentType, String fileExtension) {
        this.name = name;
        this.enabled = enabled;
        this.xslt = xslt;
        this.contentType = contentType;
        this.fileExtension = fileExtension;
    }

    /**
     * @return the unique format identifier (e.g. "bibtex", "endnote", "ris")
     */
    public String getName() {
        return name;
    }

    /**
     * @return true if this export format is enabled in the configuration
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return the XSLT stylesheet file name used for the transformation
     */
    public String getXslt() {
        return xslt;
    }

    /**
     * @return the HTTP content type to set on the response (e.g. "application/xml")
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @return the file extension for the downloaded file (e.g. "xml", "bib", "ris")
     */
    public String getFileExtension() {
        return fileExtension;
    }

    @Override
    public String toString() {
        return "ExportFormat[name=" + name + ", enabled=" + enabled + ", xslt=" + xslt + "]";
    }
}
