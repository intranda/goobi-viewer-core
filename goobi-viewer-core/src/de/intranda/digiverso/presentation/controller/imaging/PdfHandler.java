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

import org.apache.commons.lang3.StringUtils;

import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;

/**
 * @author Florian Alpers
 *
 */
class PdfHandler {

    private final WatermarkHandler watermarkHandler;
    private final String iiifUrl;
    
    public PdfHandler(WatermarkHandler watermarkHandler, Configuration configuration) {
        this.watermarkHandler = watermarkHandler;
        this.iiifUrl = configuration.getIiifUrl();
    }
    
    /**
     * Gets the url to the pdf of the given page
     * 
     * @param page
     * @return
     */
    public String getPdfUrl(PhysicalElement page) {

        final UrlParameterSeparator paramSep = new UrlParameterSeparator();

        StringBuilder sb = new StringBuilder(this.iiifUrl);
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

        this.watermarkHandler.getWatermarkTextIfExists(page).ifPresent(text -> sb.append(paramSep.getChar()).append("watermarkText=").append(text));
        this.watermarkHandler.getFooterIdIfExists().ifPresent(footerId -> sb.append(paramSep.getChar()).append("watermarkId=").append(footerId));

        return sb.toString();
    }
    
    /**
     * Gets the url to the pdf for the given pi and divId
     * 
     * @param pi PI of the process from which to build pdf. Must be provided
     * @param divId DivID (LogID) of the docstruct for which the pdf should be generated. If this is null or empty, a pdf for the complete work is
     *            generated
     * @param label The name for the output file (.pdf-extension excluded). If this is null or empty, the label will be generated from pi and divId
     * @return
     */
    public String getPdfUrl(String pi, String divId, String label) {

        final UrlParameterSeparator paramSep = new UrlParameterSeparator();

        if (StringUtils.isBlank(label)) {
            label = pi;
            if (StringUtils.isNotBlank(divId)) {
                label += "_" + divId;
            }
        }
        label = label.replaceAll("[\\s]", "_");
        label = label.replaceAll("[\\W]", "");

        StringBuilder sb = new StringBuilder(this.iiifUrl);
        sb.append("pdf/mets/").append(pi).append(".xml").append("/");

        if (StringUtils.isNotBlank(divId)) {
            sb.append(divId).append("/");
        }
        this.watermarkHandler.getWatermarkTextIfExists(pi).ifPresent(text -> sb.append(paramSep.getChar()).append("watermarkText=").append(text));
        this.watermarkHandler.getFooterIdIfExists().ifPresent(footerId -> sb.append(paramSep.getChar()).append("watermarkId=").append(footerId));

        return sb.toString();
    }

}
