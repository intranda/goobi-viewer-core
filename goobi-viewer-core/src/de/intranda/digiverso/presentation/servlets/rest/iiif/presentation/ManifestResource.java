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
import java.sql.Date;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.imaging.IIIFUrlHandler;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.iiif.presentation.AbstractPresentationModelElement;
import de.intranda.digiverso.presentation.model.iiif.presentation.Collection;
import de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement;
import de.intranda.digiverso.presentation.model.iiif.presentation.Manifest;
import de.intranda.digiverso.presentation.model.iiif.presentation.content.ImageContent;
import de.intranda.digiverso.presentation.model.iiif.presentation.content.LinkingContent;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.Format;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.Metadata;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.SimpleMetadataValue;
import de.intranda.digiverso.presentation.model.viewer.StructElement;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.model.iiif.ImageInformation;

/**
 * @author Florian Alpers
 *
 */
@Path("/manifests")
@ViewerRestServiceBinding
@IIIFPresentationBinding
public class ManifestResource extends AbstractResource {

    private static Logger logger = LoggerFactory.getLogger(ManifestResource.class);

    private static final String[] REQUIRED_SOLR_FIELDS = { SolrConstants.IDDOC, SolrConstants.PI, SolrConstants.TITLE, SolrConstants.PI_TOPSTRUCT,
            SolrConstants.MIMETYPE, SolrConstants.THUMBNAIL, SolrConstants.DOCSTRCT, SolrConstants.DOCTYPE, SolrConstants.METADATATYPE,
            SolrConstants.FILENAME, SolrConstants.FILENAME_HTML_SANDBOXED, SolrConstants.PI_PARENT };
    private static final List<String> HIDDEN_SOLR_FIELDS = Arrays.asList(new String[] { SolrConstants.IDDOC, SolrConstants.PI,
            SolrConstants.PI_TOPSTRUCT, SolrConstants.MIMETYPE, SolrConstants.THUMBNAIL, SolrConstants.DOCTYPE, SolrConstants.METADATATYPE,
            SolrConstants.FILENAME, SolrConstants.FILENAME_HTML_SANDBOXED, SolrConstants.PI_PARENT });

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    @GET
    @Path("/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    public IPresentationModelElement geManifest(@PathParam("pi") String pi)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ConfigurationException, DAOException {

        StructElement doc = getDocument(pi);
        servletResponse.addHeader("Access-Control-Allow-Origin", "*");
//
//        if (doc.isAnchor()) {
//            Collection anchor = generateAnchorCollection(doc, getBaseUrl());
//            return anchor;
//        } else {
//            Manifest manifest = generateManifest(doc, getBaseUrl());
//            return manifest;
//        }
        
        IPresentationModelElement manifest = generateManifest(doc, getBaseUrl());
        return manifest;

    }


    /**
     * @param pi
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public StructElement getDocument(String pi) throws PresentationException, IndexUnreachableException {
        String query = "PI:" + pi;
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, getSolrFieldList());
        StructElement ele = new StructElement(Long.parseLong(doc.getFieldValue(SolrConstants.IDDOC).toString()), doc);
        ele.setImageNumber(1);
        return ele;
    }

    /**
     * @param pi
     * @param baseUrl
     * @return
     * @throws URISyntaxException
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     * @throws ConfigurationException
     */
    private IPresentationModelElement generateManifest(StructElement ele, String baseUrl)
            throws URISyntaxException, PresentationException, IndexUnreachableException, ConfigurationException, DAOException {

        AbstractPresentationModelElement manifest;
        
        if(ele.isAnchor()) {
            manifest = new Collection(getManifestUrl(baseUrl, ele.getPi()));
        } else {            
           manifest = new Manifest(getManifestUrl(baseUrl, ele.getPi()));
        }

        manifest.setAttribution(new SimpleMetadataValue(ATTRIBUTION));
        manifest.setLabel(new SimpleMetadataValue(ele.getLabel()));

        addMetadata(manifest, ele);

        try {
            ImageContent thumb = new ImageContent(new URI(BeanUtils.getImageDeliveryBean().getThumb().getThumbnailUrl(ele)), true);
            manifest.setThumbnail(thumb);
        } catch (URISyntaxException e) {
            logger.warn("Unable to retrieve thumbnail url", e);
        }

        Optional<String> logoUrl =
                BeanUtils.getImageDeliveryBean().getFooter().getWatermarkUrl(Optional.empty(), Optional.ofNullable(ele), Optional.empty());
        logoUrl.ifPresent(url -> {
            try {
                ImageContent logo = new ImageContent(new URI(url), false);
                manifest.setLogo(logo);
            } catch (URISyntaxException e) {
                logger.warn("Unable to retrieve logo url", e);
            }
        });

        String navDateField = DataManager.getInstance().getConfiguration().getIIIFNavDateField();
        if (StringUtils.isNotBlank(navDateField) && StringUtils.isNotBlank(ele.getMetadataValue(navDateField))) {
            try {
                manifest.setNavDate(Date.from(Instant.parse(ele.getMetadataValue(navDateField))));
            } catch (NullPointerException | DateTimeParseException e) {
                logger.warn("Unable to parse {} as Date", ele.getMetadataValue(navDateField));
            }
        }

        try {
            LinkingContent metsResolver = new LinkingContent(new URI(getMetsResolverUrl(ele)));
            metsResolver.setFormat(Format.TEXT_XML);
            metsResolver.setLabel(new SimpleMetadataValue("METS/MODS"));
            manifest.addSeeAlso(metsResolver);
        } catch (URISyntaxException e) {
            logger.error("Unable to retrieve mets resolver url for {}", ele);
        }

        try {
            LinkingContent viewerPage = new LinkingContent(new URI(getServletURI() + ele.getUrl()));
            viewerPage.setLabel(new SimpleMetadataValue("goobi viewer"));
            manifest.addRendering(viewerPage);
        } catch (URISyntaxException e) {
            logger.error("Unable to retrieve viewer url for {}", ele);
        }

        try {
            String pdfDownloadUrl = BeanUtils.getImageDeliveryBean().getPdf().getPdfUrl(ele, manifest.getLabel().getValue().orElse(null));
            LinkingContent pdfDownload = new LinkingContent(new URI(pdfDownloadUrl));
            pdfDownload.setFormat(Format.APPLICATION_PDF);
            pdfDownload.setLabel(new SimpleMetadataValue("PDF"));
            manifest.addRendering(pdfDownload);
        } catch (URISyntaxException e) {
            logger.error("Unable to retrieve pdf download url for {}", ele);
        }

        String anchorPI = ele.getAncestors().get(SolrConstants.PI_PARENT);
        if (StringUtils.isNotBlank(anchorPI)) {
            StructElement anchor = getDocument(anchorPI);
            IPresentationModelElement anchorCollection = generateManifest(anchor, baseUrl);
            manifest.addWithin(anchorCollection);
        }

        return manifest;
    }

    /**
     * @return
     */
    private List<String> getSolrFieldList() {
        List<String> fields = DataManager.getInstance().getConfiguration().getIIIFMetadataFields();
        for (String string : REQUIRED_SOLR_FIELDS) {
            if (!fields.contains(string)) {
                fields.add(string);
            }
        }
        String navDateField = DataManager.getInstance().getConfiguration().getIIIFNavDateField();
        if (StringUtils.isNotBlank(navDateField) && !fields.contains(navDateField)) {
            fields.add(navDateField);
        }
        return fields;
    }

    /**
     * @param manifest
     * @param ele
     */
    public void addMetadata(AbstractPresentationModelElement manifest, StructElement ele) {
        for (String field : ele.getMetadataFields().keySet()) {
            if (!HIDDEN_SOLR_FIELDS.contains(field) && !field.endsWith("_UNTOKENIZED")) {
                Optional<IMetadataValue> mdValue =
                        ele.getMetadataValues(field).stream().reduce((s1, s2) -> s1 + "; " + s2).map(value -> IMetadataValue.getTranslations(value));
                mdValue.ifPresent(value -> {
                    manifest.addMetadata(new Metadata(IMetadataValue.getTranslations(field), value));
                });
            }
        }
    }

    /**
     * @param baseUrl
     * @param pi
     * @return
     * @throws URISyntaxException
     */
    private URI getManifestUrl(String baseUrl, String pi) throws URISyntaxException {
        return new URI(baseUrl + "/manifests/" + pi + "/");
    }

    protected String getPath() {
        return "/manifests";
    }
    
    

}
