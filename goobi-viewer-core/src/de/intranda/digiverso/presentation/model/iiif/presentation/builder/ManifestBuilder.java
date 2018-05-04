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

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Date;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.ImageDeliveryBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.iiif.presentation.AbstractPresentationModelElement;
import de.intranda.digiverso.presentation.model.iiif.presentation.Collection;
import de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement;
import de.intranda.digiverso.presentation.model.iiif.presentation.Manifest;
import de.intranda.digiverso.presentation.model.iiif.presentation.content.ImageContent;
import de.intranda.digiverso.presentation.model.iiif.presentation.content.LinkingContent;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.Format;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.ViewingHint;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.SimpleMetadataValue;
import de.intranda.digiverso.presentation.model.viewer.StructElement;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType.Colortype;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.RegionRequest;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Rotation;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;

/**
 * @author Florian Alpers
 *
 */
public class ManifestBuilder extends AbstractBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ManifestBuilder.class);
    protected final ImageDeliveryBean imageDelivery = BeanUtils.getImageDeliveryBean();

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
        manifest.setAttribution(getAttribution());
        manifest.setLabel(new SimpleMetadataValue(ele.getLabel()));

        addMetadata(manifest, ele);

        try {
            String thumbUrl = imageDelivery.getThumbs().getThumbnailUrl(ele);
            if (StringUtils.isNotBlank(thumbUrl)) {
                ImageContent thumb = new ImageContent(new URI(thumbUrl), true);
                manifest.setThumbnail(thumb);
            }
        } catch (URISyntaxException e) {
            logger.warn("Unable to retrieve thumbnail url", e);
        }

        
        Optional<String> logoUrl = getLogoUrl();
        if(!logoUrl.isPresent()) {
            logoUrl =  BeanUtils.getImageDeliveryBean().getFooter().getWatermarkUrl(Optional.empty(), Optional.ofNullable(ele), Optional.empty());
        }
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
     * @param baseUrl2
     * @param manifest
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public void addVolumes(Collection anchor, long iddoc) throws PresentationException, IndexUnreachableException {
        List<StructElement> volumes =
                getChildDocs(iddoc).stream().sorted((v1, v2) -> getSortingNumber(v1).compareTo(getSortingNumber(v2))).collect(Collectors.toList());

        addVolumes(anchor, volumes);
    }

    /**
     * @param anchor
     * @param volumes
     */
    public void addVolumes(Collection anchor, List<StructElement> volumes) {
        for (StructElement volume : volumes) {
            try {
                IPresentationModelElement child = generateManifest(volume);
                if (child instanceof Manifest) {
                    //                    addBaseSequence((Manifest)child, volume, child.getId().toString());
                    anchor.addManifest((Manifest) child);
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
    public void addAnchor(Manifest manifest, String anchorPI)
            throws PresentationException, IndexUnreachableException, ConfigurationException, URISyntaxException, DAOException {

        /*ANCHOR*/
        if (StringUtils.isNotBlank(anchorPI)) {
            manifest.addWithin(new Collection(getManifestURI(anchorPI)));
        }

    }

    /**
     * @param anchor
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    @SuppressWarnings("unchecked")
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
     * Retrieves the logo url configured in webapi.iiif.logo. If the configured value is an absulute http(s) url, this url 
     * will be returned. If it is any other absolute url a contentserver link to that url will be returned.
     * If it is a non-absolute url, it will be considered a filepath within the static images folder of the viewer theme and
     * the appropriate url will be returned
     * 
     * @return  An optional containing the configured logo url, or an empty optional if no logo was configured
     */
    private Optional<String> getLogoUrl() {
        String urlString = DataManager.getInstance().getConfiguration().getIIIFLogo();
        if(urlString != null) {
            try {
                URI url = new URI(urlString);
                if(url.isAbsolute() && url.getScheme().toLowerCase().startsWith("http")) {
                    //fall through
                } else if(url.isAbsolute()) {
                    try {                        
                        urlString = imageDelivery.getIiif().getIIIFImageUrl(urlString, "-", RegionRequest.FULL.toString(), Scale.MAX.toString(), 
                                Rotation.NONE.toString(), Colortype.DEFAULT.toString(), 
                                ImageFileFormat.getMatchingTargetFormat(ImageFileFormat.getImageFileFormatFromFileExtension(url.getPath())).toString()
                                , 85);
                    } catch(NullPointerException e) {
                        logger.error("Value '{}' configured in webapi.iiif.logo is not a valid uri", urlString);
                        urlString = null;
                    }
                } else if(!StringUtils.isBlank(urlString)) {
                    urlString = imageDelivery.getThumbs().getThumbnailPath(urlString).toString();
                } else {
                    urlString = null;
                }
            } catch (URISyntaxException e) {
                logger.error("Value '{}' configured in webapi.iiif.logo is not a valid uri", urlString);
                urlString = null;
            }
        }
        return Optional.ofNullable(urlString);
    }

}
