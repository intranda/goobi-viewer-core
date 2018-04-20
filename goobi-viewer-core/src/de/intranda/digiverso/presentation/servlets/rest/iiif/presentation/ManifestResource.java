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
package de.intranda.digiverso.presentation.servlets.rest.iiif.presentation;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.iiif.presentation.Collection;
import de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement;
import de.intranda.digiverso.presentation.model.iiif.presentation.Manifest;
import de.intranda.digiverso.presentation.model.iiif.presentation.Range;
import de.intranda.digiverso.presentation.model.iiif.presentation.Sequence;
import de.intranda.digiverso.presentation.model.iiif.presentation.builder.ManifestBuilder;
import de.intranda.digiverso.presentation.model.iiif.presentation.builder.StructureBuilder;
import de.intranda.digiverso.presentation.model.viewer.StructElement;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;

/**
 * @author Florian Alpers
 *
 */
@Path("/manifests")
@ViewerRestServiceBinding
@IIIFPresentationBinding
public class ManifestResource extends AbstractResource {

    private static Logger logger = LoggerFactory.getLogger(ManifestResource.class);

    private ManifestBuilder manifestBuilder;
    private StructureBuilder structureBuilder;


    @GET
    @Path("/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    public IPresentationModelElement geManifest(@PathParam("pi") String pi)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ConfigurationException, DAOException {

        StructElement doc = getManifestBuilder().getDocument(pi);
        servletResponse.addHeader("Access-Control-Allow-Origin", "*");

        IPresentationModelElement manifest = getManifestBuilder().generateManifest(doc);

        if (manifest instanceof Collection) {
            getManifestBuilder().addVolumes((Collection) manifest, doc.getLuceneId());
        } else if (manifest instanceof Manifest) {
            getManifestBuilder().addAnchor((Manifest) manifest, doc);
            getManifestBuilder().addBaseSequence((Manifest) manifest, doc, manifest.getId().toString());
        }

        return manifest;

    }

    @GET
    @Path("/{pi}/sequence/basic")
    @Produces({ MediaType.APPLICATION_JSON })
    public Sequence getBasicSequence(@PathParam("pi") String pi) throws PresentationException, IndexUnreachableException, URISyntaxException,
            ConfigurationException, DAOException, IllegalRequestException {

        StructElement doc = getManifestBuilder().getDocument(pi);
        servletResponse.addHeader("Access-Control-Allow-Origin", "*");

        IPresentationModelElement manifest = getManifestBuilder().generateManifest(doc);

        if (manifest instanceof Collection) {
            //            addVolumes((Collection) manifest, doc.getLuceneId(), getBaseUrl());
            throw new IllegalRequestException("Identifier refers to a collection which does not have a sequence");
        } else if (manifest instanceof Manifest) {
            //            addAnchor((Manifest) manifest, doc, getBaseUrl());
            getManifestBuilder().addBaseSequence((Manifest) manifest, doc, manifest.getId().toString());
            return ((Manifest) manifest).getSequences().get(0);
        }
        throw new IllegalRequestException("Not manifest with identifier " + pi + " found");

    }
    
    @GET
    @Path("/{pi}/range/{logId}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Range geManifest(@PathParam("pi") String pi, @PathParam("logId") String logId)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ConfigurationException, DAOException {
        
        StructElement doc = getStructureBuilder().getDocument(pi, logId);
        
        if(doc == null) {
            throw new NotFoundException("Not document with PI = " + pi + " and logId = " + logId + " found");
        } else {
            Range topRange = getStructureBuilder().generateStructure(doc, getStructureBuilder().getRangeUri(pi, logId));
            return topRange;
        }
    }
    
    private StructureBuilder getStructureBuilder() {
        if(this.structureBuilder == null) {
            try {
                this.structureBuilder = new StructureBuilder(this.servletRequest);
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }
        return this.structureBuilder;
    }

    /**
     * @return the manifestBuilder
     */
    public ManifestBuilder getManifestBuilder() {
        if (this.manifestBuilder == null) {
            try {
                this.manifestBuilder = new ManifestBuilder(servletRequest);
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }
        return manifestBuilder;
    }

    /**
     * @param baseUrl
     * @param pi
     * @return
     * @throws URISyntaxException
     */
    public static URI getManifestUrl(String baseUrl, String pi) throws URISyntaxException {
        return new URI(baseUrl + "/" + pi);
    }

    protected String getPath() {
        return "/manifests";
    }

}
