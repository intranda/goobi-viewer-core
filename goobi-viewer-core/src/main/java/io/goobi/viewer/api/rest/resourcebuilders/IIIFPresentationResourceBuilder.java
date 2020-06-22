/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
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
package io.goobi.viewer.api.rest.resourcebuilders;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.PathParam;

import org.apache.commons.lang3.StringUtils;

import de.intranda.api.annotation.oa.Motivation;
import de.intranda.api.iiif.presentation.AnnotationList;
import de.intranda.api.iiif.presentation.Collection;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.presentation.Layer;
import de.intranda.api.iiif.presentation.Manifest;
import de.intranda.api.iiif.presentation.Range;
import de.intranda.api.iiif.presentation.enums.AnnotationType;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.iiif.presentation.builder.BuildMode;
import io.goobi.viewer.model.iiif.presentation.builder.LayerBuilder;
import io.goobi.viewer.model.iiif.presentation.builder.ManifestBuilder;
import io.goobi.viewer.model.iiif.presentation.builder.SequenceBuilder;
import io.goobi.viewer.model.iiif.presentation.builder.StructureBuilder;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.servlets.rest.content.ContentResource;

/**
 * @author florian
 *
 */
public class IIIFPresentationResourceBuilder {

    private ManifestBuilder manifestBuilder;
    private StructureBuilder structureBuilder;
    private SequenceBuilder sequenceBuilder;
    private LayerBuilder layerBuilder;
    private final AbstractApiUrlManager urls;

    public IIIFPresentationResourceBuilder(AbstractApiUrlManager urls) {
        this.urls = urls;
    }

    public IPresentationModelElement getManifest(String pi, BuildMode mode) throws PresentationException, IndexUnreachableException,
            ContentNotFoundException, URISyntaxException, ViewerConfigurationException, DAOException {
        getManifestBuilder().setBuildMode(mode);
        getSequenceBuilder().setBuildMode(mode);
        List<StructElement> docs = getManifestBuilder().getDocumentWithChildren(pi);
        if (docs.isEmpty()) {
            throw new ContentNotFoundException("No document found for pi " + pi);
        }
        StructElement mainDoc = docs.get(0);
        IPresentationModelElement manifest = getManifestBuilder().generateManifest(mainDoc);

        if (manifest instanceof Collection && docs.size() > 1) {
            getManifestBuilder().addVolumes((Collection) manifest, docs.subList(1, docs.size()));
        } else if (manifest instanceof Manifest) {
            getManifestBuilder().addAnchor((Manifest) manifest, mainDoc.getMetadataValue(SolrConstants.PI_ANCHOR));

            getSequenceBuilder().addBaseSequence((Manifest) manifest, mainDoc, manifest.getId().toString());

            String topLogId = mainDoc.getMetadataValue(SolrConstants.LOGID);
            if (StringUtils.isNotBlank(topLogId)) {
                List<Range> ranges = getStructureBuilder().generateStructure(docs, pi, false);
                ranges.forEach(range -> {
                    ((Manifest) manifest).addStructure(range);
                });
            }
        }

        return manifest;
    }

    public Layer getLayer(String pi, String typeName) throws PresentationException, IndexUnreachableException,
            URISyntaxException, ViewerConfigurationException, DAOException, ContentNotFoundException, IllegalRequestException, IOException {
        StructElement doc = getStructureBuilder().getDocument(pi);
        AnnotationType type = AnnotationType.valueOf(typeName.toUpperCase());
        if (type == null) {
            throw new IllegalRequestException("No valid annotation type: " + typeName);
        }
        if (doc == null) {
            throw new ContentNotFoundException("Not document with PI = " + pi + " found");
        } else if (AnnotationType.TEI.equals(type)) {
            return getLayerBuilder().createAnnotationLayer(pi, type, Motivation.PAINTING, (id, repo) -> ContentResource.getTEIFiles(id),
                    (id, lang) -> ContentResource.getTEIURI(id, lang));
        } else if (AnnotationType.CMDI.equals(type)) {
            return getLayerBuilder().createAnnotationLayer(pi, type, Motivation.DESCRIBING, (id, repo) -> ContentResource.getCMDIFiles(id),
                    (id, lang) -> ContentResource.getCMDIURI(id, lang));

        } else {
            Map<AnnotationType, List<AnnotationList>> annoLists = getSequenceBuilder().addBaseSequence(null, doc, "");
            Layer layer = getLayerBuilder().generateLayer(pi, annoLists, type);
            return layer;
        }
    }

    private StructureBuilder getStructureBuilder() {
        if (this.structureBuilder == null) {
            this.structureBuilder = new StructureBuilder(urls);
        }
        return this.structureBuilder;
    }

    /**
     * <p>
     * Getter for the field <code>manifestBuilder</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.iiif.presentation.builder.ManifestBuilder} object.
     */
    public ManifestBuilder getManifestBuilder() {
        if (this.manifestBuilder == null) {
            this.manifestBuilder = new ManifestBuilder(urls);
        }
        return manifestBuilder;
    }

    /**
     * <p>
     * Getter for the field <code>sequenceBuilder</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.iiif.presentation.builder.SequenceBuilder} object.
     */
    public SequenceBuilder getSequenceBuilder() {
        if (this.sequenceBuilder == null) {
            this.sequenceBuilder = new SequenceBuilder(urls);
        }
        return sequenceBuilder;
    }

    /**
     * <p>
     * Getter for the field <code>layerBuilder</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.iiif.presentation.builder.LayerBuilder} object.
     */
    public LayerBuilder getLayerBuilder() {
        if (this.layerBuilder == null) {
            this.layerBuilder = new LayerBuilder(urls);
        }
        return layerBuilder;
    }
}
