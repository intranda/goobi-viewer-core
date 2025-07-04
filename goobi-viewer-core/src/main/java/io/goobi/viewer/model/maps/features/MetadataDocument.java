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
package io.goobi.viewer.model.maps.features;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.apache.solr.common.SolrDocument;

import io.goobi.viewer.model.metadata.ComplexMetadataContainer;
import io.goobi.viewer.model.metadata.MetadataContainer;
import io.goobi.viewer.solr.SolrConstants;

public class MetadataDocument {

    private final String pi;
    private final String iddoc;
    /**
     * Metadata directly in SOLR document
     */
    private final MetadataContainer metadata;
    /**
     * Metadata in METADATA documents
     */
    private final ComplexMetadataContainer metadataGroups;
    /**
     * Child docstructs
     */
    private final Collection<MetadataDocument> children;

    public static MetadataDocument fromSolrDocs(SolrDocument mainDoc, Collection<SolrDocument> childDocs, Collection<SolrDocument> metadataDocs) {
        return new MetadataDocument(
                mainDoc,
                getMetadataDocumentsForIddoc(getIddoc(mainDoc), metadataDocs),
                childDocs.stream()
                        .map(cDoc -> new MetadataDocument(cDoc, getMetadataDocumentsForIddoc(getIddoc(cDoc), metadataDocs), Collections.emptyList()))
                        .toList());
    }

    public MetadataDocument(String pi, String iddoc, MetadataContainer metadata, ComplexMetadataContainer metadataGroups,
            Collection<MetadataDocument> children) {
        super();
        this.pi = pi;
        this.iddoc = iddoc;
        this.metadata = metadata;
        this.metadataGroups = metadataGroups;
        this.children = children;
    }

    private MetadataDocument(SolrDocument doc, Collection<SolrDocument> metadataDocs, Collection<MetadataDocument> children) {
        this.pi = getPi(doc);
        this.iddoc = getIddoc(doc);
        this.metadata = MetadataContainer.createMetadataEntity(doc);
        this.metadataGroups = new ComplexMetadataContainer(metadataDocs);
        this.children = children;
    }

    private static String getIddoc(SolrDocument doc) {
        return Optional.ofNullable(doc.getFirstValue(SolrConstants.IDDOC)).map(Object::toString).orElse("");
    }

    private static String getIddocOwner(SolrDocument doc) {
        return Optional.ofNullable(doc.getFirstValue(SolrConstants.IDDOC_OWNER)).map(Object::toString).orElse("");
    }

    private static String getPi(SolrDocument doc) {
        return Optional.ofNullable(doc.getFirstValue(SolrConstants.PI)).map(Object::toString).orElse("");
    }

    private static Collection<SolrDocument> getMetadataDocumentsForIddoc(String iddoc, Collection<SolrDocument> metadataDocs) {
        return metadataDocs.stream().filter(mdDoc -> getIddocOwner(mdDoc).equals(iddoc)).toList();
    }

    public String getPi() {
        return pi;
    }

    public String getIddoc() {
        return iddoc;
    }

    public MetadataContainer getMainDocMetadata() {
        return metadata;
    }

    public ComplexMetadataContainer getMetadataGroups() {
        return metadataGroups;
    }

    public Collection<MetadataDocument> getChildDocuments() {
        return children;
    }

}
