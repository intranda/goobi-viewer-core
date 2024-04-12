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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * <p>
 * PdfHandler class.
 * </p>
 *
 * @author Florian Alpers
 */
public class PdfHandler {

    private final WatermarkHandler watermarkHandler;
    private final String iiifUrl;
    private final AbstractApiUrlManager urls;

    /**
     * <p>
     * Constructor for PdfHandler.
     * </p>
     *
     * @param watermarkHandler a {@link io.goobi.viewer.controller.imaging.WatermarkHandler} object.
     * @param configuration a {@link io.goobi.viewer.controller.Configuration} object.
     */
    public PdfHandler(WatermarkHandler watermarkHandler, Configuration configuration) {
        this.watermarkHandler = watermarkHandler;
        this.iiifUrl = configuration.getIIIFApiUrl();
        this.urls = null;
    }

    /**
     * <p>
     * Constructor for PdfHandler.
     * </p>
     *
     * @param watermarkHandler a {@link io.goobi.viewer.controller.imaging.WatermarkHandler} object.
     * @param urls
     */
    public PdfHandler(WatermarkHandler watermarkHandler, AbstractApiUrlManager urls) {
        this.watermarkHandler = watermarkHandler;
        this.iiifUrl = null;
        this.urls = urls;
    }

    /**
     * Return the pdf-download url for the given {@link io.goobi.viewer.model.viewer.StructElement} and
     * {@link io.goobi.viewer.model.viewer.PhysicalElement}
     *
     * @param doc a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param page a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @return a {@link java.lang.String} object.
     */
    public String getPdfUrl(StructElement doc, PhysicalElement page) {
        return getPdfUrl(doc, new PhysicalElement[] { page });
    }

    /**
     * Return the pdf-download url for the given {@link io.goobi.viewer.model.viewer.StructElement} and a number of
     * {@link io.goobi.viewer.model.viewer.PhysicalElement}s
     *
     * @param se a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param pages an array of {@link io.goobi.viewer.model.viewer.PhysicalElement} objects.
     * @return a {@link java.lang.String} object.
     */
    public String getPdfUrl(StructElement se, PhysicalElement[] pages) {
        final UrlParameterSeparator paramSep = new UrlParameterSeparator();
        StringBuilder sb = new StringBuilder();
        String filenames = Arrays.stream(pages)
                .map(page -> page.getFileName())
                .map(this::escapeURI)
                .collect(Collectors.joining("$"));
        if (this.urls != null) {
            sb.append(urls.path(ApiUrls.RECORDS_FILES_IMAGE, ApiUrls.RECORDS_FILES_IMAGE_PDF).params(pages[0].getPi(), filenames).build());
        } else {
            sb.append(this.iiifUrl);
            sb.append("image")
                    .append("/")
                    .append(pages[0].getPi())
                    .append("/")
                    .append(filenames)
                    .append("/")
                    .append("full/max/0/")
                    .append(pages[0].getPi())
                    .append("_")
                    .append(pages[0].getOrder());
            if (pages.length > 1) {
                sb.append("-").append(pages[pages.length - 1].getOrder());
            }
            sb.append(".pdf");
        }
        if (se != null && StringUtils.isNotBlank(se.getLogid())) {
            sb.append(paramSep.getChar()).append("divID=").append(se.getLogid());
        }

        return sb.toString();
    }

    private String escapeURI(String uri) {
        try {
            // logger.trace("Encoding param: {}", replacement); //NOSONAR Sometimes needed for debugging
            return URLEncoder.encode(uri, StringTools.DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return uri;
        }
    }

    /**
     * Returns an existing pdf file from the media folder
     *
     * @param pi a {@link java.lang.String} object.
     * @param filename a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getPdfUrl(String pi, String filename) {
        if (this.urls != null) {
            return urls.path(ApiUrls.RECORDS_FILES_IMAGE, ApiUrls.RECORDS_FILES_IMAGE_PDF).params(pi, filename).build();
        }
        StringBuilder sb = new StringBuilder(this.iiifUrl);
        sb.append("pdf").append("/").append(pi).append("/").append(filename).append("/").append("full/max/0/").append(filename);
        return sb.toString();
    }

    /**
     * Gets the url to the pdf for the given {@link io.goobi.viewer.model.viewer.StructElement}. The pi is the one of the topStruct element of the
     * given StructElement
     *
     * @param label The name for the output file (.pdf-extension excluded). If this is null or empty, the label will be generated from pi and divId
     * @param doc a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getPdfUrl(StructElement doc, String label) throws PresentationException, IndexUnreachableException {
        String pi = doc.getPi();
        return getPdfUrl(doc, pi, label);
    }

    /**
     * Gets the url to the pdf for the given pi and divId
     *
     * @param pi PI of the process from which to build pdf. Must be provided
     * @param label The name for the output file (.pdf-extension excluded). If this is null or empty, the label will be generated from pi and divId
     * @param doc a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getPdfUrl(StructElement doc, String pi, String label) throws PresentationException, IndexUnreachableException {

        String divId = doc.isWork() ? null : doc.getLogid();

        return getPdfUrl(pi, Optional.ofNullable(divId), Optional.ofNullable(label));
    }

    /**
     * Returns the url to a PDF build from the mets file for the given {@code pi}
     *
     * @param pi a {@link java.lang.String} object.
     * @param divID a {@link java.util.Optional} object.
     * @param label a {@link java.util.Optional} object.
     * @return a {@link java.lang.String} object.
     */
    public String getPdfUrl(String pi, final Optional<String> divID, Optional<String> label) {

        Optional<String> divId = divID.filter(StringUtils::isNotBlank);
        String filename = label.filter(StringUtils::isNotBlank)
                .map(l -> l.replaceAll("[\\s]", "_"))
                .map(l -> l.replaceAll("[\\W]", ""))
                .map(l -> l.toLowerCase().endsWith(".pdf") ? l : (l + ".pdf"))
                .orElse(pi + divId.map(id -> "_" + id).orElse("") + ".pdf");

        StringBuilder sb;
        if (this.urls != null) {
            if (divId.isPresent()) {
                sb = new StringBuilder(urls.path(ApiUrls.RECORDS_SECTIONS, ApiUrls.RECORDS_SECTIONS_PDF).params(pi, divId.get()).build());
            } else {
                sb = new StringBuilder(urls.path(ApiUrls.RECORDS_RECORD, ApiUrls.RECORDS_PDF).params(pi).build());
            }
        } else {
            sb = new StringBuilder(this.iiifUrl);
            sb.append("pdf/mets/").append(pi).append(".xml").append("/");
            divId.ifPresent(id -> sb.append(id).append("/"));
            sb.append(filename);

        }
        return sb.toString();
    }

    /**
     * @return the watermarkHandler
     */
    public WatermarkHandler getWatermarkHandler() {
        return watermarkHandler;
    }
}
