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
