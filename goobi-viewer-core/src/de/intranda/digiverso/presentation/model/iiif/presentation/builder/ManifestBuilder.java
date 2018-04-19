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
package de.intranda.digiverso.presentation.model.iiif.presentation.builder;

import java.awt.Dimension;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Date;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.iiif.presentation.AbstractPresentationModelElement;
import de.intranda.digiverso.presentation.model.iiif.presentation.Canvas;
import de.intranda.digiverso.presentation.model.iiif.presentation.Collection;
import de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement;
import de.intranda.digiverso.presentation.model.iiif.presentation.Manifest;
import de.intranda.digiverso.presentation.model.iiif.presentation.Sequence;
import de.intranda.digiverso.presentation.model.iiif.presentation.annotation.Annotation;
import de.intranda.digiverso.presentation.model.iiif.presentation.content.ImageContent;
import de.intranda.digiverso.presentation.model.iiif.presentation.content.LinkingContent;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.Format;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.Motivation;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.ViewingHint;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.Metadata;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.SimpleMetadataValue;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;
import de.intranda.digiverso.presentation.model.viewer.StructElement;
import de.intranda.digiverso.presentation.model.viewer.pageloader.EagerPageLoader;
import de.intranda.digiverso.presentation.model.viewer.pageloader.IPageLoader;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.servlet.model.iiif.ImageInformation;

/**
 * @author Florian Alpers
 *
 */
public class ManifestBuilder extends AbstractBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ManifestBuilder.class);
    

    private static final String[] REQUIRED_SOLR_FIELDS = { SolrConstants.IDDOC, SolrConstants.PI, SolrConstants.TITLE, SolrConstants.PI_TOPSTRUCT,
            SolrConstants.MIMETYPE, SolrConstants.THUMBNAIL, SolrConstants.DOCSTRCT, SolrConstants.DOCTYPE, SolrConstants.METADATATYPE,
            SolrConstants.FILENAME, SolrConstants.FILENAME_HTML_SANDBOXED, SolrConstants.PI_PARENT, SolrConstants.LOGID, SolrConstants.ISWORK,
            SolrConstants.ISANCHOR, SolrConstants.NUMVOLUMES, SolrConstants.PI_PARENT, SolrConstants.CURRENTNO, SolrConstants.CURRENTNOSORT };
    private static final List<String> HIDDEN_SOLR_FIELDS = Arrays.asList(new String[] { SolrConstants.IDDOC, SolrConstants.PI,
            SolrConstants.PI_TOPSTRUCT, SolrConstants.MIMETYPE, SolrConstants.THUMBNAIL, SolrConstants.DOCTYPE, SolrConstants.METADATATYPE,
            SolrConstants.FILENAME, SolrConstants.FILENAME_HTML_SANDBOXED, SolrConstants.PI_PARENT, SolrConstants.LOGID, SolrConstants.ISWORK,
            SolrConstants.ISANCHOR, SolrConstants.NUMVOLUMES, SolrConstants.PI_PARENT, SolrConstants.CURRENTNOSORT });
    
    /**
     * @param request
     * @throws URISyntaxException
     */
    public ManifestBuilder(HttpServletRequest request) throws URISyntaxException {
        super(request);
    }

    /**
     * @param servletUri
     * @param requestURI
     */
    public ManifestBuilder(URI servletUri, URI requestURI) {
        super(servletUri, requestURI);
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
    public IPresentationModelElement generateManifest(StructElement ele)
            throws URISyntaxException, PresentationException, IndexUnreachableException, ConfigurationException, DAOException {

        final AbstractPresentationModelElement manifest;

        if (ele.isAnchor()) {
            manifest = new Collection(getManifestURI(ele.getPi()));
            manifest.setViewingHint(ViewingHint.multipart);
        } else {
            manifest = new Manifest(getManifestURI(ele.getPi()));
        }

        populate(ele, manifest);

        return manifest;
    }
    
    /**
     * @param ele
     * @param manifest
     * @throws ConfigurationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    public void populate(StructElement ele, final AbstractPresentationModelElement manifest)
            throws ConfigurationException, IndexUnreachableException, DAOException, PresentationException {
        manifest.setAttribution(new SimpleMetadataValue(ATTRIBUTION));
        manifest.setLabel(new SimpleMetadataValue(ele.getLabel()));

        addMetadata(manifest, ele);

        try {
            String thumbUrl = BeanUtils.getImageDeliveryBean().getThumb().getThumbnailUrl(ele);
            if (StringUtils.isNotBlank(thumbUrl)) {
                ImageContent thumb = new ImageContent(new URI(thumbUrl), true);
                manifest.setThumbnail(thumb);
            }
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

        /*METS/MODS*/
        try {
            LinkingContent metsResolver = new LinkingContent(new URI(getMetsResolverUrl(ele)));
            metsResolver.setFormat(Format.TEXT_XML);
            metsResolver.setLabel(new SimpleMetadataValue("METS/MODS"));
            manifest.addSeeAlso(metsResolver);
        } catch (URISyntaxException e) {
            logger.error("Unable to retrieve mets resolver url for {}", ele);
        }

        /*VIEWER*/
        try {
            LinkingContent viewerPage = new LinkingContent(new URI(getServletURI() + ele.getUrl()));
            viewerPage.setLabel(new SimpleMetadataValue("goobi viewer"));
            manifest.addRendering(viewerPage);
        } catch (URISyntaxException e) {
            logger.error("Unable to retrieve viewer url for {}", ele);
        }

        if (manifest instanceof Manifest) {
            /*PDF*/
            try {
                String pdfDownloadUrl = BeanUtils.getImageDeliveryBean().getPdf().getPdfUrl(ele, manifest.getLabel().getValue().orElse(null));
                LinkingContent pdfDownload = new LinkingContent(new URI(pdfDownloadUrl));
                pdfDownload.setFormat(Format.APPLICATION_PDF);
                pdfDownload.setLabel(new SimpleMetadataValue("PDF"));
                manifest.addRendering(pdfDownload);
            } catch (URISyntaxException e) {
                logger.error("Unable to retrieve pdf download url for {}", ele);
            }

        }
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
     * @param baseUrl2
     * @param manifest
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public void addVolumes(Collection anchor, long iddoc) throws PresentationException, IndexUnreachableException {
        List<StructElement> volumes = getChildDocs(iddoc).stream().sorted((v1, v2) -> getSortingNumber(v1).compareTo(getSortingNumber(v2))).collect(Collectors.toList());
        
        for (StructElement volume : volumes) {
            try {
                IPresentationModelElement child =  generateManifest(volume);
                if(child instanceof Manifest) {
//                    addBaseSequence((Manifest)child, volume, child.getId().toString());
                    anchor.addManifest((Manifest)child);
                }
            } catch (ConfigurationException | URISyntaxException | PresentationException | IndexUnreachableException | DAOException e) {
                logger.error("Error creating child manigest for " + volume);
            }
            
        }
    }
    

    /**
     * @param manifest
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     * @throws URISyntaxException
     * @throws ConfigurationException
     */
    public void addAnchor(Manifest manifest, StructElement ele)
            throws PresentationException, IndexUnreachableException, ConfigurationException, URISyntaxException, DAOException {

        /*ANCHOR*/
        String anchorPI = ele.getAncestors().get(SolrConstants.PI_PARENT);
        if (StringUtils.isNotBlank(anchorPI)) {
            StructElement anchor = getDocument(anchorPI);
            IPresentationModelElement anchorCollection = generateManifest(anchor);
            manifest.addWithin(anchorCollection);
        }

    }
    
    /**
     * @param manifest
     * @param doc
     * @param string
     * @throws URISyntaxException
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public void addBaseSequence(Manifest manifest, StructElement doc, String manifestId)
            throws URISyntaxException, PresentationException, IndexUnreachableException, DAOException {

        Sequence sequence = new Sequence(new URI(manifestId + "/sequence/basic"));

        IPageLoader pageLoader = new EagerPageLoader(doc);

        for (int i = pageLoader.getFirstPageOrder(); i <= pageLoader.getLastPageOrder(); ++i) {
            PhysicalElement page = pageLoader.getPage(i);

            URI canvasId = new URI(manifestId + "/canvas/" + i);
            Canvas canvas = new Canvas(canvasId);
            canvas.setLabel(new SimpleMetadataValue(page.getOrderLabel()));

            Dimension size = getSize(page);
            if (size.getWidth() * size.getHeight() > 0) {
                canvas.setWidth(size.width);
                canvas.setHeight(size.height);
            }

            if (page.getMimeType().toLowerCase().startsWith("image") && StringUtils.isNotBlank(page.getFilepath())) {

                String thumbnailUrl = page.getThumbnailUrl();
                ImageContent resource;
                if (size.getWidth() * size.getHeight() > 0) {
                    resource = new ImageContent(new URI(thumbnailUrl), true);
                    resource.setWidth(size.width);
                    resource.setHeight(size.height);
                } else {
                    ImageInformation imageInfo;
                    resource = new ImageContent(new URI(thumbnailUrl), false);
                    try {
                        imageInfo = imageDelivery.getImage().getImageInformation(page);
                        resource.setService(imageInfo);
                    } catch (ContentLibException e) {
                        logger.error("Error reading image information from " + thumbnailUrl + ": " + e.toString());
                        resource = new ImageContent(new URI(thumbnailUrl), true);
                        resource.setWidth(size.width);
                        resource.setHeight(size.height);
                    }
                }

                Annotation imageAnnotation = new Annotation(null);
                imageAnnotation.setMotivation(Motivation.PAINTING);
                imageAnnotation.setOn(new Canvas(canvas.getId()));

                imageAnnotation.setResource(resource);
                canvas.addImage(imageAnnotation);

            }

            sequence.addCanvas(canvas);
        }
        if (sequence.getCanvases() != null) {
            manifest.setSequence(sequence);
        }
    }
    

    /**
     * @param anchor
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private List<StructElement> getChildDocs(long iddocParent) throws PresentationException, IndexUnreachableException {
        String query = SolrConstants.IDDOC_PARENT + ":" + iddocParent;
        SolrDocumentList solrDocs = DataManager.getInstance().getSearchIndex().getDocs(query, getSolrFieldList());
        if (solrDocs != null) {
            return solrDocs.stream().filter(doc -> doc.getFieldValue(SolrConstants.IDDOC) != null).map(doc -> {
                try {
                    return new StructElement(Long.parseLong((String) doc.getFieldValue(SolrConstants.IDDOC)), doc);
                } catch (NumberFormatException | IndexUnreachableException e) {
                    logger.error("Failed to create struct element from " + doc);
                    return null;
                }
            }).filter(ele -> ele != null).collect(Collectors.toList());
        } else {
            return Collections.EMPTY_LIST;
        }
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
     * @param v1
     * @return
     */
    private Integer getSortingNumber(StructElement volume) {
        String numSort = volume.getVolumeNoSort();
        if (StringUtils.isNotBlank(numSort)) {
            try {
                return Integer.parseInt(numSort);
            } catch (NumberFormatException e) {
                logger.error("Cannot read integer value from " + numSort);
            }
        }
        return -1;
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
     * @param page
     * @return
     */
    private Dimension getSize(PhysicalElement page) {
        Dimension size = new Dimension(0, 0);
        if (page.getMimeType().toLowerCase().startsWith("video") || page.getMimeType().toLowerCase().startsWith("text")) {
            size.setSize(page.getVideoWidth(), page.getVideoHeight());
        } else if (page.getMimeType().toLowerCase().startsWith("image")) {
            if (page.hasIndividualSize()) {
                size.setSize(page.getImageWidth(), page.getImageHeight());
            } else {
                try {
                    ImageInformation info = imageDelivery.getImage().getImageInformation(page);
                    size.setSize(info.getWidth(), info.getHeight());
                } catch (ContentLibException | URISyntaxException e) {
                    logger.error("Unable to retrieve image size for " + page + ": " + e.toString());
                }
            }
        }
        return size;
    }



    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.iiif.presentation.builder.AbstractBuilder#getPath()
     */
    @Override
    protected String getPath() {
        return "/manifests";
    }

}
