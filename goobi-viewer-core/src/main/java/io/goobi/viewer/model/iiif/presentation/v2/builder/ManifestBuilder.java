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
package io.goobi.viewer.model.iiif.presentation.v2.builder;

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_ALTO;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PLAINTEXT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RECORD;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.api.iiif.IIIFUrlResolver;
import de.intranda.api.iiif.image.ImageInformation;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.presentation.content.ImageContent;
import de.intranda.api.iiif.presentation.content.LinkingContent;
import de.intranda.api.iiif.presentation.enums.Format;
import de.intranda.api.iiif.presentation.enums.ViewingHint;
import de.intranda.api.iiif.presentation.v2.AbstractPresentationModelElement2;
import de.intranda.api.iiif.presentation.v2.Collection2;
import de.intranda.api.iiif.presentation.v2.Manifest2;
import de.intranda.api.iiif.search.AutoSuggestService;
import de.intranda.api.iiif.search.SearchService;
import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType.Colortype;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.RegionRequest;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Rotation;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.model.ManifestLinkConfiguration;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.ImageDeliveryBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.iiif.presentation.v2.builder.LinkingProperty.LinkingTarget;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * <p>
 * ManifestBuilder class.
 * </p>
 *
 * @author Florian Alpers
 */
public class ManifestBuilder extends AbstractBuilder {

    private static final Logger logger = LogManager.getLogger(ManifestBuilder.class);
    protected ImageDeliveryBean imageDelivery = BeanUtils.getImageDeliveryBean();
    private BuildMode buildMode = BuildMode.IIIF;

    /**
     * <p>
     * Constructor for ManifestBuilder.
     * </p>
     *
     * @param apiUrlManager
     */
    public ManifestBuilder(AbstractApiUrlManager apiUrlManager) {
        super(apiUrlManager);
    }

    /**
     * <p>
     * generateManifest.
     * </p>
     *
     * @param ele a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @return a {@link de.intranda.api.iiif.presentation.IPresentationModelElement} object.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public AbstractPresentationModelElement2 generateManifest(StructElement ele)
            throws URISyntaxException, PresentationException, IndexUnreachableException, ViewerConfigurationException, DAOException {

        final AbstractPresentationModelElement2 manifest;

        if (ele.isAnchor()) {
            manifest = new Collection2(getManifestURI(ele.getPi()), ele.getPi());
            manifest.addViewingHint(ViewingHint.multipart);
        } else {
            ele.setImageNumber(1);
            manifest = new Manifest2(getManifestURI(ele.getPi()));
            SearchService search = new SearchService(getSearchServiceURI(ele.getPi()));
            AutoSuggestService autoComplete = new AutoSuggestService(getAutoCompleteServiceURI(ele.getPi()));
            search.addService(autoComplete);
            manifest.addService(search);
        }

        populate(ele, manifest);

        return manifest;
    }

    /**
     * <p>
     * populate.
     * </p>
     *
     * @param ele a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param manifest a {@link de.intranda.api.iiif.presentation.AbstractPresentationModelElement} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public void populate(StructElement ele, final AbstractPresentationModelElement2 manifest)
            throws ViewerConfigurationException, IndexUnreachableException, DAOException, PresentationException {
        this.getAttributions().forEach(manifest::addAttribution);
        IMetadataValue label = getLabel(ele).orElse(new SimpleMetadataValue(ele.getLabel()));
        manifest.setLabel(label);
        getDescription(ele).ifPresent(manifest::setDescription);

        addMetadata(manifest, ele);

        addThumbnail(ele, manifest);

        addLogo(ele, manifest);
        addLicences(manifest);
        addNavDate(ele, manifest);
        addSeeAlsos(manifest, ele);
        addRenderings(manifest, ele);

        if (getBuildMode().equals(BuildMode.IIIF)) {
            addCmsPages(ele, manifest);
        }
    }

    private void addThumbnail(StructElement ele, final AbstractPresentationModelElement2 manifest) {
        try {
            String thumbUrl = imageDelivery.getThumbs().getThumbnailUrl(ele);
            if (StringUtils.isNotBlank(thumbUrl)) {
                ImageContent thumb = new ImageContent(new URI(thumbUrl));
                manifest.addThumbnail(thumb);
                if (IIIFUrlResolver.isIIIFImageUrl(thumbUrl)) {
                    String imageInfoURI = IIIFUrlResolver.getIIIFImageBaseUrl(thumbUrl);
                    thumb.setService(new ImageInformation(imageInfoURI));
                }
            }
        } catch (URISyntaxException e) {
            logger.warn("Unable to retrieve thumbnail url", e);
        }
    }

    private void addCmsPages(StructElement ele, final AbstractPresentationModelElement2 manifest) {
        try {
            DataManager.getInstance()
                    .getDao()
                    .getCMSPagesForRecord(ele.getPi(), null)
                    .stream()
                    .filter(CMSPage::isPublished)
                    .forEach(page -> {
                        try {
                            LinkingContent cmsPage = new LinkingContent(new URI(this.urls.getApplicationUrl() + "/" + page.getUrl()));
                            cmsPage.setLabel(new SimpleMetadataValue(page.getTitle()));
                            cmsPage.setFormat(Format.TEXT_HTML);
                            manifest.addRelated(cmsPage);
                        } catch (URISyntaxException e) {
                            logger.error("Unable to retrieve viewer url for {}", ele);
                        }
                    });
        } catch (Exception e) {
            logger.warn(e.toString());
        }
    }

    private static void addNavDate(StructElement ele, final AbstractPresentationModelElement2 manifest) {
        String navDateField = DataManager.getInstance().getConfiguration().getIIIFNavDateField();
        if (StringUtils.isNotBlank(navDateField) && StringUtils.isNotBlank(ele.getMetadataValue(navDateField))) {
            try {
                String eleValue = ele.getMetadataValue(navDateField);
                LocalDate date = LocalDate.parse(eleValue);
                manifest.setNavDate(date.atStartOfDay());
            } catch (NullPointerException | DateTimeParseException e) {
                logger.warn("Unable to parse {} as Date", ele.getMetadataValue(navDateField));
            }
        }
    }

    private static void addLicences(final AbstractPresentationModelElement2 manifest) {
        for (String license : DataManager.getInstance().getConfiguration().getIIIFLicenses()) {
            try {
                URI uri = new URI(license);
                manifest.addLicense(uri);
            } catch (URISyntaxException e) {
                logger.error("Configured license '{}' is not a URI", license);
            }
        }
    }

    private void addLogo(StructElement ele, final AbstractPresentationModelElement2 manifest)
            throws ViewerConfigurationException, IndexUnreachableException, DAOException {
        List<String> logoUrl = getLogoUrl();
        if (logoUrl.isEmpty()) {
            Optional<String> url =
                    BeanUtils.getImageDeliveryBean().getFooter().getWatermarkUrl(Optional.empty(), Optional.ofNullable(ele), Optional.empty());
            url.ifPresent(l -> logoUrl.add(l));
        }
        for (String url : logoUrl) {
            ImageContent logo;
            try {
                logo = new ImageContent(new URI(url));
                manifest.addLogo(logo);
            } catch (URISyntaxException e) {
                logger.error("Error adding manifest logo from {}", url, e);
            }
        }
    }

    public void addSeeAlsos(AbstractPresentationModelElement2 manifest, StructElement ele) {

        if (ele.isLidoRecord() && DataManager.getInstance().getConfiguration().isVisibleIIIFSeeAlsoLido()) {
            /*LIDO*/
            try {
                LinkingContent resolver = new LinkingContent(new URI(getLidoResolverUrl(ele)));
                resolver.setFormat(Format.TEXT_XML);
                IMetadataValue label = getLabel(DataManager.getInstance().getConfiguration().getLabelIIIFSeeAlsoLido());
                resolver.setLabel(label);
                manifest.addSeeAlso(resolver);
            } catch (URISyntaxException e) {
                logger.error("Unable to retrieve lido resolver url for {}", ele);
            }
        } else if (DataManager.getInstance().getConfiguration().isVisibleIIIFSeeAlsoMets()) {
            /*METS/MODS*/
            try {
                LinkingContent metsResolver = new LinkingContent(new URI(getMetsResolverUrl(ele)));
                metsResolver.setFormat(Format.TEXT_XML);
                IMetadataValue label = getLabel(DataManager.getInstance().getConfiguration().getLabelIIIFSeeAlsoMets());
                metsResolver.setLabel(label);
                manifest.addSeeAlso(metsResolver);
            } catch (URISyntaxException e) {
                logger.error("Unable to retrieve mets resolver url for {}", ele);
            }
        }

        List<ManifestLinkConfiguration> linkConfigurations = DataManager.getInstance().getConfiguration().getIIIFSeeAlsoMetadataConfigurations();
        for (ManifestLinkConfiguration config : linkConfigurations) {
            try {
                Metadata md = config.getMetadata();
                md.populate(ele, "", null, null);
                String label = config.getLabel();
                String format = config.getFormat();
                String value = md.getCombinedValue(", ");
                if (StringUtils.isNotBlank(value)) {
                    LinkingContent seeAlso = new LinkingContent(new URI(value));
                    seeAlso.setFormat(Format.fromMimeType(format));
                    seeAlso.setLabel(ViewerResourceBundle.getTranslations(label, true));
                    manifest.addSeeAlso(seeAlso);
                }
            } catch (IndexUnreachableException | PresentationException | URISyntaxException e) {
                logger.error("Unable to create seeAlso link for {}", config.getLabel(), e);
            }

        }
    }

    /**
     * @param manifest
     * @param ele
     * @throws URISyntaxException
     */
    public void addRenderings(AbstractPresentationModelElement2 manifest, StructElement ele) {

        this.getRenderings().forEach(link -> {
            try {
                URI id = getLinkingPropertyUri(ele, link.target);
                if (id != null) {
                    manifest.addRendering(link.getLinkingContent(id));
                }
            } catch (URISyntaxException | PresentationException | IndexUnreachableException e) {
                logger.error("Error building linking property url", e);
            }
        });
    }

    private URI getLinkingPropertyUri(StructElement ele, LinkingTarget target)
            throws URISyntaxException, PresentationException, IndexUnreachableException {

        if (!LinkingTarget.VIEWER.equals(target) && ele.isAnchor()) {
            return null;
        }
        if (target.equals(LinkingTarget.PDF) && !ele.isHasImages()) {
            return null;
        }

        URI uri = null;
        switch (target) {
            case VIEWER:
                String pageUrl = ele.getUrl(getMatchingPageType(ele));
                uri = URI.create(pageUrl);
                if (!uri.isAbsolute()) {
                    uri = URI.create(this.urls.getApplicationUrl() + pageUrl);
                }
                break;
            case ALTO:
                uri = this.urls.path(RECORDS_RECORD, RECORDS_ALTO).params(ele.getPi()).buildURI();
                break;
            case PLAINTEXT:
                uri = this.urls.path(RECORDS_RECORD, RECORDS_PLAINTEXT).params(ele.getPi()).buildURI();
                break;
            case PDF:
                String pdfDownloadUrl = BeanUtils.getImageDeliveryBean().getPdf().getPdfUrl(ele, ele.getLabel());
                uri = URI.create(pdfDownloadUrl);
                break;
            case METS:
                uri = new URI(getMetsResolverUrl(ele));
                break;
            case LIDO:
                uri = new URI(getLidoResolverUrl(ele));
                break;
            default:
                break;
        }
        return uri;
    }

    private static PageType getMatchingPageType(StructElement ele) {
        PageType pageType = PageType.viewMetadata;
        if (ele.isHasImages()) {
            pageType = PageType.viewImage;
        } else if (ele.isAnchor()) {
            pageType = PageType.viewToc;
        }
        return pageType;
    }

    /**
     * <p>
     * addVolumes.
     * </p>
     *
     * @param anchor a {@link de.intranda.api.iiif.presentation.v2.Collection2} object.
     * @param volumes a {@link java.util.List} object.
     */
    public void addVolumes(Collection2 anchor, List<StructElement> volumes) {
        for (StructElement volume : volumes) {
            try {
                IPresentationModelElement child = generateManifest(volume);
                if (child instanceof Manifest2) {
                    //                    addBaseSequence((Manifest)child, volume, child.getId().toString());
                    anchor.addManifest((Manifest2) child);
                }
            } catch (ViewerConfigurationException | URISyntaxException | PresentationException | IndexUnreachableException | DAOException e) {
                logger.error("Error creating child manigest for {}", volume);
            }

        }
    }

    /**
     * <p>
     * addAnchor.
     * </p>
     *
     * @param manifest a {@link de.intranda.api.iiif.v2.Manifest} object.
     * @param anchorPI a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void addAnchor(Manifest2 manifest, String anchorPI)
            throws PresentationException, IndexUnreachableException, URISyntaxException, DAOException {

        /*ANCHOR*/
        if (StringUtils.isNotBlank(anchorPI)) {
            manifest.addWithin(new Collection2(getManifestURI(anchorPI), anchorPI));
        }

    }

    /**
     * @param volume
     * @return {@link Integer}
     */
    private static Integer getSortingNumber(StructElement volume) {
        String numSort = volume.getVolumeNoSort();
        if (StringUtils.isNotBlank(numSort)) {
            try {
                return Integer.parseInt(numSort);
            } catch (NumberFormatException e) {
                logger.error("Cannot read integer value from {}", numSort);
            }
        }
        return -1;
    }

    /**
     * Retrieves the logo url configured in webapi.iiif.logo. If the configured value is an absulute http(s) url, this url will be returned. If it is
     * any other absolute url a contentserver link to that url will be returned. If it is a non-absolute url, it will be considered a filepath within
     * the static images folder of the viewer theme and the appropriate url will be returned
     *
     * @return An optional containing the configured logo url, or an empty optional if no logo was configured
     * @throws ViewerConfigurationException
     */
    private List<String> getLogoUrl() throws ViewerConfigurationException {
        List<String> urlStrings = DataManager.getInstance().getConfiguration().getIIIFLogo();
        List<String> logos = new ArrayList<>();
        for (final String urlString : urlStrings) {
            try {
                URI url = new URI(urlString);
                if (url.isAbsolute() && url.getScheme().toLowerCase().startsWith("http")) {
                    logos.add(urlString);
                } else if (url.isAbsolute()) {
                    try {
                        String logo = imageDelivery.getIiif()
                                .getIIIFImageUrl(urlString, "-", RegionRequest.FULL.toString(), Scale.MAX.toString(), Rotation.NONE.toString(),
                                        Colortype.DEFAULT.toString(),
                                        ImageFileFormat.getMatchingTargetFormat(ImageFileFormat.getImageFileFormatFromFileExtension(url.getPath()))
                                                .toString());
                        logos.add(logo);
                    } catch (NullPointerException e) {
                        logger.error("Value '{}' configured in webapi.iiif.logo is not a valid uri", urlString);
                    }
                } else if (!StringUtils.isBlank(urlString)) {
                    logos.add(imageDelivery.getThumbs().getThumbnailPath(urlString).toString());
                }
            } catch (URISyntaxException e) {
                logger.error("Value '{}' configured in webapi.iiif.logo is not a valid uri", urlString);
            }

        }
        return logos;
    }

    /**
     * <p>
     * Getter for the field <code>buildMode</code>.
     * </p>
     *
     * @return the buildMode
     */
    public BuildMode getBuildMode() {
        return buildMode;
    }

    /**
     * <p>
     * Setter for the field <code>buildMode</code>.
     * </p>
     *
     * @param buildMode the buildMode to set
     * @return a {@link io.goobi.viewer.model.iiif.presentation.v2.builder.ManifestBuilder} object.
     */
    public ManifestBuilder setBuildMode(BuildMode buildMode) {
        this.buildMode = buildMode;
        return this;
    }

}
