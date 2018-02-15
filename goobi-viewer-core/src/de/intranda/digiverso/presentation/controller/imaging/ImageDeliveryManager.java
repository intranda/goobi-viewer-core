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
package de.intranda.digiverso.presentation.controller.imaging;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.ActiveDocumentBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;
import de.intranda.digiverso.presentation.model.viewer.StructElement;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType;
import de.unigoettingen.sub.commons.contentlib.servlet.model.iiif.ImageInformation;

/**
 * @author Florian Alpers
 *
 */
public class ImageDeliveryManager {

    private static final Logger logger = LoggerFactory.getLogger(ImageDeliveryManager.class);
    public static final String WATERMARK_TEXT_TYPE_URN = "URN";
    public static final String WATERMARK_TEXT_TYPE_PURL = "PURL";
    public static final String WATERMARK_TEXT_TYPE_SOLR = "SOLR:";

    @Inject
    private ActiveDocumentBean activeDocumentBean;
    
    private Optional<PhysicalElement> getCurrentPageIfExists() {
        return Optional.ofNullable(activeDocumentBean)
                .map(adb -> adb.getViewManager())
                .map(vm -> {
                    try {
                        return vm.getCurrentPage();
                    } catch (IndexUnreachableException | DAOException e) {
                        logger.error(e.toString());
                        return null;
                    }
                });
    }

    private Optional<ActiveDocumentBean> getActiveDocumentBeanIfExists() {
        return Optional.ofNullable(activeDocumentBean);
    }

    private Optional<String> getFooterIdIfExists() {
        return getActiveDocumentBeanIfExists().map(adb -> adb.getTopDocument())
                .map(doc -> getFooterId(doc));
    }

    /**
     * Creates the watermark url for the given pageType, adding watermarkId for the current {@link ActiveDocumentBean#getTopDocument()} and
     * watermarkText for the current {@link PhysicalElement page} If the watermark height of the given pageType and image is 0, an empty optional is
     * returned
     * 
     * @param info ImageInformation as basis for watermark size. Must not be null
     * @param pageType The pageType of the currentView. Taken into consideration for footer height, if not null
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ConfigurationException
     */
    public Optional<String> getWatermarkUrl(ImageInformation info, PageType pageType)
            throws IndexUnreachableException, DAOException, ConfigurationException {

        int footerHeight = DataManager.getInstance()
                .getConfiguration()
                .getFooterHeight(pageType, getImageType(info));
        if (footerHeight > 0) {
            String format = DataManager.getInstance()
                    .getConfiguration()
                    .getWatermarkFormat();

            Integer width = info.getSizes()
                    .stream()
                    .sorted((size1, size2) -> Integer.compare(size2.getWidth(), size2.getWidth()))
                    .map(size -> size.getWidth())
                    .findFirst()
                    .orElse(info.getWidth());

            StringBuilder urlBuilder = new StringBuilder(DataManager.getInstance()
                    .getConfiguration()
                    .getIiifUrl());

            urlBuilder.append("footer/full/!")
                    .append(width)
                    .append(",") //width
                    .append(DataManager.getInstance()
                            .getConfiguration()
                            .getFooterHeight(pageType, getImageType(info)))
                    .append("/0/default.")
                    .append(format)
                    .append("?");

            getFooterIdIfExists().ifPresent(footerId -> urlBuilder.append("watermarkId=")
                    .append(footerId)
                    .append("&"));
            getCurrentPageIfExists().ifPresent(page -> urlBuilder.append("watermarkText=")
                    .append(page.getWatermarkText()));

            return Optional.of(urlBuilder.toString());
        } else {
            return Optional.empty();
        }
    }

    private String getFooterId(StructElement topDocument) {
        String footerIdField = DataManager.getInstance()
                .getConfiguration()
                .getWatermarkIdField();
        String footerId = null;
        if (footerIdField != null && topDocument != null) {
            footerId = topDocument.getMetadataValue(footerIdField);
        }
        return footerId;
    }

    private ImageType getImageType(ImageInformation info) {
        String id = info.getId();
        ImageFileFormat iff = ImageFileFormat.getImageFileFormatFromFileExtension(id);
        if (iff != null) {
            return new ImageType(iff);
        } else {
            return null;
        }
    }

    public String getPdfUrl(PhysicalElement page) {
        StringBuilder sb = new StringBuilder(DataManager.getInstance()
                .getConfiguration()
                .getIiifUrl());
        sb.append("image")
                .append("/")
                .append(page.getPi())
                .append("/")
                .append(page.getFileName())
                .append("/")
                .append("full/max/0/")
                .append(page.getPi())
                .append("_")
                .append(page.getOrder())
                .append(".pdf");
                .append("?watermarkText=")
                .append(getWatermarkText(page));

        getFooterIdIfExists().ifPresent(footerId -> sb.append("&watermarkId=")
                .append(footerId)
                .append("&"));

        return sb.toString();
    }
    
    public String getPdfUrl(String pi, String divId, String label) {
        
        if(StringUtils.isBlank(label)) {
            label = pi;
            if(StringUtils.isNotBlank(divId)) {
                label += "_" + divId;
            }
        }
        label = label.replaceAll("[\\s]", "_");
        label = label.replaceAll("[\\W]", "");
        
        StringBuilder sb = new StringBuilder(DataManager.getInstance()
                .getConfiguration()
                .getIiifUrl());
        sb.append("pdf/mets/")
                .append(pi).append(".xml")
                .append("/");
        
        if(StringUtils.isNotBlank(divId)) {
            sb.append(divId).append("/");
        }

        getFooterIdIfExists().ifPresent(footerId -> sb.append("&watermarkId=")
                .append(footerId)
                .append("&"));

        return sb.toString();
    }
    
    /**
     * Optionally returns the watermark text for the given page. If the text is empty or none is configures, an empty optional is returned
     * 
     * @param page
     * @return
     */
    public Optional<String> getWatermarkText(PhysicalElement page) {
        List<String> watermarkTextConfiguration = getWatermarkTextConfiguration();
        if (!watermarkTextConfiguration.isEmpty()) {
            StringBuilder urlBuilder = new StringBuilder();
            for (String text : watermarkTextConfiguration) {
                if (StringUtils.startsWithIgnoreCase(text, WATERMARK_TEXT_TYPE_SOLR)) {
                    String field = text.substring(WATERMARK_TEXT_TYPE_SOLR.length());
                    try {
                        SolrDocumentList res = DataManager.getInstance().getSearchIndex().search(new StringBuilder(SolrConstants.PI).append(":")
                                .append(page.getPi()).toString(), SolrSearchIndex.MAX_HITS, null, Collections.singletonList(field));
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
                    urlBuilder.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append("/").append(
                            PageType.viewImage.getName()).append("/").append(page.getPi()).append("/").append(page.getOrder()).append("/");
                    break;
                } else {
                    urlBuilder.append(text);
                    break;
                }
            }
            if(StringUtils.isNotBlank(text)) {                
                return Optional.of(urlBuilder.toString());
            }
        }

        return Optional.empty();
    }
    
    /**
     * @return  The watermark text configuration. If none exists, an empty list is returned
     */
    private List<String> getWatermarkTextConfiguration() {
        List<String> watermarkTextConfiguration = DataManager.getInstance().getConfiguration().getWatermarkTextConfiguration();
        if(watermarkTextConfiguration == null) {
            watermarkTextConfiguration = Collections.EMPTY_LIST;
        }
        return watermarkTextConfiguration;
    }
    
    public static class UrlParameterSepartor {
        
        private char[] separators = new char[]{'?','&'};
        int index = 0;
        
        public char getChar() {
            return separators[Math.min(1, index++)];

        }
        
    }

}
