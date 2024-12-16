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
package io.goobi.viewer.controller.imaging;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocumentList;

import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;

/**
 * <p>
 * WatermarkHandler class.
 * </p>
 *
 * @author Florian Alpers
 */
public class WatermarkHandler implements Serializable {

    private static final long serialVersionUID = 3341191578406800851L;

    private static final Logger logger = LogManager.getLogger(WatermarkHandler.class);

    /** Constant <code>WATERMARK_TEXT_TYPE_URN="URN"</code>. */
    public static final String WATERMARK_TEXT_TYPE_URN = "URN";
    /** Constant <code>WATERMARK_TEXT_TYPE_PURL="PURL"</code>. */
    public static final String WATERMARK_TEXT_TYPE_PURL = "PURL";
    /** Constant <code>WATERMARK_TEXT_TYPE_SOLR="SOLR:"</code>. */
    public static final String WATERMARK_TEXT_TYPE_SOLR = "SOLR:";

    /** Constant <code>REQUIRED_SOLR_FIELDS</code>. */
    public static final String[] REQUIRED_SOLR_FIELDS = new String[] { SolrConstants.PI, SolrConstants.IDDOC, SolrConstants.LOGID,
            SolrConstants.ISWORK, SolrConstants.ISANCHOR, SolrConstants.DOCSTRCT, SolrConstants.DATAREPOSITORY };

    /**
     * caches all watermark texts for documents by document pi.
     */
    private final Map<String, String> documentWatermarkTextMap = new HashMap<>();
    private final List<String> watermarkTextConfiguration;
    private final List<String> watermarkIdFields;
    private final String servletPath;

    /**
     * <p>
     * Constructor for WatermarkHandler.
     * </p>
     *
     * @param configuration a {@link io.goobi.viewer.controller.Configuration} object.
     * @param servletPath a {@link java.lang.String} object.
     */
    public WatermarkHandler(Configuration configuration, String servletPath) {
        this.watermarkTextConfiguration =
                configuration.isWatermarkTextConfigurationEnabled() ? configuration.getWatermarkTextConfiguration() : Collections.emptyList();
        this.watermarkIdFields = configuration.getWatermarkIdField();
        this.servletPath = servletPath;
    }

    /**
     * Creates the watermark url for the given pageType, adding watermarkId for the given {@link io.goobi.viewer.model.viewer.StructElement} and
     * {@link io.goobi.viewer.model.viewer.PhysicalElement} page. If the watermark height of the given pageType and image is 0, an empty optional is
     * returned
     *
     * @param page a {@link java.util.Optional} object.
     * @param doc a {@link java.util.Optional} object.
     * @param pageType The pageType of the currentView. Taken into consideration for footer height, if not null
     * @return a {@link java.util.Optional} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Optional<String> getWatermarkUrl(Optional<PhysicalElement> page, Optional<StructElement> doc, Optional<PageType> pageType)
            throws ViewerConfigurationException, IndexUnreachableException, DAOException {
        return getWatermarkUrl(Scale.MAX, pageType, page.map(p -> p.getImageType()), doc.map(d -> getFooterIdIfExists(d).orElse(null)),
                page.map(p -> getWatermarkTextIfExists(p).orElse(null)));
    }

    /**
     * Creates the watermark url for the given pageType, adding watermarkId for the current
     * {@link io.goobi.viewer.managedbeans.ActiveDocumentBean#getTopDocument()} and watermarkText for the current
     * {@link io.goobi.viewer.model.viewer.PhysicalElement} page. If the watermark height of the given pageType and image is 0, an empty optional is
     * returned.
     *
     * @param pageType The pageType of the currentView. Taken into consideration for footer height, if not null
     * @param scale a {@link de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale} object.
     * @param imageType a {@link java.util.Optional} object.
     * @param watermarkId a {@link java.util.Optional} object.
     * @param watermarkText a {@link java.util.Optional} object.
     * @return a {@link java.util.Optional} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public Optional<String> getWatermarkUrl(Scale scale, Optional<PageType> pageType, Optional<ImageType> imageType, Optional<String> watermarkId,
            Optional<String> watermarkText) throws IndexUnreachableException, DAOException, ViewerConfigurationException {

        int footerHeight = DataManager.getInstance()
                .getConfiguration()
                .getFooterHeight(pageType.orElse(null), imageType.map(ImageType::getFormat).map(ImageFileFormat::getMimeType).orElse(""));
        if (footerHeight > 0) {
            String format = DataManager.getInstance().getConfiguration().getWatermarkFormat();

            AbstractApiUrlManager urls = DataManager.getInstance().getRestApiManager().getContentApiManager().orElse(null);
            if (urls != null) {

                String url = urls.path(ApiUrls.RECORDS_FILES_FOOTER, ApiUrls.RECORDS_FILES_FOOTER_IIIF)
                        .params("-", "-", "full", scale.toString(), "0", "default", format)
                        .build();
                StringBuilder urlBuilder = new StringBuilder(url);

                UrlParameterSeparator separator = new UrlParameterSeparator();

                watermarkId.ifPresent(footerId -> urlBuilder.append(separator.getChar()).append("watermarkId=").append(footerId));
                watermarkText.ifPresent(text -> urlBuilder.append(separator.getChar()).append("watermarkText=").append(text));

                return Optional.of(urlBuilder.toString());

            }
            StringBuilder urlBuilder = new StringBuilder(DataManager.getInstance().getConfiguration().getIIIFApiUrl());

            urlBuilder.append("footer/full/").append(scale.toString()).append("/0/default.").append(format);

            UrlParameterSeparator separator = new UrlParameterSeparator();

            watermarkId.ifPresent(footerId -> urlBuilder.append(separator.getChar()).append("watermarkId=").append(footerId));
            watermarkText.ifPresent(text -> urlBuilder.append(separator.getChar()).append("watermarkText=").append(text));

            return Optional.of(urlBuilder.toString());
        }

        return Optional.empty();
    }

    /**
     * Optionally returns the watermark text for the given page. If the text is empty or none is configures, an empty optional is returned
     *
     * @param page a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @return a {@link java.util.Optional} object.
     */
    public Optional<String> getWatermarkTextIfExists(PhysicalElement page) {
        if (!watermarkTextConfiguration.isEmpty()) {
            StringBuilder urlBuilder = new StringBuilder();
            for (String text : watermarkTextConfiguration) {
                if (StringUtils.startsWithIgnoreCase(text, WATERMARK_TEXT_TYPE_SOLR)) {
                    String field = text.substring(WATERMARK_TEXT_TYPE_SOLR.length());
                    try {
                        SolrDocumentList res = DataManager.getInstance()
                                .getSearchIndex()
                                .search(new StringBuilder(SolrConstants.PI).append(":").append(page.getPi()).toString(), SolrSearchIndex.MAX_HITS,
                                        null, Collections.singletonList(field));
                        if (res != null && !res.isEmpty() && res.get(0).getFirstValue(field) != null) {
                            // logger.debug(field + ":" + res.get(0).getFirstValue(field));
                            urlBuilder.append((String) res.get(0).getFirstValue(field));
                            break;
                        }
                    } catch (PresentationException e) {
                        logger.debug("PresentationException thrown here: " + e.getMessage());
                    } catch (IndexUnreachableException e) {
                        logger.debug("IndexUnreachableException thrown here: " + e.getMessage());

                    }
                } else if (StringUtils.equalsIgnoreCase(text, WATERMARK_TEXT_TYPE_URN)) {
                    if (StringUtils.isNotEmpty(page.getUrn())) {
                        urlBuilder.append(page.getUrn());
                        break;
                    }
                } else if (StringUtils.equalsIgnoreCase(text, WATERMARK_TEXT_TYPE_PURL)) {
                    urlBuilder.append(servletPath)
                            .append("/")
                            .append(PageType.viewImage.getName())
                            .append("/")
                            .append(page.getPi())
                            .append("/")
                            .append(page.getOrder())
                            .append("/");
                    break;
                } else {
                    urlBuilder.append(text);
                    break;
                }
            }
            if (StringUtils.isNotBlank(urlBuilder.toString())) {

                return Optional.of(urlBuilder.toString());
            }
        }

        return Optional.empty();
    }

    /**
     * Optionally returns the watermark text for the given pi. If the text is empty or none is configures, an empty optional is returned
     *
     * @param doc a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @return a {@link java.util.Optional} object.
     */
    public Optional<String> getWatermarkTextIfExists(StructElement doc) {
        if (documentWatermarkTextMap.containsKey(doc.getPi())) {
            return Optional.ofNullable(documentWatermarkTextMap.get(doc.getPi()));
        }
        synchronized (documentWatermarkTextMap) {
            if (!watermarkTextConfiguration.isEmpty()) {
                StringBuilder urlBuilder = new StringBuilder();
                for (String text : watermarkTextConfiguration) {
                    if (StringUtils.startsWithIgnoreCase(text, WATERMARK_TEXT_TYPE_SOLR)) {
                        String field = text.substring(WATERMARK_TEXT_TYPE_SOLR.length());
                        try {
                            SolrDocumentList res = DataManager.getInstance()
                                    .getSearchIndex()
                                    .search(new StringBuilder(SolrConstants.PI).append(":").append(doc.getPi()).toString(), SolrSearchIndex.MAX_HITS,
                                            null, Collections.singletonList(field));
                            if (res != null && !res.isEmpty() && res.get(0).getFirstValue(field) != null) {
                                // logger.debug(field + ":" + res.get(0).getFirstValue(field));
                                urlBuilder.append((String) res.get(0).getFirstValue(field));
                                break;
                            }
                        } catch (PresentationException e) {
                            logger.debug("PresentationException thrown here: " + e.getMessage());
                        } catch (IndexUnreachableException e) {
                            logger.debug("IndexUnreachableException thrown here: " + e.getMessage());

                        }
                    } else if (StringUtils.equalsIgnoreCase(text, WATERMARK_TEXT_TYPE_URN)) {
                        String urn = doc.getMetadataValue(SolrConstants.URN);
                        try {
                            if (StringUtils.isBlank(urn) && doc.getTopStruct() != null) {
                                urn = doc.getTopStruct().getMetadataValue(SolrConstants.URN);
                            }
                        } catch (PresentationException | IndexUnreachableException e) {
                            logger.error(e.toString());
                        }
                        if (StringUtils.isNotEmpty(urn)) {
                            urlBuilder.append(urn);
                            break;
                        }
                    } else if (StringUtils.equalsIgnoreCase(text, WATERMARK_TEXT_TYPE_PURL)) {
                        urlBuilder.append(servletPath)
                                .append("/")
                                .append(PageType.viewImage.getName())
                                .append("/")
                                .append(doc.getPi())
                                .append("/")
                                .append(1)
                                .append("/");
                        break;
                    } else {
                        urlBuilder.append(text);
                        break;
                    }
                }
                if (!documentWatermarkTextMap.containsKey(doc.getPi())) {
                    documentWatermarkTextMap.put(doc.getPi(), urlBuilder.length() > 0 ? urlBuilder.toString() : null);
                }
                if (StringUtils.isNotBlank(urlBuilder.toString())) {
                    return Optional.of(urlBuilder.toString());
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Return the watermark id for the given {@link io.goobi.viewer.model.viewer.StructElement}.
     *
     * @param topDocument a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @return a {@link java.util.Optional} object.
     */
    public Optional<String> getFooterIdIfExists(StructElement topDocument) {
        String footerId = null;
        if (!watermarkIdFields.isEmpty() && topDocument != null) {
            for (String watermarkIdField : watermarkIdFields) {
                footerId = topDocument.getMetadataValue(watermarkIdField);
                if (StringUtils.isNotBlank(footerId)) {
                    break;
                }
            }
        }
        return Optional.ofNullable(footerId);
    }

}
