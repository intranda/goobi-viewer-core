/**
 * This file is part of the Goobi viewer Connector - OAI-PMH and SRU interfaces for digital objects.
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
package io.goobi.viewer.connector.oai.enums;

/**
 * <p>
 * Metadata class.
 * </p>
 *
 */
public enum Metadata {
    METS("mets", "http://www.loc.gov/mets/mets.xsd", "mets", "http://www.loc.gov/METS/", true, true),
    MODS("mods", "http://www.loc.gov/standards/mods/v3/mods-3-3.xsd", "mods", "http://www.loc.gov/mods/v3", false, true),
    SOLR("solr", "TODO", "TODO", "TODO", false, true),
    MARCXML("marcxml", "http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd", "marcxml", "http://www.loc.gov/MARC21/slim", true, true),
    OAI_DC("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc.xsd", "oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/", true, false),
    DC("dc", "http://www.openarchives.org/OAI/2.0/oai_dc.xsd", "dc", "http://purl.org/dc/elements/1.1/", false, true),
    EPICUR("epicur", "http://nbn-resolving.de/urn/resolver.pl?urn=urn:nbn:de:1111-2004033116", "epicur", "urn:nbn:de:1111-2004033116", true, false),
    LIDO("lido", "http://www.lido-schema.org/schema/v1.0/lido-v1.0.xsd", "lido", "http://www.lido-schema.org", true, true),
    ESE("europeana", "http://www.europeana.eu/schemas/ese/ESE-V3.4.xsd", "europeana", "http://www.europeana.eu/schemas/ese/", true, false),
    IV_OVERVIEWPAGE("iv_overviewpage", "http://www.intranda.com/intrandaviewer_overviewpage.xsd", "iv_overviewpage",
            "http://www.intranda.com/digiverso/intrandaviewer/overviewpage", true, false),
    IV_CROWDSOURCING("iv_crowdsourcing", "http://www.intranda.com/intrandaviewer_overviewpage.xsd", "iv_crowdsourcing",
            "http://www.intranda.com/digiverso/intrandaviewer/crowdsourcing", true, false),
    TEI("tei", "https://www.tei-c.org/release/xml/tei/custom/schema/xsd/tei_all.xsd", "tei", "http://www.tei-c.org/ns/1.0", true, false),
    CMDI("cmdi",
            "http://www.clarin.eu/cmd/1 https://infra.clarin.eu/CMDI/1.x/xsd/cmd-envelop.xsd http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1380106710826 https://catalog.clarin.eu/ds/ComponentRegistry/rest/registry/1.x/profiles/clarin.eu:cr1:p_1380106710826/xsd",
            "cmd", "http://www.clarin.eu/cmd/1", true, false);

    /** OAI metadataPrefix parameter value */
    private final String metadataPrefix;
    private final String schema;
    private final String metadataNamespacePrefix;
    private final String metadataNamespaceUri;
    private final boolean oaiSet;
    private final boolean sruSet;

    /**
     * Constructor.
     * 
     * @param metadataPrefix
     * @param schema
     * @param metadataNamespacePrefix
     * @param metadataNamespaceUri
     * @param oaiSet Use format for SRU
     * @param sruSet Use format for OAI-PMH
     */
    private Metadata(String metadataPrefix, String schema, String metadataNamespacePrefix, String metadataNamespaceUri, boolean oaiSet,
            boolean sruSet) {
        this.metadataPrefix = metadataPrefix;
        this.schema = schema;
        this.metadataNamespacePrefix = metadataNamespacePrefix;
        this.metadataNamespaceUri = metadataNamespaceUri;
        this.oaiSet = oaiSet;
        this.sruSet = sruSet;
    }

    /**
     * <p>
     * Getter for the field <code>metadataPrefix</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMetadataPrefix() {
        return metadataPrefix;
    }

    /**
     * <p>
     * Getter for the field <code>schema</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSchema() {
        return schema;
    }

    /**
     * <p>
     * Getter for the field <code>metadataNamespacePrefix</code>.
     * </p>
     *
     * @return the metadataNamespacePrefix
     */
    public String getMetadataNamespacePrefix() {
        return metadataNamespacePrefix;
    }

    /**
     * <p>
     * Getter for the field <code>metadataNamespaceUri</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMetadataNamespaceUri() {
        return metadataNamespaceUri;
    }

    /**
     * <p>
     * isSruSet.
     * </p>
     *
     * @return the sruSet
     */
    public boolean isSruSet() {
        return sruSet;
    }

    /**
     * <p>
     * isOaiSet.
     * </p>
     *
     * @return the oaiSet
     */
    public boolean isOaiSet() {
        return oaiSet;
    }

    /**
     * <p>
     * getByMetadataPrefix.
     * </p>
     *
     * @param metadataPrefix a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.connector.oai.enums.Metadata} object.
     */
    public static Metadata getByMetadataPrefix(String metadataPrefix) {
        for (Metadata m : Metadata.values()) {
            if (m.getMetadataPrefix().equals(metadataPrefix)) {
                return m;
            }
        }
        return null;
    }
}
