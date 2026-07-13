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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Describes one configurable search export format as defined in {@code config_viewer.xml} under
 * {@code <export><format>}.
 *
 * <p>Two kinds of format are supported, distinguished by the presence of the {@code xslt} attribute:
 * <ul>
 *   <li><b>XSLT-based</b> (e.g. ris/endnote/bibtex): carries {@code xslt}, {@code contentType} and
 *       {@code fileExtension} attributes; the export is produced by applying the stylesheet to the
 *       Solr result XML.</li>
 *   <li><b>Java field-mapped</b> (e.g. excel/csv): carries a list of {@code <field>} child elements
 *       naming the Solr fields to export as columns; the export is produced by a built-in Java
 *       handler keyed by the format {@code name}.</li>
 * </ul>
 *
 * <p>Example configuration:
 * <pre>{@code
 * <format name="bibtex" enabled="true" xslt="solr2bibtex.xsl"
 *         contentType="text/plain" fileExtension="bib" />
 * <format name="csv" enabled="true">
 *     <field>PI_TOPSTRUCT</field>
 *     <field>MD_TITLE</field>
 * </format>
 * }</pre>
 */
public class ExportFormat {

    /** HTTP content type for XLSX (Excel) downloads. */
    public static final String CONTENT_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    /** HTTP content type for CSV downloads. */
    public static final String CONTENT_TYPE_CSV = "text/csv";

    private final String name;
    private final boolean enabled;
    private final String xslt;
    private final String contentType;
    private final String fileExtension;
    private final List<ExportFieldConfiguration> fields;

    /**
     * Creates a new export format descriptor. For Java field-mapped formats (no {@code xslt}) the
     * {@code contentType} and {@code fileExtension} are derived from the format {@code name} when not
     * explicitly configured.
     *
     * @param name unique format identifier used in the REST path (e.g. "bibtex", "csv")
     * @param enabled whether this format is currently active
     * @param xslt file name of the XSLT stylesheet (e.g. "solr2bibtex.xsl"), or blank for Java formats
     * @param contentType HTTP content type for the response; may be blank to use a name-based default
     * @param fileExtension file extension for the download file; may be blank to use a name-based default
     * @param fields Solr field columns for Java field-mapped formats (may be null/empty for XSLT formats)
     */
    public ExportFormat(String name, boolean enabled, String xslt, String contentType, String fileExtension,
            List<ExportFieldConfiguration> fields) {
        this.name = name;
        this.enabled = enabled;
        this.xslt = xslt;
        this.fields = (fields != null) ? fields : new ArrayList<>();

        boolean xsltBased = StringUtils.isNotBlank(xslt);
        this.contentType = StringUtils.isNotBlank(contentType) ? contentType : defaultContentType(name, xsltBased);
        this.fileExtension = StringUtils.isNotBlank(fileExtension) ? fileExtension : defaultFileExtension(name, xsltBased);
    }

    private static String defaultContentType(String name, boolean xsltBased) {
        if (!xsltBased) {
            if ("excel".equals(name)) {
                return CONTENT_TYPE_XLSX;
            }
            if ("csv".equals(name)) {
                return CONTENT_TYPE_CSV;
            }
        }
        return "text/plain";
    }

    private static String defaultFileExtension(String name, boolean xsltBased) {
        if (!xsltBased) {
            if ("excel".equals(name)) {
                return "xlsx";
            }
            if ("csv".equals(name)) {
                return "csv";
            }
        }
        return "txt";
    }

    /**
     * @return true if this format is produced by an XSLT stylesheet, false if it is a Java field-mapped format
     */
    public boolean isXsltBased() {
        return StringUtils.isNotBlank(xslt);
    }

    /**
     * @return the configured Solr field columns for Java field-mapped formats; empty for XSLT formats
     */
    public List<ExportFieldConfiguration> getFields() {
        return Collections.unmodifiableList(fields);
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
